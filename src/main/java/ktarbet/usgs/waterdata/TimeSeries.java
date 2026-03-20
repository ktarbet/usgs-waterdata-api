package ktarbet.usgs.waterdata;

import java.util.List;

/**
 * A time series combining {@link TimeSeriesMetadata} with a list of data points.
 * The type parameter {@code T} is typically {@link DailyValue} or {@link InstantaneousValue}.
 */
public class TimeSeries<T> {

    public final TimeSeriesMetadata metadata;
    public final List<T> values;

    public TimeSeries(TimeSeriesMetadata metadata, List<T> values) {
        this.metadata = metadata;
        this.values = values;
    }

    public String getId() { return metadata.id; }
    public String getParameterCode() { return metadata.parameterCode; }
    public String getParameterName() { return metadata.parameterName; }
    public String getStatisticId() { return metadata.statisticId; }
    public String getUnitOfMeasure() { return metadata.unitOfMeasure; }
    public String getMonitoringLocationId() { return metadata.monitoringLocationId; }

    public T get(int index) { return values.get(index); }
    public int size() { return values.size(); }
    public boolean isEmpty() { return values.isEmpty(); }

    @Override
    public String toString() {
        return metadata.toString() + " (" + values.size() + " values)";
    }
}
