package org.opendcs.usgs.waterdata;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Client for the USGS Water Data API ({@code api.waterdata.usgs.gov}).
 * <a href="https://api.waterdata.usgs.gov/ogcapi/v0/openapi?f=html#/">OpenAPI Documentation</a>
 * 
 * <p>Provides methods to retrieve monitoring locations, daily time-series data,
 * continuous time-series data, and time-series metadata. Responses are cached in 
 * memory for five minutes to reduce redundant network calls.
 *
 * <p>USGS has - <b>Rate limits:</b> 50 requests per hour and 50,000 responses per request
 * without an API key. With an API key: 1,000 requests per hour and 50,000
 * responses per request. Register for a key at
 * <a href="https://api.waterdata.usgs.gov/signup/">https://api.waterdata.usgs.gov/signup/</a>
 * and set the {@code USGS_WATER_API_KEY} environment variable.
 */
public class UsgsWaterDataApi {

    private static final Logger logger = Logger.getLogger(UsgsWaterDataApi.class.getName());

    public static final double UNDEFINED_DOUBLE = -Float.MAX_VALUE;

    /**
     * Maximum number of responses the API returns per request
     */
    static final int PAGE_LIMIT = 50000;
    static final long MAX_WINDOW_DAYS = 1000;

    static final String ROOT_URL                      = "https://api.waterdata.usgs.gov/ogcapi/v0/collections/";
    static final String LOCATIONS_URL                 = ROOT_URL + "monitoring-locations/items?f=csv&lang=en-US&limit=50000&offset=0&agency_code=USGS&state_code=%s&site_type_code=%s";
    static final String TIME_SERIES_QUERY_ID          = "items?f=csv&lang=en-US&limit=50000&properties=time,value&skipGeometry=true&sortby=time&offset=0&time_series_id=%s&time=%s/%s";
    static final String DAILY_URL_ID                  = ROOT_URL + "daily/" + TIME_SERIES_QUERY_ID;
    static final String CONTINUOUS_URL_ID             = ROOT_URL + "continuous/" + TIME_SERIES_QUERY_ID;
    static final String TIME_SERIES_METADATA_PROPERTIES = "id,unit_of_measure,parameter_name,parameter_code,statistic_id,hydrologic_unit_code,state_name,last_modified,begin,end,begin_utc,end_utc,computation_period_identifier,computation_identifier,thresholds,sublocation_identifier,primary,monitoring_location_id,web_description,parameter_description,parent_time_series_id";
    static final String TIME_SERIES_METADATA_URL      = ROOT_URL + "time-series-metadata/items?f=csv&lang=en-US&limit=50000&properties=" + TIME_SERIES_METADATA_PROPERTIES + "&skipGeometry=false&offset=0&monitoring_location_id=%s";
    static final String TIME_SERIES_METADATA_POST_URL = ROOT_URL + "time-series-metadata/items?f=csv&lang=en-US&limit=50000&properties=" + TIME_SERIES_METADATA_PROPERTIES + "&skipGeometry=false&offset=0";
    private static volatile String apiKey;
    private static volatile String applicationName;

    private UsgsWaterDataApi() {
        // Prevent instantiation
    }

    /**
     * Sets the API key used for later requests.
     * This takes priority over the environment variable.
     */
    public static void setApiKey(String key) {
        apiKey = key;
    }

    /**
     * Sets an optional application name included in the User-Agent header
     * of every request. This helps USGS identify the calling application.
     */
    public static void setApplicationName(String name) {
        applicationName = name;
    }

    /**
     * Returns the current application name, or null if not set.
     */
    public static String getApplicationName() {
        return applicationName;
    }

    /**
     * Returns the current API key: the in-memory key if set,
     * otherwise the environment variable, or null if neither is set.
     */
    public static String getApiKey() {
        String key = apiKey;
        if (key != null && !key.isEmpty()) {
            return key;
        }
        return System.getenv(UsgsApiKeyException.ENV_VAR_NAME);
    }


