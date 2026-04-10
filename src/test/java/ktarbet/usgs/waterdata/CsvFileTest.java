package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvFileTest {

    static DataTable csv;

    @BeforeAll
    static void loadCsv() throws Exception {
        Path csvPath = Path.of(CsvFileTest.class.getResource("/monitoring-locations.csv").toURI());
        csv = new CsvFile(csvPath);
    }

    @Test
    void parseLine_handlesQuotedCommas() throws Exception {
        assertEquals(42, csv.getColumnCount());
        assertEquals("monitoring_location_name", csv.getColumnNames()[6]);

        // row index 2 (third data row) has a quoted name with a comma
        assertEquals("COLORADO RIVER AT NEEDLES, CA",
                csv.get(2, "monitoring_location_name"));
        assertEquals("USGS-09423500", csv.get(2, "id"));
    }

    @Test
    void parseLine_handlesUnquotedFields() throws Exception {
        // first data row - no quotes
        assertEquals("USGS-09423350", csv.get(0, "id"));
        assertEquals("CARUTHERS C NR IVANPAH CA",
                csv.get(0, "monitoring_location_name"));
        assertEquals("U.S. Geological Survey", csv.get(0, "agency_name"));
    }

    @Test
    void rowCount() throws Exception {
        assertTrue(csv.getRowCount() > 100, "Expected many rows in monitoring-locations.csv");
    }

    @Test
    void unknownColumnReturnsEmpty() throws Exception {
        assertEquals("", csv.get(0, "bogus_column"));
    }

    @Test
    void mapRows_monitoringLocation() throws Exception {
        List<MonitoringLocation> locations = csv.mapRows(MonitoringLocation::fromRow);
        assertFalse(locations.isEmpty());
        MonitoringLocation first = locations.get(0);
        assertEquals("USGS-09423350", first.id);
        assertEquals("CARUTHERS C NR IVANPAH CA", first.monitoringLocationName);
        assertEquals("U.S. Geological Survey", first.agencyName);

        // row with quoted comma
        MonitoringLocation colorado = locations.get(2);
        assertEquals("COLORADO RIVER AT NEEDLES, CA", colorado.monitoringLocationName);
    }

    @Test
    void mapRows_daily() throws Exception {
        Path dailyPath = Path.of(CsvFileTest.class.getResource("/daily.csv").toURI());
        List<DailyValue> values = new CsvFile(dailyPath).mapRows(DailyValue::fromRow);
        assertFalse(values.isEmpty());
        assertEquals(LocalDate.of(2018, 2, 12), values.get(0).date);
        assertEquals(277.0, values.get(0).value, 0.0);
    }

    @Test
    void mapRows_timeSeriesMetadata() throws Exception {
        Path metaPath = Path.of(CsvFileTest.class.getResource("/time-series-metadata.csv").toURI());
        List<TimeSeriesMetadata> metadata = new CsvFile(metaPath).mapRows(TimeSeriesMetadata::fromRow);
        assertFalse(metadata.isEmpty());
        assertEquals("00095", metadata.get(0).parameterCode);
        assertEquals("Specific cond at 25C", metadata.get(0).parameterName);
        assertEquals("USGS-13037500", metadata.get(0).monitoringLocationId);
    }

    @Test
    void filterDaily_USGS13186000() throws Exception {
        Path metaPath = Path.of(CsvFileTest.class.getResource("/time-series-metadata_USGS-13186000.csv").toURI());
        List<TimeSeriesMetadata> all = new CsvFile(metaPath).mapRows(TimeSeriesMetadata::fromRow);
        assertEquals(7, all.size());

        List<TimeSeriesMetadata> daily = TimeSeriesMetadata.filter(all).daily().toList();
        assertEquals(1, daily.size());
        TimeSeriesMetadata ts = daily.get(0);
        assertEquals("Discharge", ts.parameterName);
        assertEquals("00060", ts.parameterCode);
        assertEquals("Mean", ts.computationIdentifier);
        assertEquals("Daily", ts.computationPeriodIdentifier);
        assertEquals("00003", ts.statisticId);
        assertEquals("USGS-13186000", ts.monitoringLocationId);
    }

    @Test
    void alaskaMonitoringLocations_allStartWithUSGS() throws Exception {
        Path alaskaPath = Path.of(CsvFileTest.class.getResource("/monitoring-locations_Alaska.csv").toURI());
        List<MonitoringLocation> locations = new CsvFile(alaskaPath).mapRows(MonitoringLocation::fromRow);
        assertFalse(locations.isEmpty());
        for (MonitoringLocation loc : locations) {
            assertTrue(loc.id.startsWith("USGS-"),
                    "Expected id to start with 'USGS-' but was: " + loc.id);
        }
    }

    @Test
    void parseLine_handlesEmbeddedDoubleQuotes() throws Exception {
        java.nio.file.Path tmp = java.nio.file.Files.createTempFile("test", ".csv");
        java.nio.file.Files.writeString(tmp, "name,desc\n\"Alice\",\"She said \"\"hello\"\"\"\n");

        DataTable csv = new CsvFile(tmp);
        assertEquals("Alice", csv.get(0, "name"));
        assertEquals("She said \"hello\"", csv.get(0, "desc"));

        java.nio.file.Files.delete(tmp);
    }
}
