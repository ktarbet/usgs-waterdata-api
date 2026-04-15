package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests using the in-memory {@link TestSite} (fake site 88888888).
 * No HTTP calls are made — all data is generated deterministically.
 */
class TestSiteTest {

    private static final Logger logger = Logger.getLogger(TestSiteTest.class.getName());

    /**
     * Demonstrates querying the test site for water temperature time-series
     * and using sublocationIdentifier to distinguish between "Left Bank" and "Right Bank".
     */
    @Test
    void filterBySublocationIdentifier() throws Exception {
        List<TimeSeriesMetadata> metadata = UsgsWaterDataApi.getTimeSeriesMetadata(TestSite.MONITORING_LOCATION_ID);
        assertFalse(metadata.isEmpty());

        // 2. Filter to water temperature — should return both Left Bank and Right Bank
        var tempSeries = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.WATER_TEMPERATURE)
                .computationPeriod("Instantaneous")
                .toList();
        assertEquals(2, tempSeries.size(), "Expected two water temperature time-series (Left Bank and Right Bank)");

        var leftBank = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.WATER_TEMPERATURE)
                .sublocation("Left Bank")
                .findFirst().orElseThrow();
        assertEquals("Left Bank", leftBank.sublocationIdentifier);
        assertEquals(TestSite.CONTINUOUS_TEMP_LEFT_TS_ID, leftBank.id);

        var rightBank = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.WATER_TEMPERATURE)
                .sublocation("Right Bank")
                .findFirst().orElseThrow();
        assertEquals("Right Bank", rightBank.sublocationIdentifier);
        assertEquals(TestSite.CONTINUOUS_TEMP_RIGHT_TS_ID, rightBank.id);

        // 5. Retrieve continuous data for each sublocation and verify
        String t1 = "2025-06-01T00:00:00Z";
        String t2 = "2025-06-01T06:00:00Z";

        TimeSeries<InstantaneousValue> leftTS = UsgsWaterDataApi.getContinuousTimeSeries(leftBank, t1, t2);
        TimeSeries<InstantaneousValue> rightTS = UsgsWaterDataApi.getContinuousTimeSeries(rightBank, t1, t2);

        // 15-minute intervals over 6 hours = 24 values
        assertEquals(24, leftTS.size());
        assertEquals(24, rightTS.size());

        // First value at midnight should have hour=0 as its value
        assertEquals(0, leftTS.get(0).value);
        assertEquals(0, rightTS.get(0).value);

        logger.info("Left Bank (" + leftBank.sublocationIdentifier + ") values: " + leftTS.size());
        logger.info("Right Bank (" + rightBank.sublocationIdentifier + ") values: " + rightTS.size());
        leftTS.printToConsole(5);
    }

    /**
     * Demonstrates using sublocationContains for case-insensitive partial matching.
     */
    @Test
    void filterBySublocationContains() throws Exception {
        List<TimeSeriesMetadata> metadata = UsgsWaterDataApi.getTimeSeriesMetadata(TestSite.MONITORING_LOCATION_ID);

        // Case-insensitive partial match on "bank" should return both temperature series
        var bankSeries = TimeSeriesMetadata.filter(metadata)
                .sublocationContains("bank")
                .toList();
        assertEquals(2, bankSeries.size());

        // Partial match on "left" should return only the Left Bank series
        var leftOnly = TimeSeriesMetadata.filter(metadata)
                .sublocationContains("left")
                .findFirst().orElseThrow();
        assertEquals("Left Bank", leftOnly.sublocationIdentifier);
    }

    /**
     * Reads daily and continuous time-series data from the test site
     * and verifies the deterministic values.
     */
    @Test
    void readTimeSeriesData() throws Exception {
        List<TimeSeriesMetadata> metadata = UsgsWaterDataApi.getTimeSeriesMetadata(TestSite.MONITORING_LOCATION_ID);

        // --- Daily discharge: value = day of month ---
        var dischargeMeta = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.DISCHARGE)
                .statisticId(Statistic.MEAN)
                .findFirst().orElseThrow();

        TimeSeries<DailyValue> daily = UsgsWaterDataApi.getDailyTimeSeries(dischargeMeta,
                "2025-03-10", "2025-03-14");

        assertEquals(5, daily.size());
        assertEquals("ft^3/s", daily.getUnitOfMeasure());
        assertEquals(10, daily.get(0).value); // 2025-03-10 = 10.0
        assertEquals(11, daily.get(1).value); // 2025-03-11 = 11.0
        assertEquals(12, daily.get(2).value); // 2025-03-12 = 12.0
        assertEquals(13, daily.get(3).value); // 2025-03-13 = 13.0
        assertEquals(14, daily.get(4).value); // 2025-03-14 = 14.0
        assertEquals(LocalDate.of(2025, 3, 12), daily.get(2).date);

        // --- Continuous gage height: value = hour of day ---
        var gageMeta = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.STAGE)
                .findFirst().orElseThrow();

        TimeSeries<InstantaneousValue> continuous = UsgsWaterDataApi.getContinuousTimeSeries(gageMeta,
                "2025-07-01T00:00:00Z", "2025-07-01T03:00:00Z");

        // 15-minute intervals over 3 hours = 12 values
        assertEquals(12, continuous.size());
        assertEquals("ft", continuous.getUnitOfMeasure());
        assertEquals(0, continuous.get(0).value);  // 00:00 = 0.0
        assertEquals(0, continuous.get(1).value);  // 00:15 = 0.0
        assertEquals(0, continuous.get(2).value);  // 00:30 = 0.0
        assertEquals(0, continuous.get(3).value);  // 00:45 = 0.0
        assertEquals(1, continuous.get(4).value);  // 01:00 = 1.0
        assertEquals(1, continuous.get(5).value);  // 01:15 = 1.0
        assertEquals(1, continuous.get(6).value);  // 01:30 = 1.0
        assertEquals(1, continuous.get(7).value);  // 01:45 = 1.0
        assertEquals(2, continuous.get(8).value);  // 02:00 = 2.0
        assertEquals(2, continuous.get(9).value);  // 02:15 = 2.0
        assertEquals(2, continuous.get(10).value); // 02:30 = 2.0
        assertEquals(2, continuous.get(11).value); // 02:45 = 2.0

        // --- Continuous temperature filtered by sublocation ---
        var leftBankMeta = TimeSeriesMetadata.filter(metadata)
                .parameterCode(Parameter.WATER_TEMPERATURE)
                .sublocation("Left Bank")
                .findFirst().orElseThrow();

        TimeSeries<InstantaneousValue> temp = UsgsWaterDataApi.getContinuousTimeSeries(leftBankMeta,
                "2025-07-01T12:00:00Z", "2025-07-01T13:00:00Z");

        // 15-minute intervals over 1 hour = 4 values
        assertEquals(4, temp.size());
        assertEquals("deg C", temp.getUnitOfMeasure());
        assertEquals(12, temp.get(0).value); // 12:00 = 12.0
        assertEquals(12, temp.get(1).value); // 12:15 = 12.0
        assertEquals(12, temp.get(2).value); // 12:30 = 12.0
        assertEquals(12, temp.get(3).value); // 12:45 = 12.0
    }

}