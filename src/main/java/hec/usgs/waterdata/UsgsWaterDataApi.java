package hec.usgs.waterdata;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Client for the USGS Water Data API ({@code api.waterdata.usgs.gov}).
 *
 * <p>Provides methods to retrieve monitoring locations, daily time-series data,
 * and time-series metadata. Responses are cached in memory for five minutes
 * to reduce redundant network calls.
 *
 * <p><b>Rate limits:</b> 50 requests per hour and 50,000 responses per request
 * without an API key. With an API key: 1,000 requests per hour and 50,000
 * responses per request. Register for a key at
 * <a href="https://api.waterdata.usgs.gov/signup/">https://api.waterdata.usgs.gov/signup/</a>
 * and set the {@value #ENV_USGS_WATER_API_KEY} environment variable.
 */
public class UsgsWaterDataApi {

    private static final Logger logger = Logger.getLogger(UsgsWaterDataApi.class.getName());
    public static final double UNDEFINED_DOUBLE = -Float.MAX_VALUE;

    static final String LOCATIONS_URL = "https://api.waterdata.usgs.gov/ogcapi/v0/collections/monitoring-locations/items?f=csv&lang=en-US&limit=50000&offset=0&agency_code=USGS&state_code=%s&site_type_code=%s";
    static final String DAILY_URL = "https://api.waterdata.usgs.gov/ogcapi/v0/collections/daily/items?f=csv&lang=en-US&limit=50000&properties=time,value&skipGeometry=true&sortby=time&offset=0&monitoring_location_id=%s&parameter_code=%s&statistic_id=%s&time=%s/%s";
    static final String TIME_SERIES_METADATA_URL ="https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?f=csv&lang=en-US&limit=100&properties=id,unit_of_measure,parameter_name,parameter_code,statistic_id,hydrologic_unit_code,state_name,last_modified,begin,end,begin_utc,end_utc,computation_period_identifier,computation_identifier,thresholds,sublocation_identifier,primary,monitoring_location_id,web_description,parameter_description,parent_time_series_id&skipGeometry=false&offset=0&monitoring_location_id=%s";
    static final String ENV_USGS_WATER_API_KEY = "USGS_WATER_API_KEY";
    private static final long PAGE_CACHE_TTL_MS = 5 * 60 * 1000; // 5 minutes
    private static final int PAGE_CACHE_MAX_SIZE = 100;
    private static final ConcurrentHashMap<String, CacheEntry> PAGE_CACHE = new ConcurrentHashMap<>();

    private static class CacheEntry {
        final String body;
        final long fetchedAtMillis;

        CacheEntry(String body, long fetchedAtMillis) {
            this.body = body;
            this.fetchedAtMillis = fetchedAtMillis;
        }
    }


    private UsgsWaterDataApi() {
        // Prevent instantiation
    }   

    public static List<DailyValue> getDailyTimeSeries(String monitoringLocationId, String parameterCode,
                                                String statisticId, LocalDate startDate, LocalDate endDate) throws Exception {
        String url = String.format(DAILY_URL, monitoringLocationId,
                parameterCode, statisticId, startDate.toString(), endDate.toString());
        String csv = getPage(url);
        List<DailyValue> values = CsvFile.fromString(csv).mapRows(DailyValue::fromRow);
        return DailyValue.ensureContinuous(values);
    }

    public static List<MonitoringLocation> getLocations(String stateCode, String siteTypeCode) throws Exception {
        String url = String.format(LOCATIONS_URL, stateCode, siteTypeCode);
        String csv = getPage(url);
        return CsvFile.fromString(csv).mapRows(MonitoringLocation::fromRow);
    }

    public static String getPage(String url) throws Exception {
        
        logger.info("Requesting: " + url);

        long now = System.currentTimeMillis();
        CacheEntry cached = PAGE_CACHE.get(url);
        if (cached != null) {
            if ((now - cached.fetchedAtMillis) < PAGE_CACHE_TTL_MS) {
                logger.info("Cache hit: " + url);
                return cached.body;
            }
        }
        
        HttpClient client = HttpClient.newHttpClient();
        String apiKey = System.getenv(ENV_USGS_WATER_API_KEY);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();
        if (apiKey != null && !apiKey.isEmpty()) {
            logger.info("Using API key from environment variable " + ENV_USGS_WATER_API_KEY);
            requestBuilder.header("X-Api-Key", apiKey);
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        String body = response.body();

        response.headers().firstValue("X-RateLimit-Limit")
                .ifPresent(v -> logger.info("X-RateLimit-Limit: " + v));
        response.headers().firstValue("X-RateLimit-Remaining")
                .ifPresent(v -> {
                    logger.info("X-RateLimit-Remaining: " + v);
                    if ("0".equals(v.trim())) {
                        throw new RuntimeException(
                                "USGS API rate limit exceeded (X-RateLimit-Remaining: 0). "
                                + "Register for an API key at https://api.waterdata.usgs.gov/signup/ "
                                + "and set the " + ENV_USGS_WATER_API_KEY + " environment variable.");
                    }
                });
        if (Boolean.getBoolean("usgs.debug")) {
            saveForDebugging(response, body);
        }

        if (PAGE_CACHE.size() >= PAGE_CACHE_MAX_SIZE) {
            PAGE_CACHE.clear();
        }
        PAGE_CACHE.put(url, new CacheEntry(body, now));
        return body;
    }

    private static void saveForDebugging(HttpResponse<String> response, String body) {
        try {
            Optional<String> disposition = response.headers().firstValue("Content-Disposition");
            String filename = null;
            if (disposition.isPresent()) {
                for (String part : disposition.get().split(";")) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("filename=")) {
                        filename = trimmed.substring("filename=".length()).replace("\"", "").trim();
                    }
                }
            }
            if (filename == null || filename.isEmpty()) {
                logger.info("No filename found in Content-Disposition header");
                return;
            }
            Path dir = Paths.get(System.getProperty("user.home"), "usgs.waterdata");
            Files.createDirectories(dir);
            Path file = dir.resolve(filename);
            if (Files.exists(file)) {
                String name = filename;
                String ext = "";
                int dot = filename.lastIndexOf('.');
                if (dot >= 0) {
                    name = filename.substring(0, dot);
                    ext = filename.substring(dot);
                }
                int n = 1;
                while (Files.exists(file)) {
                    file = dir.resolve(name + n + ext);
                    n++;
                }
            }
            Files.writeString(file, body);
            logger.info("Saved response to: " + file);
        } catch (Exception e) {
            logger.warning("Failed to save debug file: " + e.getMessage());
        }
    }

    // TOD add t1,t2 parameters for time range
    public static List<TimeSeriesMetadata> getTimeSeriesMetadata(String monitoringLocationId) throws Exception {
        String url = String.format(TIME_SERIES_METADATA_URL, monitoringLocationId);
        String csv = getPage(url);
        return CsvFile.fromString(csv).mapRows(TimeSeriesMetadata::fromRow);
    }

    
}
