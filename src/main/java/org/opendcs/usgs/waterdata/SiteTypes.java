package org.opendcs.usgs.waterdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Site-type codes used to classify monitoring locations, for presenting users a choice of site type.
 *
 * <p>Only the {@code id} and {@code site_type_name} columns are kept. The list is read from a
 * bundled copy of the USGS {@code site-types} collection, so building a picker costs no network
 * request and cannot fail; this vocabulary is effectively static.
 *
 * <p>To regenerate {@code src/main/resources/site-types.csv}, fetch
 * {@code https://api.waterdata.usgs.gov/ogcapi/v0/collections/site-types/items?f=csv&limit=1000}
 * and keep only those two columns.
 *
 * <pre>{@code
 * // populate a combo box
 * comboBox.setModel(new DefaultComboBoxModel<>(SiteTypes.getNames()));
 * String siteTypeCode = SiteTypes.getIds()[comboBox.getSelectedIndex()];
 * var locations = UsgsWaterDataApi.getLocations(stateCode, siteTypeCode);
 * }</pre>
 */
public final class SiteTypes {

    /** Site-type code for streams, the most commonly requested type. */
    public static final String STREAM = "ST";

    private static final String RESOURCE = "/site-types.csv";

    private static volatile List<SiteType> cache;

    private SiteTypes() {
        // static utility class, prevent instantiation
    }

    /** A single site type: its code and display name. */
    public static class SiteType {
        public final String id;
        public final String name;

        SiteType(String id, String name) {
            this.id = id;
            this.name = name;
        }

        static SiteType fromRow(DataTable table, int row) {
            return new SiteType(table.get(row, "id"), table.get(row, "site_type_name"));
        }

        @Override
        public String toString() {
            return name + " (" + id + ")";
        }
    }

    /**
     * Returns all site types sorted by name.
     */
    public static List<SiteType> getSiteTypes() {
        List<SiteType> types = cache;
        if (types == null) {
            synchronized (SiteTypes.class) {
                if (cache == null) {
                    cache = sortedByName(loadBundled());
                }
                types = cache;
            }
        }
        return types;
    }

    private static List<SiteType> loadBundled() {
        try (InputStream is = SiteTypes.class.getResourceAsStream(RESOURCE)) {
            if (is == null) {
                throw new IllegalStateException("Missing bundled resource " + RESOURCE);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            return new CsvFile(reader).mapRows(SiteType::fromRow);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + RESOURCE, e);
        }
    }

    private static List<SiteType> sortedByName(List<SiteType> types) {
        List<SiteType> sorted = new ArrayList<>(types);
        sorted.sort(Comparator.comparing(t -> t.name));
        return List.copyOf(sorted);
    }

    /**
     * Returns site type names sorted alphabetically (e.g., "Aggregate groundwater use", ..., "Stream", ...).
     */
    public static String[] getNames() {
        return getSiteTypes().stream().map(t -> t.name).toArray(String[]::new);
    }

    /**
     * Returns site type codes in the same order as {@link #getNames()} (e.g., "AG", ..., "ST", ...).
     */
    public static String[] getIds() {
        return getSiteTypes().stream().map(t -> t.id).toArray(String[]::new);
    }

    /**
     * Returns the display name for a site type code, or null if the code is unknown.
     */
    public static String getName(String id) {
        return getSiteTypes().stream()
                .filter(t -> t.id.equals(id))
                .map(t -> t.name)
                .findFirst().orElse(null);
    }
}
