package ktarbet.usgs.waterdata;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

class DataTable {

    private final String[] columnNames;
    private final Map<String, Integer> columnIndex;
    private final List<String[]> rows;

    protected static class ParseResult {
        final String[] columnNames;
        final List<String[]> rows;

        public ParseResult(String[] columnNames, List<String[]> rows) {
            this.columnNames = columnNames;
            this.rows = rows;
        }
    }

    protected DataTable(ParseResult result) {
        this(result.columnNames, result.rows);
    }

    protected DataTable(String[] columnNames, List<String[]> rows) {
        this.columnNames = columnNames;
        this.rows = new ArrayList<>(rows);
        this.columnIndex = new LinkedHashMap<>();
        for (int i = 0; i < columnNames.length; i++) {
            columnIndex.put(columnNames[i], i);
        }
    }

    public String get(int row, String column) {
        Integer col = columnIndex.get(column);
        if (col == null) {
            return "";
        }
        String[] r = rows.get(row);
        return col < r.length ? r[col] : "";
    }

    public boolean hasColumn(String column) {
        return columnIndex.containsKey(column);
    }

    public double getDouble(int row, String column, double defaultValue) {
        String value = get(row, column);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String[] getRow(int row) {
        return rows.get(row);
    }

    public int findRow(String column, String value) {
        Integer col = columnIndex.get(column);
        if (col == null) {
            throw new IllegalArgumentException("Unknown column: " + column);
        }
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            if (col < r.length && value.equals(r[col])) {
                return i;
            }
        }
        return -1;
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public int getRowCount() {
        return rows.size();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public <T> List<T> mapRows(BiFunction<DataTable, Integer, T> mapper) {
        List<T> result = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            result.add(mapper.apply(this, i));
        }
        return result;
    }
}
