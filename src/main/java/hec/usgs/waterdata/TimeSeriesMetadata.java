package hec.usgs.waterdata;

import java.util.List;
import java.util.stream.Collectors;

public class TimeSeriesMetadata {

    public String x;
    public String y;
    public String id;
    public String unitOfMeasure;
    public String parameterName;
    public String parameterCode;
    public String statisticId;
    public String hydrologicUnitCode;
    public String stateName;
    public String lastModified;
    public String begin;
    public String end;
    public String beginUtc;
    public String endUtc;
    public String computationPeriodIdentifier;
    public String computationIdentifier;
    public String thresholds;
    public String sublocationIdentifier;
    public String primary;
    public String monitoringLocationId;
    public String webDescription;
    public String parameterDescription;
    public String parentTimeSeriesId;

    /**
     * Filters to most common Daily time series 
     */
    public static List<TimeSeriesMetadata> filterDaily(List<TimeSeriesMetadata> metadata) {
        return metadata.stream()
                .filter(ts -> "Daily".equals(ts.computationPeriodIdentifier))
                .filter(ts -> ts.statisticId != null && !ts.statisticId.isEmpty())
                .filter(ts -> "Mean".equals(ts.computationIdentifier) || "Instantaneous".equals(ts.computationIdentifier))
                .collect(Collectors.toList());
    }

    static TimeSeriesMetadata fromRow(DataTable table, int row) {
        TimeSeriesMetadata ts = new TimeSeriesMetadata();
        ts.x = table.get(row, "x");
        ts.y = table.get(row, "y");
        ts.id = table.get(row, "id");
        ts.unitOfMeasure = table.get(row, "unit_of_measure");
        ts.parameterName = table.get(row, "parameter_name");
        ts.parameterCode = table.get(row, "parameter_code");
        ts.statisticId = table.get(row, "statistic_id");
        ts.hydrologicUnitCode = table.get(row, "hydrologic_unit_code");
        ts.stateName = table.get(row, "state_name");
        ts.lastModified = table.get(row, "last_modified");
        ts.begin = table.get(row, "begin");
        ts.end = table.get(row, "end");
        ts.beginUtc = table.get(row, "begin_utc");
        ts.endUtc = table.get(row, "end_utc");
        ts.computationPeriodIdentifier = table.get(row, "computation_period_identifier");
        ts.computationIdentifier = table.get(row, "computation_identifier");
        ts.thresholds = table.get(row, "thresholds");
        ts.sublocationIdentifier = table.get(row, "sublocation_identifier");
        ts.primary = table.get(row, "primary");
        ts.monitoringLocationId = table.get(row, "monitoring_location_id");
        ts.webDescription = table.get(row, "web_description");
        ts.parameterDescription = table.get(row, "parameter_description");
        ts.parentTimeSeriesId = table.get(row, "parent_time_series_id");
        return ts;
    }
}
