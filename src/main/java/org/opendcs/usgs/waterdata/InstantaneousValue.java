package org.opendcs.usgs.waterdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    /**
     * Builds a value from a peaks-collection row, combining time and time_of_day.
     */
    static InstantaneousValue fromPeakRow(DataTable table, int row) {
        InstantaneousValue v = new InstantaneousValue();
        v.time = parsePeakTime(table.get(row, "time"), table.get(row, "time_of_day"));
        v.value = table.getDouble(row, "value", UsgsWaterDataApi.UNDEFINED_DOUBLE);
        return v;
    }

    static Instant parsePeakTime(String dateStr, String timeStr) {
        LocalDate date = LocalDate.parse(dateStr);
        if (timeStr != null && !timeStr.isBlank()) {
            try {
                return date.atTime(LocalTime.parse(timeStr.trim())).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                // fall through to date-only
            }
        }
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
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
