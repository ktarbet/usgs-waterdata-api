package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesFilterTest {

    static List<TimeSeriesMetadata> snakeRiver;   // USGS-13037500, 9 rows
    static List<TimeSeriesMetadata> boiseRiver;    // USGS-13186000, 7 rows

    @BeforeAll
    static void loadTestData() throws Exception {
        snakeRiver = new CsvFile("src/test/resources/time-series-metadata_USGS-13037500.csv")
                .mapRows(TimeSeriesMetadata::fromRow);
        boiseRiver = new CsvFile("src/test/resources/time-series-metadata_USGS-13186000.csv")
                .mapRows(TimeSeriesMetadata::fromRow);
    }

    @Test
    void parameterCode_single() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.DISCHARGE).toList();
        assertEquals(3, results.size());
        for (var ts : results) {
            assertEquals(Parameter.DISCHARGE, ts.parameterCode);
        }
    }

    @Test
    void parameterCode_multiple() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.DISCHARGE, Parameter.STAGE).toList();
        assertEquals(5, results.size());
        for (var ts : results) {
            assertTrue(ts.parameterCode.equals(Parameter.DISCHARGE)
                    || ts.parameterCode.equals(Parameter.STAGE));
        }
    }

    @Test
    void statisticId_single() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .statisticId(Statistic.MEAN).toList();
        assertEquals(2, results.size());
    }

    @Test
    void chained_parameterCode_and_statisticId() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.DISCHARGE)
                .statisticId(Statistic.MEAN)
                .toList();
        assertEquals(1, results.size());
        assertEquals("Mean", results.get(0).computationIdentifier);
    }

    @Test
    void daily_matchesExpected() {
        // USGS-13186000: only 1 Daily Mean series (Discharge)
        var daily = TimeSeriesMetadata.filter(boiseRiver).daily().toList();
        assertEquals(1, daily.size());
        assertEquals("Discharge", daily.get(0).parameterName);
        assertEquals("Daily", daily.get(0).computationPeriodIdentifier);
        assertEquals("Mean", daily.get(0).computationIdentifier);
    }

    @Test
    void daily_snakeRiver() {
        // USGS-13037500: Daily Instantaneous (Specific cond), Daily Mean (Temp, Discharge) = 3
        var daily = TimeSeriesMetadata.filter(snakeRiver).daily().toList();
        assertEquals(3, daily.size());
    }

    @Test
    void daily_withDateRange() {
        var daily = TimeSeriesMetadata.filter(snakeRiver).daily()
                .dateRange(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31))
                .toList();
        // Only Discharge Mean has dates spanning 1910-2026, so it overlaps 2020
        // Specific cond ends 1972, Temp Mean ends 1999 — neither overlap 2020
        assertEquals(1, daily.size());
        assertEquals(Parameter.DISCHARGE, daily.get(0).parameterCode);
    }

    @Test
    void computationPeriod() {
        var waterYear = TimeSeriesMetadata.filter(snakeRiver)
                .computationPeriod("Water Year").toList();
        assertEquals(2, waterYear.size());
        for (var ts : waterYear) {
            assertEquals("Water Year", ts.computationPeriodIdentifier);
        }
    }

    @Test
    void computation() {
        var mean = TimeSeriesMetadata.filter(snakeRiver)
                .computation("Mean").toList();
        assertEquals(2, mean.size());
    }

    @Test
    void hasDateRange() {
        var withDates = TimeSeriesMetadata.filter(snakeRiver).hasDateRange().toList();
        for (var ts : withDates) {
            assertNotNull(ts.begin);
            assertNotNull(ts.end);
        }
        // 2 Water Year entries have no begin/end
        assertEquals(snakeRiver.size() - 2, withDates.size());
    }

    @Test
    void hasStatistic() {
        var withStat = TimeSeriesMetadata.filter(snakeRiver).hasStatistic().toList();
        // 2 Water Year entries have no statisticId
        assertEquals(snakeRiver.size() - 2, withStat.size());
    }

    @Test
    void unitOfMeasure() {
        var degC = TimeSeriesMetadata.filter(snakeRiver)
                .unitOfMeasure("degC").toList();
        assertEquals(3, degC.size());
    }

    @Test
    void monitoringLocationId() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .monitoringLocationId("USGS-13037500").toList();
        assertEquals(snakeRiver.size(), results.size());

        var empty = TimeSeriesMetadata.filter(snakeRiver)
                .monitoringLocationId("USGS-99999999").toList();
        assertTrue(empty.isEmpty());
    }

    @Test
    void descriptionContains() {
        // Test with programmatic metadata since CSV test data has empty webDescription
        TimeSeriesMetadata ts1 = new TimeSeriesMetadata();
        ts1.webDescription = "East Fender";
        ts1.parameterCode = "00010";

        TimeSeriesMetadata ts2 = new TimeSeriesMetadata();
        ts2.webDescription = "West Side";
        ts2.parameterCode = "00010";

        TimeSeriesMetadata ts3 = new TimeSeriesMetadata();
        ts3.webDescription = null;
        ts3.parameterCode = "00010";

        var results = TimeSeriesMetadata.filter(List.of(ts1, ts2, ts3))
                .descriptionContains("fender").toList();
        assertEquals(1, results.size());
        assertEquals("East Fender", results.get(0).webDescription);
    }

    @Test
    void sublocationContains() {
        TimeSeriesMetadata ts1 = new TimeSeriesMetadata();
        ts1.sublocationIdentifier = "BGC PROJECT";

        TimeSeriesMetadata ts2 = new TimeSeriesMetadata();
        ts2.sublocationIdentifier = "Main Channel";

        TimeSeriesMetadata ts3 = new TimeSeriesMetadata();
        ts3.sublocationIdentifier = null;

        var results = TimeSeriesMetadata.filter(List.of(ts1, ts2, ts3))
                .sublocationContains("bgc").toList();
        assertEquals(1, results.size());
        assertEquals("BGC PROJECT", results.get(0).sublocationIdentifier);
    }

    @Test
    void sublocation_exactMatch() {
        TimeSeriesMetadata ts1 = new TimeSeriesMetadata();
        ts1.sublocationIdentifier = "BGC PROJECT";

        TimeSeriesMetadata ts2 = new TimeSeriesMetadata();
        ts2.sublocationIdentifier = "Main Channel";

        var results = TimeSeriesMetadata.filter(List.of(ts1, ts2))
                .sublocation("BGC PROJECT").toList();
        assertEquals(1, results.size());
    }

    @Test
    void where_customPredicate() {
        var results = TimeSeriesMetadata.filter(snakeRiver)
                .where(ts -> ts.begin != null && ts.begin.getYear() >= 1990)
                .toList();
        for (var ts : results) {
            assertTrue(ts.begin.getYear() >= 1990);
        }
    }

    @Test
    void findFirst() {
        var result = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.DISCHARGE)
                .statisticId(Statistic.MEAN)
                .findFirst();
        assertTrue(result.isPresent());
        assertEquals(Parameter.DISCHARGE, result.get().parameterCode);
    }

    @Test
    void findFirst_empty() {
        var result = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode("99999")
                .findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    void count() {
        long n = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.DISCHARGE)
                .count();
        assertEquals(3, n);
    }

    @Test
    void stream() {
        var names = TimeSeriesMetadata.filter(snakeRiver)
                .parameterCode(Parameter.WATER_TEMPERATURE)
                .stream()
                .map(ts -> ts.computationIdentifier)
                .distinct()
                .sorted()
                .toArray(String[]::new);
        assertArrayEquals(new String[]{"Max", "Mean", "Min"}, names);
    }
}
