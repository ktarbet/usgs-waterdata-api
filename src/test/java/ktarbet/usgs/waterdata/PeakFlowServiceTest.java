package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PeakFlowServiceTest {

    static DataTable table;

    @BeforeAll
    static void loadPeaks() throws Exception {
        Path path = Path.of(PeakFlowServiceTest.class.getResource("/peaks-05495000.rdb").toURI());
        table = new RdbFile(path);
    }

    @Test
    void emptyPeakTime_returnsStartOfDay() {
        // row 0: USGS 05495000 07/12/1922  (no time)
        Instant time = PeakFlowService.parsePeakTime(table, 0);
        assertEquals(Instant.parse("1922-07-12T00:00:00Z"), time);
    }

    @Test
    void peakTimeWithHHmm_parsesCorrectly() {
        // row 84: USGS 05495000 03/13/2006 15:30
        int row = findRowByDate("03/13/2006");
        Instant time = PeakFlowService.parsePeakTime(table, row);
        assertEquals(Instant.parse("2006-03-13T15:30:00Z"), time);
    }

    @Test
    void stageAndFlowCountMatchRows() {
        // each row produces exactly one flow value and one stage value
        int expectedCount = 104;
        assertEquals(expectedCount, table.getRowCount());

        int flowCount = 0;
        int stageCount = 0;
        for (int row = 0; row < table.getRowCount(); row++) {
            double flow = table.getDouble(row, "peak_va", UsgsWaterDataApi.UNDEFINED_DOUBLE);
            if (flow != UsgsWaterDataApi.UNDEFINED_DOUBLE) flowCount++;
            double stage = table.getDouble(row, "gage_ht", UsgsWaterDataApi.UNDEFINED_DOUBLE);
            if (stage != UsgsWaterDataApi.UNDEFINED_DOUBLE) stageCount++;
        }
        assertEquals(104, flowCount, "non-missing flow values");
        assertEquals(104, stageCount, "non-missing stage values");
    }

    private int findRowByDate(String date) {
        for (int i = 0; i < table.getRowCount(); i++) {
            if (date.equals(table.get(i, "peak_dt"))) return i;
        }
        throw new AssertionError("Row not found for date: " + date);
    }
}
