package ktarbet.usgs.waterdata;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
public class InstantaneousValue {

    public Instant time;
    public double value;


    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ssXXX")
            .withZone(java.time.ZoneId.systemDefault());

    private static Instant parse(String timeStr) {
        try { // Failed to parse time: 2026-01-15 00:00:00+00:00
            return Instant.from(formatter.parse(timeStr));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse time: " + timeStr, e);
        }
    }


    static InstantaneousValue fromRow(DataTable table, int row) {
        InstantaneousValue v = new InstantaneousValue();
        v.time = parse(table.get(row, "time")); 
        v.value = table.getDouble(row, "value", UsgsWaterDataApi.UNDEFINED_DOUBLE);
        return v;
    }

    @Override
    public String toString() {
        return time + " = " + value;
    }

    public InstantaneousValue() {
    }

    public InstantaneousValue(Instant time, double value) {
        this.time = time;
        this.value = value;
    }

}
