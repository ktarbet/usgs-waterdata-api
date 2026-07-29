package org.opendcs.usgs.waterdata;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SiteTypesTest {

    /** Rows in the shape the live API returns, to prove the extra columns are ignored. */
    private static final String SITE_TYPES_CSV =
            "x,y,id,site_type_primary_flag,site_type_name,site_type_description\n" +
            ",,AT,Y,Atmosphere,A site established primarily to measure meteorological properties.\n" +
            ",,\"FA-FON\",N,\"Field, Pasture, Orchard, or Nursery\",\"A water-using agricultural facility.\"\n" +
            ",,ST,Y,Stream,\"A body of running water.\"\n";

    @Test
    void fromRow_keepsOnlyIdAndName() throws Exception {
        List<SiteTypes.SiteType> types =
                CsvFile.fromString(SITE_TYPES_CSV).mapRows(SiteTypes.SiteType::fromRow);

        assertEquals(3, types.size());
        assertEquals("AT", types.get(0).id);
        assertEquals("Atmosphere", types.get(0).name);
        // Quoted name containing commas must survive CSV parsing intact.
        assertEquals("FA-FON", types.get(1).id);
        assertEquals("Field, Pasture, Orchard, or Nursery", types.get(1).name);
        assertEquals("ST", types.get(2).id);
        assertEquals("Stream", types.get(2).name);
        assertEquals("Stream (ST)", types.get(2).toString());
    }

    @Test
    void bundledList_loadsWithoutNetwork() {
        List<SiteTypes.SiteType> types = SiteTypes.getSiteTypes();
        assertEquals(56, types.size(), "Expected the full bundled site-type list");

        assertEquals("Stream", SiteTypes.getName(SiteTypes.STREAM));
        assertEquals("Lake, Reservoir, Impoundment", SiteTypes.getName("LK"));
        assertEquals("Well", SiteTypes.getName("GW"));
        assertNull(SiteTypes.getName("ZZ"), "Unknown code should return null");

        for (SiteTypes.SiteType t : types) {
            assertFalse(t.id.isEmpty(), "Every site type should have a code");
            assertFalse(t.name.isEmpty(), "Every site type should have a name");
        }
    }

    @Test
    void bundledList_quotedNameSurvivesRoundTrip() {
        // "Field, Pasture, Orchard, or Nursery" is quoted in the resource because of its commas.
        assertEquals("Field, Pasture, Orchard, or Nursery", SiteTypes.getName("FA-FON"));
    }

    @Test
    void getNamesAndIds_areParallelAndSortedByName() {
        String[] names = SiteTypes.getNames();
        String[] ids = SiteTypes.getIds();
        assertEquals(names.length, ids.length, "Names and ids must line up for combo-box use");

        for (int i = 1; i < names.length; i++) {
            assertTrue(names[i - 1].compareTo(names[i]) <= 0,
                    "Expected names sorted alphabetically but " + names[i - 1] + " preceded " + names[i]);
        }

        int stream = List.of(ids).indexOf(SiteTypes.STREAM);
        assertTrue(stream >= 0, "Expected the stream code in the list");
        assertEquals("Stream", names[stream]);
    }

    @Test
    void streamConstant_matchesTheCodeDssvueUses() {
        assertEquals("ST", SiteTypes.STREAM);
    }
}
