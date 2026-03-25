package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class UsgsWaterDataApiTest {

    private static final Logger logger = Logger.getLogger(UsgsWaterDataApiTest.class.getName());

    @Test
    @Tag("integration")
    void getLocations_californiaStreams() throws Exception {
        List<MonitoringLocation> locations = UsgsWaterDataApi.getLocations(StateLookup.getStateCode("CA"), "ST");
        assertTrue(locations.size() > 100, "Expected many California stream locations");
        assertEquals("USGS", locations.get(0).agencyCode);

        MonitoringLocation russian = MonitoringLocation.findByNumber(locations, "11461000");
        assertNotNull(russian, "Expected to find monitoring_location_number 11461000");
        assertEquals("RUSSIAN R NR UKIAH CA", russian.monitoringLocationName);
    }


    @Test
    @Tag("integration")
    void getMetaDatafor_SnakeRiverAtHeise_13037500() throws Exception {
        var metadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-13037500");
        assertFalse(metadata.isEmpty());

    }


    /**
     * Tests scenario of user looking for Daily data for Idaho streams.
     * <ol>
     *   <li>query for monitoring locations in Idaho</li>
     *   <li>select specific site of interest</li>
     *   <li>query for time-series metadata for those sites (select Flow and Gage Height parameters)</li>
     *   <li>download time-series data for a specific parameter and statistic</li>
     * </ol>
     *
     * ./gradlew :usgs-water-api:integrationTest --tests "ktarbet.usgs.waterdata.UsgsWaterDataApiTest.dailyData_userScenario" -PusgsDebug=true
     * 
     * @throws Exception
     */
    @ParameterizedTest(name = "{2} ({0})")
    @CsvSource({
            "13037500, USGS-13037500, Snake River at Heise, 366",
            "13186000, USGS-13186000, Boise River near Featherville, 366"
    })
    @Tag("integration")
    void dailyData_userScenario(String locationNumber, String expectedId, String displayName,
                                int expectedDailyValues2020) throws Exception {
        String idaho = "16";
        var t1= LocalDate.of(2020, 1, 1);
        var t2 = LocalDate.of(2020, 12, 31);

        List<MonitoringLocation> locations = UsgsWaterDataApi.getLocations(idaho, "ST");
        assertTrue(locations.size() > 100, "Expected many Idaho stream locations");

        MonitoringLocation location = MonitoringLocation.findByNumber(locations, locationNumber);
        assertNotNull(location, "Expected to find " + displayName + " (" + locationNumber + ")");
        assertEquals(expectedId, location.id);
        logger.info("Found: " + location.monitoringLocationName);

        List<TimeSeriesMetadata> metadata = UsgsWaterDataApi.getTimeSeriesMetadata(location.id);
        assertFalse(metadata.isEmpty(), "Expected time-series metadata for " + displayName);
        logger.info(displayName + " metadata count: " + metadata.size());
        for (TimeSeriesMetadata ts : metadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }

        metadata = TimeSeriesMetadata.filterDaily(metadata, t1, t2);
        logger.info("Filtered to " + metadata.size() + " Daily time-series");
        for (TimeSeriesMetadata ts : metadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }
        assertTrue(metadata.size()>0, "Expected some Daily time-series metadata for " + displayName);

        for (TimeSeriesMetadata ts : metadata) {
            TimeSeries<DailyValue> dailyTimeSeries = UsgsWaterDataApi.getDailyTimeSeries(ts, t1.toString(), t2.toString());
            assertFalse(dailyTimeSeries.isEmpty(), "Expected daily time-series data for " + ts.parameterName);
            logger.info("  " + ts.parameterName + " has " + dailyTimeSeries.size() + " daily values in 2020");
            assertEquals(expectedDailyValues2020, dailyTimeSeries.size(), "Expected " + expectedDailyValues2020 + " daily values for 2020 (leap year)");
        }
    }

    @Test
    @Tag("integration")
    void dailyData_ShellpotCreek() throws Exception {
        List<TimeSeriesMetadata> metadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-01477800");
        metadata = TimeSeriesMetadata.filterDaily(metadata);
        for (TimeSeriesMetadata ts : metadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }
        var shellpot = metadata.get(0);
        var ts = UsgsWaterDataApi.getDailyTimeSeries(shellpot,
             LocalDate.of(2025, 2, 26).toString(),
              LocalDate.of(2026, 2, 26).toString());
        for (int i = 0; i < Math.min(4, ts.size()); i++) {
            DailyValue dv = ts.get(i);
            logger.info("  " + dv.date + " = " + dv.value);
        }
    }

    /**
     * This test demonstrates the ability to retrieve time-series metadata for a large number of
     *  monitoring locations efficently using the POST endpoint. 
     * @throws Exception
     */
    @Test
    @Tag("integration")
    void getMetadata_many_locations() throws Exception {
        CsvFile csv = new CsvFile(Path.of("src/test/resources/monitoring-locations.csv"));
        int count = csv.getRowCount();
        String[] ids = new String[count];
        for (int i = 0; i < count; i++) {
            ids[i] = csv.get(i, "id");
        }

        var metadataMap = UsgsWaterDataApi.getTimeSeriesMetadata(ids);
        assertFalse(metadataMap.isEmpty(), "Expected metadata for at least some locations");
        assertTrue(metadataMap.size() > 1000, "Expected metadata for most of the " + count + " locations");
        logger.info("Queried " + count + " locations, got metadata for " + metadataMap.size());
    }


    /**
     * This test demonstrates the ability to retrieve continuous time-series data for a specific parameter and statistic, 
     */
    @Test
    @Tag("integration")
    void getContinuousTimeSeries() throws Exception {

        String location_id = "USGS-08068800";
        String parameter = Parameter.STAGE;
        String statistic = Statistic.INSTANTANEOUS;
        // Partial-day query: 6 hours should return fewer values than a full day
        String t1 = "2026-01-15T06:00:00Z";
        String t2 = "2026-01-15T12:00:00Z";

        var metadata = UsgsWaterDataApi.getTimeSeriesMetadata(location_id).stream()
                .filter(ts -> ts.parameterCode.equals(parameter) && ts.statisticId.equals(statistic))
                .findFirst().orElseThrow();

        var continuousTimeSeries = UsgsWaterDataApi.getContinuousTimeSeries(metadata, t1, t2);

        for (int i = 0; i < Math.min(5, continuousTimeSeries.size()); i++) {
            InstantaneousValue iv = continuousTimeSeries.get(i);
            logger.info("  " + iv.time + " = " + iv.value);
        }

        // 15-minute intervals over 6 hours = 25 values (inclusive of both endpoints)
        assertEquals(25, continuousTimeSeries.size());
        // first value at start of partial-day window
        assertEquals(OffsetDateTime.parse("2026-01-15T06:00:00Z").toInstant(), continuousTimeSeries.get(0).time);
        // last value at end of partial-day window
        assertEquals(OffsetDateTime.parse("2026-01-15T12:00:00Z").toInstant(), continuousTimeSeries.get(continuousTimeSeries.size()-1).time);

        // Also test daily via metadata
        var dailyMetadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-13213000").stream()
                .filter(ts -> ts.parameterCode.equals(Parameter.DISCHARGE) && ts.statisticId.equals(Statistic.MEAN))
                .findFirst().orElseThrow();

        var dailyTimeSeries = UsgsWaterDataApi.getDailyTimeSeries(dailyMetadata,
                                                                    "2026-01-01T00:00:00Z", "2026-01-05T00:00:00Z");

        for (int i = 0; i < dailyTimeSeries.size(); i++) {
            DailyValue dv = dailyTimeSeries.get(i);
            logger.info("  " + dv.date + " = " + dv.value);
        }

    }

     /**
     * This test demonstrates the ability to retrieve continuous time-series data for 
     * USGS 11447650 SACRAMENTO R A FREEPORT CA
     * 
     * This site has a two different time-series with the same parameter and statistic (00010 Temperature, water)
     * 
     */
    @Test
    @Tag("integration")
    void getContinuousTimeSeriesDuplicateStatistic() throws Exception {

        String location_id = "USGS-11447650";
        String parameter = Parameter.WATER_TEMPERATURE;
        String statistic = Statistic.INSTANTANEOUS;
        String t1 = "2026-03-15T00:00:00Z";
        String t2 = "2026-03-19T23:59:00Z";

        var timeSeriesMetadata = UsgsWaterDataApi.getTimeSeriesMetadata(location_id).stream()
                .filter(ts -> ts.parameterCode.equals(parameter) && ts.statisticId.equals(statistic) && ts.begin != null)
                .collect(Collectors.toList());


                timeSeriesMetadata.forEach(ts -> logger.info("Metadata: " + ts.monitoringLocationId + " " + ts.parameterCode + " " + ts.statisticId
                       + ts.webDescription+" "+ "[" + ts.sublocationIdentifier + "] "
                        + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end));
                        

        for (TimeSeriesMetadata ts : timeSeriesMetadata) {
            var continuousTimeSeries = UsgsWaterDataApi.getContinuousTimeSeries(ts, t1, t2);
            logger.info("  " + ts.sublocationIdentifier + " has " + continuousTimeSeries.size() + " values");
        }
        


    }

    /**
     * Tests retrieving annual peak streamflow from the legacy NWIS RDB service.
     *
     * ./gradlew integrationTest --tests "ktarbet.usgs.waterdata.UsgsWaterDataApiTest.getAnnualPeaks_FoxRiver" -PusgsDebug=true
     */
    @Test
    @Tag("integration")
    void getAnnualPeaks_FoxRiver() throws Exception {
        // Fox River at Wayland, MO — site 05495000 (same site from the RDB example)
        var siteMetadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-05495000");

        List<TimeSeries<InstantaneousValue>> peakSeries = UsgsWaterDataApi.getAnnualPeaks(siteMetadata);
        assertEquals(2, peakSeries.size(), "Expected discharge and stage series");

        TimeSeries<InstantaneousValue> flowPeaks = peakSeries.get(0);
        TimeSeries<InstantaneousValue> stagePeaks = peakSeries.get(1);

        assertFalse(flowPeaks.isEmpty(), "Expected annual peak flow values");
        assertFalse(stagePeaks.isEmpty(), "Expected annual peak stage values");
        logger.info("Annual peaks count: " + flowPeaks.size());

        assertEquals(Parameter.DISCHARGE, flowPeaks.metadata.parameterCode);
        assertEquals(Parameter.STAGE, stagePeaks.metadata.parameterCode);
        assertEquals("Water Year", flowPeaks.metadata.computationPeriodIdentifier);
        assertEquals("Max At Event Time", flowPeaks.metadata.computationIdentifier);

        // Verify units come from metadata, not hardcoded
        assertNotNull(flowPeaks.metadata.unitOfMeasure, "Flow units should come from metadata");
        assertNotNull(stagePeaks.metadata.unitOfMeasure, "Stage units should come from metadata");
        logger.info("Flow units: " + flowPeaks.metadata.unitOfMeasure);
        logger.info("Stage units: " + stagePeaks.metadata.unitOfMeasure);

        // First peak should be from 1922
        InstantaneousValue first = flowPeaks.get(0);
        logger.info("First peak: " + first.time + " = " + first.value);
        assertEquals(1922, LocalDate.ofInstant(first.time, java.time.ZoneOffset.UTC).getYear());

        for (int i = 0; i < Math.min(5, flowPeaks.size()); i++) {
            InstantaneousValue flow = flowPeaks.get(i);
            InstantaneousValue stage = stagePeaks.get(i);
            logger.info("  " + flow.time + "  flow=" + flow.value + "  stage=" + stage.value);
        }
    }

    @Test
    @Tag("integration")
    void getAnnualPeaks_withDateRange() throws Exception {
        var siteMetadata = UsgsWaterDataApi.getTimeSeriesMetadata("USGS-05495000");

        List<TimeSeries<InstantaneousValue>> peakSeries =
                UsgsWaterDataApi.getAnnualPeaks(siteMetadata, "2000-01-01", "2010-12-31");
        assertFalse(peakSeries.isEmpty());

        TimeSeries<InstantaneousValue> flowPeaks = peakSeries.get(0);
        assertFalse(flowPeaks.isEmpty(), "Expected peaks in 2000-2010 range");
        logger.info("Peaks in range: " + flowPeaks.size());

        for (InstantaneousValue iv : flowPeaks.values) {
            int year = LocalDate.ofInstant(iv.time, java.time.ZoneOffset.UTC).getYear();
            assertTrue(year >= 2000 && year <= 2011, "Peak year " + year + " outside expected range");
        }
    }

}
