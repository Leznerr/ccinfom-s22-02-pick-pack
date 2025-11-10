package com.ccinfom.report;

import javax.swing.table.DefaultTableModel;

/**
 * TODO[Phase F]:
 *  - Implement a table model (or utility) that can accept arbitrary report column definitions.
 *  - Support CSV export by exposing the current data set.
 *  - Use this model in all ReportR*Form classes for consistent UX.
 */
public class ReportTableModel extends DefaultTableModel {

    public ReportTableModel() {
        super();
        // TODO: Initialize with sensible defaults (column headers, non-editable cells, etc.).
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    // TODO: Add helper methods for loading DTO lists and exporting to CSV.
}
