package hec.usgs.waterdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
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
    static final String TIME_SERIES_METADATA_URL ="https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?f=csv&lang=en-US&limit=50000&properties=id,unit_of_measure,parameter_name,parameter_code,statistic_id,hydrologic_unit_code,state_name,last_modified,begin,end,begin_utc,end_utc,computation_period_identifier,computation_identifier,thresholds,sublocation_identifier,primary,monitoring_location_id,web_description,parameter_description,parent_time_series_id&skipGeometry=false&offset=0&monitoring_location_id=%s";
    static final String TIME_SERIES_METADATA_POST_URL ="https://api.waterdata.usgs.gov/ogcapi/v0/collections/time-series-metadata/items?f=csv&lang=en-US&limit=50000&properties=id,unit_of_measure,parameter_name,parameter_code,statistic_id,hydrologic_unit_code,state_name,last_modified,begin,end,begin_utc,end_utc,computation_period_identifier,computation_identifier,thresholds,sublocation_identifier,primary,monitoring_location_id,web_description,parameter_description,parent_time_series_id&skipGeometry=false&offset=0";
    private UsgsWaterDataApi() {
        // Prevent instantiation
    }

    public static List<DailyValue> getDailyTimeSeries(String monitoringLocationId, String parameterCode,
                                                String statisticId, LocalDate startDate, LocalDate endDate) throws Exception {
        String url = String.format(DAILY_URL, monitoringLocationId,
                parameterCode, statisticId, startDate.toString(), endDate.toString());
        String csv = WebUtility.getPage(url);
        List<DailyValue> values = CsvFile.fromString(csv).mapRows(DailyValue::fromRow);
        return DailyValue.ensureContinuous(values);
    }

    public static List<MonitoringLocation> getLocations(String stateCode, String siteTypeCode) throws Exception {
        String url = String.format(LOCATIONS_URL, stateCode, siteTypeCode);
        String csv = WebUtility.getPage(url);
        return CsvFile.fromString(csv).mapRows(MonitoringLocation::fromRow);
    }

    public static List<TimeSeriesMetadata> getTimeSeriesMetadata(String monitoringLocationId) throws Exception {
        String url = String.format(TIME_SERIES_METADATA_URL, monitoringLocationId);
        String csv = WebUtility.getPage(url);
        return CsvFile.fromString(csv).mapRows(TimeSeriesMetadata::fromRow);
    }

    public static String postPage(String url, String propertyName, String[] items) throws Exception {
        String json = buildCqlInFilter(propertyName, items);
        String cacheKey = url + "|" + propertyName + "|" + String.join(",", items);
        return WebUtility.postPage(url, "application/query-cql-json", json, cacheKey);
    }

    static String buildCqlInFilter(String propertyName, String[] items) {
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

}
