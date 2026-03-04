package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UsgsWaterDataApiTest {

    private static final Logger logger = Logger.getLogger(UsgsWaterDataApiTest.class.getName());

    @Test
    @Tag("integration")
    void getLocations_californiaStreams() throws Exception {
        List<MonitoringLocation> locations = UsgsWaterDataApi.getLocations("06", "ST");
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
            List<DailyValue> dailyValues = UsgsWaterDataApi.getDailyTimeSeries(location.id, ts.parameterCode, ts.statisticId, t1, t2);
            assertFalse(dailyValues.isEmpty(), "Expected daily time-series data for " + ts.parameterName);
            logger.info("  " + ts.parameterName + " has " + dailyValues.size() + " daily values in 2020");
            assertEquals(expectedDailyValues2020, dailyValues.size(), "Expected " + expectedDailyValues2020 + " daily values for 2020 (leap year)");
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
        var shellpot= metadata.get(0);
        var ts = UsgsWaterDataApi.getDailyTimeSeries(shellpot.monitoringLocationId, shellpot.parameterCode, shellpot.statisticId,
             LocalDate.of(2025, 2, 26),
              LocalDate.of(2026, 2, 26));
        for (int i = 0; i < Math.min(4, ts.size()); i++) {
            DailyValue dv = ts.get(i);
            logger.info("  " + dv.date + " = " + dv.value);
        }
    }

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

}
