package hec.usgs.waterdata;

import java.time.LocalDate;
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
    public LocalDate begin;
    public LocalDate end;
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

    /**
     * Filters to most common Daily time series within a date range
     */
    public static List<TimeSeriesMetadata> filterDaily(List<TimeSeriesMetadata> metadata, LocalDate start, LocalDate end) {
        return filterDaily(metadata).stream()
                .filter(ts -> ts.begin != null && ts.end != null
                        && ts.begin.compareTo(end) <= 0 && ts.end.compareTo(start) >= 0)
                .collect(Collectors.toList());
    }

    public static class DateRange {
        public final LocalDate begin;
        public final LocalDate end;

        DateRange(LocalDate begin, LocalDate end) {
            this.begin = begin;
            this.end = end;
        }

        public String getBeginAsString() {
            return begin != null ? begin.toString() : "";
        }

        public String getEndAsString() {
            return end != null ? end.toString() : "";
        }
    }

    /**
     * Returns the earliest begin and latest end for entries matching
     * the given computationPeriodIdentifier, or null if none match.
     */
    public static DateRange dateRange(List<TimeSeriesMetadata> metadata, String computationPeriodIdentifier) {
        return dateRange(metadata.stream()
                .filter(ts -> computationPeriodIdentifier.equals(ts.computationPeriodIdentifier))
                .collect(Collectors.toList()));
    }

    /**
     * Returns the earliest begin and latest end across the given metadata,
     * or null if no entries have a non-null begin.
     */
    public static DateRange dateRange(List<TimeSeriesMetadata> metadata) {
        LocalDate minBegin = null;
        LocalDate maxEnd = null;
        for (TimeSeriesMetadata ts : metadata) {
            if (ts.begin == null) continue;
            if (minBegin == null || ts.begin.isBefore(minBegin))
                minBegin = ts.begin;
            if (ts.end != null && (maxEnd == null || ts.end.isAfter(maxEnd)))
                maxEnd = ts.end;
        }
        return minBegin != null ? new DateRange(minBegin, maxEnd) : null;
    }

    static LocalDate parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        return LocalDate.parse(s.substring(0, 10));
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
        ts.begin = parseDate(table.get(row, "begin"));
        ts.end = parseDate(table.get(row, "end"));
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