    private static List<DailyValue> fetchDailyValues(String timeSeriesId, String startDate, String endDate) throws Exception {
        if (TestSite.isTestSeriesId(timeSeriesId))
            return TestSite.generateDailyValues(startDate, endDate);
        String url = String.format(DAILY_URL_ID, timeSeriesId, startDate, endDate);
        String csv = WebUtility.getPage(url);
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return DailyValue.ensureContinuous(CsvFile.fromString(csv).mapRows(DailyValue::fromRow));
    }

    /**
     * Fetches continuous values for a date range, paging in chunks when necessary.
     */
    private static List<InstantaneousValue> fetchContinuousValues(String timeSeriesId, String startDate, String endDate) throws Exception {
        if (TestSite.isTestSeriesId(timeSeriesId))
            return TestSite.generateContinuousValues(startDate, endDate);

        Instant rangeEnd = Instant.parse(normalizeToInstant(endDate));
        Instant chunkStart = Instant.parse(normalizeToInstant(startDate));

        List<InstantaneousValue> all = new ArrayList<>();
        Instant lastSeen = null;
        while (chunkStart.isBefore(rangeEnd)) {
            Instant windowEnd = chunkStart.plus(MAX_WINDOW_DAYS, ChronoUnit.DAYS);
            if (windowEnd.isAfter(rangeEnd)) windowEnd = rangeEnd;

            List<InstantaneousValue> page = fetchContinuousPage(timeSeriesId, chunkStart, windowEnd);
            lastSeen = appendDeduplicated(all, page, lastSeen, timeSeriesId);

            if (page.size() >= PAGE_LIMIT && lastSeen != null && lastSeen.isAfter(chunkStart)) {
                // Response was truncated at the page limit; resume from its last point.
                chunkStart = lastSeen;
                logger.info("Continuous query returned a full page; fetching next chunk from " + chunkStart);
            } else {
                chunkStart = windowEnd;
            }
        }
        return all;
    }

    /**
     * Fetches a single page of continuous values for one time window.
     */
    private static List<InstantaneousValue> fetchContinuousPage(String timeSeriesId, Instant start, Instant end) throws Exception {
        String url = String.format(CONTINUOUS_URL_ID, timeSeriesId, start.toString(), end.toString());
        String csv = WebUtility.getPage(url);
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return CsvFile.fromString(csv).mapRows(InstantaneousValue::fromRow);
    }

    /**
     * Appends a page's values to dest, skipping potental duplicates at the page boundary.
     */
    private static Instant appendDeduplicated(List<InstantaneousValue> dest, List<InstantaneousValue> page,
                                              Instant lastSeen, String timeSeriesId) {
        for (int i = 0; i < page.size(); i++) {
            InstantaneousValue iv = page.get(i);
            if (lastSeen != null && !iv.time.isAfter(lastSeen)) {
                boolean expectedBoundaryDuplicate = i == 0 && iv.time.equals(lastSeen);
                if (!expectedBoundaryDuplicate) {
                    logger.warning("Dropping duplicate time-stamp " + iv.time
                            + " in continuous series " + timeSeriesId);
                }
                continue;
            }
            dest.add(iv);
            lastSeen = iv.time;
        }
        return lastSeen;
    }

    /**
     * Normalizes a date or date-time string to a full RFC 3339 instant
     *   example: 2026-06-01T00:00:00Z
     */
    static String normalizeToInstant(String dateOrDateTime) {
        // A date-only value ("2026-06-01") has no time component; make it midnight UTC.
        String instant = dateOrDateTime.contains("T") ? dateOrDateTime : dateOrDateTime + "T00:00:00Z";
        return Instant.parse(instant).toString();
    }

    /**
     * Retrieves daily time-series data using metadata.
     * Uses the time-series id for a precise query, which correctly handles
     * locations that have multiple time-series for the same parameter/statistic.
     * @param metadata identifies the time series (location, parameter, statistic)
     * @param startDate start of date range (RFC 3339, e.g. "2020-01-01" or "2020-01-01T00:00:00Z")
     * @param endDate end of date range (RFC 3339)
     */
    public static TimeSeries<DailyValue> getDailyTimeSeries(TimeSeriesMetadata metadata,
                                                             String startDate, String endDate) throws Exception {
        return new TimeSeries<>(metadata, fetchDailyValues(metadata.id, startDate, endDate));
    }

