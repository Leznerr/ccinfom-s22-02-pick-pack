package com.ccinfom.report;

import javax.swing.table.DefaultTableModel;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simple DefaultTableModel extension for reports.
 * Features:
 *  - Non-editable cells.
 *  - Helper methods for setting columns, loading row arrays, and exporting to CSV.
 */
public class ReportTableModel extends DefaultTableModel {

    public ReportTableModel() {
        super();
    }

    public void setColumns(String... columns) {
        setColumnIdentifiers(columns);
        setRowCount(0);
    }

    public void loadRows(List<? extends Object[]> rows) {
        setRowCount(0);
        if (rows == null) {
            return;
        }
        for (Object[] row : rows) {
            addRow(row);
        }
    }

    public void appendRow(Object... values) {
        addRow(values);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Object[] getRowValues(int rowIndex) {
        int columnCount = getColumnCount();
        Object[] values = new Object[columnCount];
        for (int col = 0; col < columnCount; col++) {
            values[col] = getValueAt(rowIndex, col);
        }
        return values;
    }

    public void exportToCsv(Path outputPath) throws IOException {
        Objects.requireNonNull(outputPath, "outputPath");
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(buildCsvLine(IntStream.range(0, getColumnCount())
                    .mapToObj(this::getColumnName)
                    .collect(Collectors.toList())));
            writer.newLine();

            for (int row = 0; row < getRowCount(); row++) {
                writer.write(buildCsvLine(Arrays.asList(getRowValues(row))));
                writer.newLine();
            }
        }
    }

    private String buildCsvLine(List<?> values) {
        return values.stream()
                .map(this::escapeCsv)
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        boolean needsQuote = text.contains(",") || text.contains("\"") || text.contains("\n");
        if (needsQuote) {
            text = "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
