package hec.usgs.waterdata;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

        MonitoringLocation russian = locations.stream()
                .filter(loc -> "11461000".equals(loc.monitoringLocationNumber))
                .findFirst()
                .orElse(null);
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
     *   <li>select specific sites of interest</li>
     *   <li>query for time-series metadata for those sites (select Flow and Gage Height parameters)</li>
     *   <li>download time-series data for a specific parameter and statistic</li>
     * </ol>
     *
     * @throws Exception
     */
    @Test
    @Tag("integration")
    void dailyData_userScenario() throws Exception {
        // ./gradlew :usgs-water-api:integrationTest --tests "hec.usgs.waterdata.UsgsWaterDataApiTest.dailyData_userScenario"
        String idaho = "16";
        List<MonitoringLocation> locations = UsgsWaterDataApi.getLocations(idaho, "ST");
        assertTrue(locations.size() > 100, "Expected many Idaho stream locations");

        // Snake River at Heise (USGS-13037500)
        MonitoringLocation snakeRiver = locations.stream()
                .filter(loc -> "13037500".equals(loc.monitoringLocationNumber))
                .findFirst()
                .orElse(null);
        assertNotNull(snakeRiver, "Expected to find Snake River at Heise (13037500)");
        assertEquals("USGS-13037500", snakeRiver.id);
        logger.info("Found: " + snakeRiver.monitoringLocationName);

        MonitoringLocation boiseRiver = locations.stream()
                .filter(loc -> "13186000".equals(loc.monitoringLocationNumber))
                .findFirst()
                .orElse(null);
        assertNotNull(boiseRiver, "Expected to find Boise River near Featherville (13186000)");
        assertEquals("USGS-13186000", boiseRiver.id);
        logger.info("Found: " + boiseRiver.monitoringLocationName);

        // query metadata for each site
        List<TimeSeriesMetadata> snakeMetadata = UsgsWaterDataApi.getTimeSeriesMetadata(snakeRiver.id);
        assertFalse(snakeMetadata.isEmpty(), "Expected time-series metadata for Snake River at Heise");
        logger.info("Snake River metadata count: " + snakeMetadata.size());
        for (TimeSeriesMetadata ts : snakeMetadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }

        List<TimeSeriesMetadata> boiseMetadata = UsgsWaterDataApi.getTimeSeriesMetadata(boiseRiver.id);
        assertFalse(boiseMetadata.isEmpty(), "Expected time-series metadata for Boise River near Featherville");
        
        logger.info("Boise River metadata count: " + boiseMetadata.size());
        for (TimeSeriesMetadata ts : boiseMetadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }
        boiseMetadata = TimeSeriesMetadata.filterDaily(boiseMetadata);
        logger.info("Filtered to " + boiseMetadata.size() + " Daily time-series with statistic");
        for (TimeSeriesMetadata ts : boiseMetadata) {
            logger.info("  " + ts.parameterCode + " " + ts.parameterName
                    + " [" + ts.unitOfMeasure + "] " + ts.begin + " to " + ts.end);
        }
        assertEquals(1,boiseMetadata.size());
        for (TimeSeriesMetadata ts : boiseMetadata) {
            List<DailyValue> dailyValues = UsgsWaterDataApi.getDailyTimeSeries(boiseRiver.id, ts.parameterCode, ts.statisticId, LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
            assertFalse(dailyValues.isEmpty(), "Expected daily time-series data for " + ts.parameterName);
            logger.info("  " + ts.parameterName + " has " + dailyValues.size() + " daily values in 2020");
            assertEquals(366, dailyValues.size(), "Expected 366 daily values for 2020 (leap year)");
        }
        
    }
}
