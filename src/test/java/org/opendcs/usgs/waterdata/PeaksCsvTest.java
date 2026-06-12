package org.opendcs.usgs.waterdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for parsing the Peak Csv format.
 */
class PeaksCsvTest {

    private static final String PEAKS_CSV =
            "value,time,time_of_day,qualifier\n" +
            "12300,1922-07-12,,\n" +                                          // no time of day -> start of day
            "45600,2006-03-13,15:30,\n" +                                     // HH:mm time of day
            ",2010-05-01,,\n" +                                               // missing value -> UNDEFINED_DOUBLE
            "40300,2023-04-25,05:15:00+00:00,['REGULATED']\n" +              // offset time of day, single qualifier
            "210000,1884-01-01,,\"['ESTIMATED', 'HISTORIC', 'MONTHUNKNOWN']\"\n"; // multiple qualifiers

    private List<InstantaneousValue> parse() throws Exception {
        return CsvFile.fromString(PEAKS_CSV).mapRows(InstantaneousValue::fromPeakRow);
    }

    @ParameterizedTest
    @CsvSource({
            "1922-07-12, ,              1922-07-12T00:00:00Z",   // no time of day -> start of day
            "2006-03-13, 15:30,         2006-03-13T15:30:00Z",   // HH:mm
            "2023-04-25, 05:15:00+00:00, 2023-04-25T05:15:00Z",  // time with UTC offset
    })
    void parsePeakTime_handlesEachTimeOfDayFormat(String date, String timeOfDay, Instant expected) {
        assertEquals(expected, InstantaneousValue.parsePeakTime(date, timeOfDay));
    }

    @Test
    void valuesParsedWithMissingAsUndefined() throws Exception {
        List<InstantaneousValue> values = parse();
        assertEquals(12300.0, values.get(0).value);
        assertEquals(45600.0, values.get(1).value);
        assertEquals(UsgsWaterDataApi.UNDEFINED_DOUBLE, values.get(2).value, "missing value -> undefined");
    }

    static Stream<Arguments> qualifierCases() {
        return Stream.of(
                arguments("['REGULATED']", List.of("REGULATED")),
                arguments("['ESTIMATED', 'HISTORIC', 'MONTHUNKNOWN']", List.of("ESTIMATED", "HISTORIC", "MONTHUNKNOWN")),
                arguments("", List.of()),
                arguments(null, List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("qualifierCases")
    void parseQualifiers_handlesEachForm(String raw, List<String> expected) {
        assertEquals(expected, InstantaneousValue.parseQualifiers(raw));
    }
}
