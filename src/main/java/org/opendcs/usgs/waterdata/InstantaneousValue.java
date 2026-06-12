package org.opendcs.usgs.waterdata;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
public class InstantaneousValue {

    public Instant time;
    public double value;

    /**
     * Qualifier Strings that interpret a peak value (e.g. {@code ESTIMATED},
     * {@code REGULATED}, {@code MONTHUNKNOWN}); empty for non-peak values.
     */
    public List<String> qualifiers = Collections.emptyList();


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
        v.qualifiers = parseQualifiers(table.get(row, "qualifier"));
        return v;
    }

    static Instant parsePeakTime(String dateStr, String timeStr) {
        LocalDate date = LocalDate.parse(dateStr);
        if (timeStr != null && !timeStr.isBlank()) {
            String t = timeStr.trim();
            try {
                // Recent peaks carry an offset (e.g. "01:45:00+00:00"); older ones do not (e.g. "15:30").
                OffsetTime ot = OffsetTime.parse(t);
                return date.atTime(ot.toLocalTime()).toInstant(ot.getOffset());
            } catch (DateTimeParseException offsetMiss) {
                try {
                    return date.atTime(LocalTime.parse(t)).toInstant(ZoneOffset.UTC);
                } catch (DateTimeParseException e) {
                    // fall through to date-only
                }
            }
        }
        return date.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    /**
     * Parses the peaks qualifier field, such
     * as {@code ['ESTIMATED', 'REGULATED']}, into a list of strings.
     */
    static List<String> parseQualifiers(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String cleaned = raw.replaceAll("[\\[\\]'\"]", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return time + " = " + value + (qualifiers.isEmpty() ? "" : " " + qualifiers);
    }

    public InstantaneousValue() {
    }

    public InstantaneousValue(Instant time, double value) {
        this.time = time;
        this.value = value;
    }

}
