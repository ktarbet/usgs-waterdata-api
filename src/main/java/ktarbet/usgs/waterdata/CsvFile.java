package ktarbet.usgs.waterdata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class CsvFile extends DataTable {

    public CsvFile(String filename) throws IOException {
        this(Path.of(filename));
    }

    public CsvFile(Path path) throws IOException {
        this(Files.newBufferedReader(path));
    }

    public CsvFile(BufferedReader reader) throws IOException {
        super(parse(reader));
    }

    public static CsvFile fromString(String csvContent) throws IOException {
        return new CsvFile(new BufferedReader(new StringReader(csvContent)));
    }

    private static ParseResult parse(BufferedReader reader) throws IOException {
        try (reader) {
            String line = readLogicalLine(reader);
            if (line == null) {
                return new ParseResult(new String[0], new ArrayList<>());
            }
            String[] columnNames = parseLine(line);
            List<String[]> rows = new ArrayList<>();
            while ((line = readLogicalLine(reader)) != null) {
                rows.add(parseLine(line));
            }
            return new ParseResult(columnNames, rows);
        }
    }

    /**
     * Reads a complete logical CSV line, joining continuation lines when a quoted
     * field contains embedded newlines.
     */
    private static String readLogicalLine(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) return null;
        while (hasUnclosedQuote(line)) {
            String next = reader.readLine();
            if (next == null) break;
            line = line + "\n" + next;
        }
        return line;
    }

    private static boolean hasUnclosedQuote(String line) {
        return line.chars().filter(c -> c == '"').count() % 2 != 0;
    }

    private static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        int i = 0;
        int len = line.length();

        while (i <= len) {
            if (i == len) {
                // trailing comma produces empty final field
                fields.add("");
                break;
            }
            if (line.charAt(i) == '"') {
                // quoted field
                StringBuilder sb = new StringBuilder();
                i++; // skip opening quote
                while (i < len) {
                    char c = line.charAt(i);
                    if (c == '"') {
                        if (i + 1 < len && line.charAt(i + 1) == '"') {
                            sb.append('"');
                            i += 2;
                        } else {
                            i++; // skip closing quote
                            break;
                        }
                    } else {
                        sb.append(c);
                        i++;
                    }
                }
                fields.add(sb.toString());
                // skip comma after closing quote
                if (i < len && line.charAt(i) == ',') {
                    i++;
                }
            } else {
                // unquoted field
                int comma = line.indexOf(',', i);
                if (comma == -1) {
                    fields.add(line.substring(i));
                    break;
                } else {
                    fields.add(line.substring(i, comma));
                    i = comma + 1;
                }
            }
        }
        return fields.toArray(new String[0]);
    }
}
