package ktarbet.usgs.waterdata;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RdbFileTest {

    static DataTable rdb;

    @BeforeAll
    static void loadRdb() throws Exception {
        Path rdbPath = Path.of(RdbFileTest.class.getResource("/peaks.rdb").toURI());
        rdb = new RdbFile(rdbPath);
    }

    @Test
    void columnNames() {
        String[] cols = rdb.getColumnNames();
        assertEquals("agency_cd", cols[0]);
        assertEquals("site_no", cols[1]);
        assertEquals("peak_dt", cols[2]);
        assertEquals("peak_tm", cols[3]);
        assertEquals("peak_va", cols[4]);
    }

    @Test
    void rowCount() {
        assertEquals(3, rdb.getRowCount());
    }

    @Test
    void dataValues() {
        assertEquals("USGS", rdb.get(0, "agency_cd"));
        assertEquals("05495000", rdb.get(0, "site_no"));
        assertEquals("07/12/1922", rdb.get(0, "peak_dt"));
        assertEquals(2400.0, rdb.getDouble(0, "peak_va", -1), 0.0);
        assertEquals(11.90, rdb.getDouble(0, "gage_ht", -1), 0.001);
    }

    @Test
    void secondRow() {
        assertEquals("03/16/1923", rdb.get(1, "peak_dt"));
        assertEquals(1980.0, rdb.getDouble(1, "peak_va", -1), 0.0);
    }

    @Test
    void thirdRow() {
        assertEquals("08/06/1924", rdb.get(2, "peak_dt"));
        assertEquals(3250.0, rdb.getDouble(2, "peak_va", -1), 0.0);
    }

    @Test
    void fromString_parsesCorrectly() throws Exception {
        String content = "# comment line\nname\tvalue\n5s\t8s\nAlice\t42\nBob\t99\n";
        DataTable table = RdbFile.fromString(content);
        assertEquals(2, table.getRowCount());
        assertEquals("Alice", table.get(0, "name"));
        assertEquals(99.0, table.getDouble(1, "value", -1), 0.0);
    }

    @Test
    void emptyTimeMeansBlank() {
        // peak_tm is empty for all rows in our test data
        assertEquals("", rdb.get(0, "peak_tm"));
    }
}
