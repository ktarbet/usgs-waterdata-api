package ktarbet.usgs.waterdata;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyValueTest {

    @ParameterizedTest(name = "[{index}] input={0}")
    @CsvSource(
            value = {
                    // Empty input should return the same empty list instance.
                    "'EMPTY'|'EMPTY'",
                    // Single value should return unchanged.
                    "'2024-01-01/1.0'|'2024-01-01/1.0'",
                    // Multi-day gap is filled with undefined values.
                    "'2024-01-01/1.0,2024-01-04/4.0'|'2024-01-01/1.0,2024-01-02/U,2024-01-03/U,2024-01-04/4.0'",
                    // Single missing day between endpoints is backfilled as undefined.
                    "'2024-02-10/7.5,2024-02-12/9.25'|'2024-02-10/7.5,2024-02-11/U,2024-02-12/9.25'",
                    // Already continuous data should remain unchanged.
                    "'2024-01-01/1.0,2024-01-02/2.0,2024-01-03/3.0'|'2024-01-01/1.0,2024-01-02/2.0,2024-01-03/3.0'",
            },
            delimiter = '|'
    )
    void ensureContinuous_fromCsv(String inputSpec, String expectedSpec) {
        List<DailyValue> input = parseValues(inputSpec);
        List<DailyValue> expected = parseValues(expectedSpec);

        List<DailyValue> output = DailyValue.ensureContinuous(input);

        assertEquals(expected.size(), output.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i).date, output.get(i).date);
            assertEquals(expected.get(i).value, output.get(i).value, 0.0);
        }
    }

    private static DailyValue value(String date, double value) {
        DailyValue v = new DailyValue();
        v.date = LocalDate.parse(date);
        v.value = value;
        return v;
    }

    private static List<DailyValue> parseValues(String spec) {
        if ("EMPTY".equalsIgnoreCase(spec.trim())) {
            return new ArrayList<>();
        }
        return Arrays.stream(spec.split(","))
                .map(String::trim)
                .map(DailyValueTest::parseValue)
                .collect(Collectors.toList());
    }

    private static DailyValue parseValue(String item) {
        String[] parts = item.split("/");
        String date = parts[0].trim();
        String value = parts[1].trim();
        if ("U".equalsIgnoreCase(value)) {
            return value(date, UsgsWaterDataApi.UNDEFINED_DOUBLE);
        }
        return value(date, Double.parseDouble(value));
    }
}
