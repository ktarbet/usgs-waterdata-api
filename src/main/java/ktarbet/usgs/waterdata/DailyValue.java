package ktarbet.usgs.waterdata;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DailyValue {

    public LocalDate date;
    public double value;

    static DailyValue fromRow(DataTable table, int row) {
        DailyValue v = new DailyValue();
        v.date = LocalDate.parse(table.get(row, "time"));
        v.value = table.getDouble(row, "value", UsgsWaterDataApi.UNDEFINED_DOUBLE);
        return v;
    }

    public DailyValue() {
    }

    public DailyValue(LocalDate date, double value) {
        this.date = date;
        this.value = value;
    }

    /**
     * Constructor for a missing value, which creates a new DailyValue
     * with the specified date and a default undefined value.
     */
    public DailyValue(LocalDate date) {
        this.date = date;
        this.value = UsgsWaterDataApi.UNDEFINED_DOUBLE;
    }

    /**
     * Ensures a list of DailyValue objects has a continuous date range, filling in
     * any missing dates with a default value.
     * <p>
     * This method assumes the input list is sorted by date in ascending order.
     *
     * @param values A list of {@link DailyValue} objects, sorted by date.
     * @return A new list with a continuous sequence of dates
     *         Returns the original list if it's null, empty, or has only one element.
     */
    @Override
    public String toString() {
        return date + " = " + value;
    }

    public static List<DailyValue> ensureContinuous(List<DailyValue> values) {
        if (values == null || values.size() <= 1) {
            return values;
        }

        LocalDate startDate = values.get(0).date;
        LocalDate endDate = values.get(values.size() - 1).date;

        Map<LocalDate, DailyValue> valuesByDate = values.stream()
                .collect(Collectors.toMap(dv -> dv.date, dv -> dv));

        return Stream.iterate(startDate, date -> !date.isAfter(endDate), date -> date.plusDays(1))
                .map(date -> valuesByDate.computeIfAbsent(date, DailyValue::new))
                .collect(Collectors.toList());
    }   

}
