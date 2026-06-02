package org.opendcs.usgs.waterdata;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for parsing the Peak Csv format.
 */
class PeaksCsvTest {

    private static final String PEAKS_CSV =
            "value,time,time_of_day\n" +
            "12300,1922-07-12,\n" +          // no time of day -> start of day
            "45600,2006-03-13,15:30\n" +     // HH:mm time of day
            ",2010-05-01,\n";                // missing value -> UNDEFINED_DOUBLE

    private List<InstantaneousValue> parse() throws Exception {
        return CsvFile.fromString(PEAKS_CSV).mapRows(InstantaneousValue::fromPeakRow);
    }

    @Test
    void emptyTimeOfDay_returnsStartOfDay() throws Exception {
        assertEquals(Instant.parse("1922-07-12T00:00:00Z"), parse().get(0).time);
    }

    @Test
    void timeOfDayWithHHmm_parsesCorrectly() throws Exception {
        assertEquals(Instant.parse("2006-03-13T15:30:00Z"), parse().get(1).time);
    }

    @Test
    void valuesParsedWithMissingAsUndefined() throws Exception {
        List<InstantaneousValue> values = parse();
        assertEquals(12300.0, values.get(0).value);
        assertEquals(45600.0, values.get(1).value);
        assertEquals(UsgsWaterDataApi.UNDEFINED_DOUBLE, values.get(2).value, "missing value -> undefined");
    }
}