    /**
     * Retrieves continuous time-series data using metadata.
     * Uses the time-series id for a precise query, which correctly handles
     * locations that have multiple time-series for the same parameter/statistic.
     * @param metadata identifies the time series (location, parameter, statistic)
     * @param startDate start of date range (ISO 8601, e.g. "2018-02-12T00:00:00Z")
     * @param endDate end of date range (ISO 8601, e.g. "2018-03-18T12:31:12Z")
     */
    public static TimeSeries<InstantaneousValue> getContinuousTimeSeries(TimeSeriesMetadata metadata,
                                                                          String startDate, String endDate) throws Exception {
        return new TimeSeries<>(metadata, fetchContinuousValues(metadata.id, startDate, endDate));
    }

    public static List<MonitoringLocation> getLocations(String stateCode, String siteTypeCode) throws Exception {
        String url = String.format(LOCATIONS_URL, stateCode, siteTypeCode);
        String csv = WebUtility.getPage(url);
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return CsvFile.fromString(csv).mapRows(MonitoringLocation::fromRow);
    }

    public static List<TimeSeriesMetadata> getTimeSeriesMetadata(String monitoringLocationId) throws Exception {
        if (TestSite.isTestSite(monitoringLocationId))
            return TestSite.generateMetadata();
        String url = String.format(TIME_SERIES_METADATA_URL, monitoringLocationId);
        String csv = WebUtility.getPage(url);
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return CsvFile.fromString(csv).mapRows(TimeSeriesMetadata::fromRow);
    }

    public static String postPage(String url, String propertyName, String[] items) throws Exception {
        String json = buildCqlInFilter(propertyName, items);
        String cacheKey = url + "|" + propertyName + "|" + String.join(",", items);
        return WebUtility.postPage(url, "application/query-cql-json", json, cacheKey);
    }

    private static String buildCqlInFilter(String propertyName, String[] items) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"op\":\"in\",\"args\":[{\"property\":\"")
          .append(propertyName).append("\"},[");
        for (int i = 0; i < items.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(items[i]).append("\"");
        }
        sb.append("]]}");
        return sb.toString();
    }

    /**
     * Retrieves time-series metadata for multiple monitoring locations.
     * @param monitoringLocations list of locations. each is the format "USGS-12345678"
     * @return map of monitoring location ID to list of time-series metadata
     * @throws Exception on network or parsing errors
     */
    public static Map<String, List<TimeSeriesMetadata>> getTimeSeriesMetadata(String[] monitoringLocations) throws Exception {

        int batchSize = 200; // found by trial and error , avoid 403 Forbidden from USGS API when too many IDs are included in a single request
        HashMap<String, List<TimeSeriesMetadata>> byLocation = new HashMap<>();
        for (String loc : monitoringLocations) {
            if (TestSite.isTestSite(loc))
                byLocation.put(loc, TestSite.generateMetadata());
        }
        for (int i = 0; i < monitoringLocations.length; i += batchSize) {
            String[] batch = Arrays.copyOfRange(monitoringLocations, i,
                    Math.min(i + batchSize, monitoringLocations.length));
            String csv = postPage(TIME_SERIES_METADATA_POST_URL, "monitoring_location_id", batch);
            if (csv == null || csv.isBlank()) continue;
            CsvFile.fromString(csv).mapRows(TimeSeriesMetadata::fromRow).forEach(ts ->
                    byLocation.computeIfAbsent(ts.monitoringLocationId, k -> new ArrayList<>()).add(ts));
        }
        return byLocation;
    }

    /**
     * Retrieves annual peak streamflow and gage height from the legacy NWIS peak-flow service.
     * @see PeakFlowService
     */
    public static List<TimeSeries<InstantaneousValue>> getAnnualPeaks(
            List<TimeSeriesMetadata> siteMetadata) throws Exception {
        return PeakFlowService.getAnnualPeaks(siteMetadata);
    }

    /**
     * Retrieves annual peak streamflow and gage height within a date range.
     * @see PeakFlowService
     */
    public static List<TimeSeries<InstantaneousValue>> getAnnualPeaks(
            List<TimeSeriesMetadata> siteMetadata, String startDate, String endDate) throws Exception {
        return PeakFlowService.getAnnualPeaks(siteMetadata, startDate, endDate);
    }

}
