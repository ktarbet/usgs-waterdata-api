package ktarbet.usgs.waterdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads USGS RDB (tab-delimited) files into a {@link DataTable}.
 *
 * <p>RDB format rules:
 * <ul>
 *   <li>Lines starting with {@code #} are comments and are skipped.</li>
 *   <li>The first non-comment line is the column header row (tab-separated).</li>
 *   <li>The second non-comment line describes column widths/types (e.g. {@code 10d}, {@code 8s}) and is skipped.</li>
 *   <li>Remaining lines are tab-separated data rows.</li>
 * </ul>
 */
class RdbFile extends DataTable {

    public RdbFile(String filename) throws IOException {
        this(Path.of(filename));
    }

    public RdbFile(Path path) throws IOException {
        this(Files.newBufferedReader(path));
    }

    public RdbFile(BufferedReader reader) throws IOException {
        super(parse(reader));
    }

    public static RdbFile fromString(String rdbContent) throws IOException {
        return new RdbFile(new BufferedReader(new StringReader(rdbContent)));
    }

    private static ParseResult parse(BufferedReader reader) throws IOException {
        try (reader) {
            String line;

            // Skip comment lines starting with #
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    break;
                }
            }

            if (line == null) {
                return new ParseResult(new String[0], new ArrayList<>());
            }

            // First non-comment line is the column headers
            String[] columnNames = line.split("\t", -1);

            // Second non-comment line is the type/width descriptor — skip it
            reader.readLine();

            // Remaining lines are data
            List<String[]> rows = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                rows.add(line.split("\t", -1));
            }
            return new ParseResult(columnNames, rows);
        }
    }
}
