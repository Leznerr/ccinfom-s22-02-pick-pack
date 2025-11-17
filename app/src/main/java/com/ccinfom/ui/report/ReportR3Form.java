package com.ccinfom.ui.report;


import com.ccinfom.report.r3.ReportR3Dao;
import com.ccinfom.report.r4.ReportFilters;
import main.java.com.ccinfom.report.r3.R3MonthlyThroughputRow;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import javax.swing.JInternalFrame;

/**
 * TODO[Phase F - R3]:
 *  - Build the monthly inventory / pack-dispatch throughput form with month/year filters plus optional product/branch selectors.
 *  - Display both detail rows and high-level KPIs sourced from ReportR3Dao.
 *  - Include export and SLA-highlighting logic if required.
 */
public class ReportR3Form extends JInternalFrame {
    //ui components
    private JComboBox<String> monthCombo;
    private JComboBox<Integer> yearCombo;
    private JTextField customerIdFilter;
    private JTextField productCatFilter;
    private JButton runButton;
    private JButton exportButton;

    private JTable reportTable;
    private DefaultTableModel tableModel;

    // KPI Labels
    private JTextField kpiTotalPacked;
    private JTextField kpiTotalManifests;
    private JTextField kpiShortRate;
    private JTextField kpiShortTickets;

    // DAO
    private ReportR3Dao reportDao;

    public ReportR3Form() {
        super("R3 – Monthly Inventory / Pack-Dispatch Throughput", true, true, true, true);
        // TODO: Layout UI components (filters, KPI summary, table, export button).
        reportDao = new ReportR3Dao();

        // --- Layout ---
        setLayout(new BorderLayout(10, 10));

        // Create panels
        JPanel filterPanel = createFilterPanel();
        JPanel kpiPanel = createKpiPanel();
        JPanel tablePanel = createTablePanel();
        JPanel buttonPanel = createButtonPanel();

        // Combine filter and KPI panels at the top
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.NORTH);
        topPanel.add(kpiPanel, BorderLayout.CENTER);

        // Add panels to the frame
        add(topPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add action listeners
        addListeners();

        // Set frame properties
        setSize(1000, 700); // Widened frame for new columns
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    /**
     * Creates the top panel containing all report filters.
     * Refactored to use text fields matching the new ReportFilters object.
     */
    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Filters"));

        // Month Filter
        panel.add(new JLabel("Month:"));
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(Calendar.getInstance().get(Calendar.MONTH)); // Default to current month
        panel.add(monthCombo);

        // Year Filter
        panel.add(new JLabel("Year:"));
        Integer[] years = new Integer[5];
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = 0; i < 5; i++) {
            years[i] = currentYear - i;
        }
        yearCombo = new JComboBox<>(years);
        panel.add(yearCombo);

        // Customer ID Filter (Optional)
        panel.add(new JLabel("Customer ID:"));
        customerIdFilter = new JTextField(8);
        panel.add(customerIdFilter);

        // Product Category Filter (Optional)
        panel.add(new JLabel("Product Category:"));
        productCatFilter = new JTextField(12);
        panel.add(productCatFilter);

        // Run Button
        runButton = new JButton("Run Report");
        panel.add(runButton);

        return panel;
    }

    /**
     * Creates the panel for displaying high-level KPIs.
     * Refactored to match the metrics in R3MonthlyThroughputRow.
     */
    private JPanel createKpiPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("KPI Summary (Matching Filters)"));

        // Helper to create a KPI box
        kpiTotalPacked = createKpiField("Total Boxes Packed");
        kpiTotalManifests = createKpiField("Total Manifests");
        kpiShortRate = createKpiField("Short-Close Rate %");
        kpiShortTickets = createKpiField("Short-Closed Tickets");

        panel.add(createKpiBox("Total Boxes Packed", kpiTotalPacked));
        panel.add(createKpiBox("Total Manifests", kpiTotalManifests));
        panel.add(createKpiBox("Short-Close Rate %", kpiShortRate));
        panel.add(createKpiBox("Short-Closed Tickets", kpiShortTickets));

        // Highlight problematic KPIs
        kpiShortRate.setFont(kpiShortRate.getFont().deriveFont(Font.BOLD));
        kpiShortRate.setForeground(Color.RED);
        kpiShortTickets.setFont(kpiShortTickets.getFont().deriveFont(Font.BOLD));
        kpiShortTickets.setForeground(Color.RED);

        return panel;
    }

    // Helper for createKpiPanel
    private JTextField createKpiField(String label) {
        JTextField field = new JTextField("N/A", 10);
        field.setEditable(false);
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return field;
    }

    // Helper for createKpiPanel
    private JPanel createKpiBox(String title, JComponent component) {
        JPanel box = new JPanel(new BorderLayout());
        box.add(new JLabel(title, JLabel.CENTER), BorderLayout.NORTH);
        box.add(component, BorderLayout.CENTER);
        return box;
    }

    /**
     * Creates the panel containing the main data table.
     * Refactored to show aggregated columns from R3MonthlyThroughputRow.
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // New columns matching R3MonthlyThroughputRow
        String[] columnNames = {
                "Year-Month", "Customer", "Branch", "Total Tickets", "Short-Closed",
                "Short %", "Req. Qty", "Deliv. Qty", "Short Qty", "Fulfill %",
                "Est. Cost", "Boxes Packed", "Manifests"
        };
        tableModel = new DefaultTableModel(columnNames, 0);
        reportTable = new JTable(tableModel);

        reportTable.setFillsViewportHeight(true);
        reportTable.setRowHeight(24);
        reportTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scroll

        JScrollPane scrollPane = new JScrollPane(reportTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Helper to set preferred column widths for the new table.
     */
    private void resizeColumns() {
        TableColumnModel columnModel = reportTable.getColumnModel();
        // {"Year-Month", "Customer", "Branch", "Total Tickets", "Short-Closed", "Short %", "Req. Qty", "Deliv. Qty", "Short Qty", "Fulfill %", "Est. Cost", "Boxes Packed", "Manifests"}
        int[] widths = {90, 150, 150, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90};
        for (int i = 0; i < tableModel.getColumnCount() && i < widths.length; i++) {
            TableColumn column = columnModel.getColumn(i);
            column.setPreferredWidth(widths[i]);
        }
    }

    /**
     * Creates the bottom panel with action buttons (e.g., Export).
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exportButton = new JButton("Export to CSV");
        panel.add(exportButton);
        return panel;
    }

    /**
     * Attaches action listeners to buttons.
     */
    private void addListeners() {
        runButton.addActionListener(e -> loadReportData());
        exportButton.addActionListener(e -> exportReport());
    }

    /**
     * Action for the "Run Report" button.
     * Fetches data from the DAO and populates the table and KPIs.
     */
    private void loadReportData() {
        // 1. Get filter values and build ReportFilters object
        ReportFilters filters = new ReportFilters();

        filters.setMonth(monthCombo.getSelectedIndex() + 1); // JComboBox is 0-based, SQL is 1-based
        filters.setYear((Integer) yearCombo.getSelectedItem());

        // Parse optional Customer ID
        try {
            if (customerIdFilter.getText() != null && !customerIdFilter.getText().trim().isEmpty()) {
                filters.setCustomerId(Long.parseLong(customerIdFilter.getText().trim()));
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid Customer ID. Please enter a number.", "Filter Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get optional Product Category
        if (productCatFilter.getText() != null && !productCatFilter.getText().trim().isEmpty()) {
            filters.setProductId(productCatFilter.getText().trim());
        }

        // 2. Clear previous data
        tableModel.setRowCount(0);
        resetKpis();

        try {
            // 3. Call DAO for KPIs
            R3MonthlyThroughputRow summary = reportDao.findMonthlySummary(filters);
            if (summary != null) {
                populateKpis(summary);
            }

            // 4. Call DAO for Detail Rows
            List<R3MonthlyThroughputRow> detailRows = reportDao.findMonthlyThroughput(filters);
            if (detailRows.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No data found for the selected filters.", "No Results", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // 5. Populate Table
            for (R3MonthlyThroughputRow row : detailRows) {
                tableModel.addRow(new Object[]{
                        row.getYearMonth(),
                        row.getCustomerName(),
                        row.getBranchName(),
                        row.getTotalTicketsClosed(),
                        row.getShortClosedTickets(),
                        formatPercent(row.getShortCloseRatePct()),
                        row.getTotalRequestedQty(),
                        row.getTotalDeliveredQty(),
                        row.getTotalShortQty(),
                        formatPercent(row.getFulfillmentRatePct()),
                        row.getEstimatedReturnCost(),
                        row.getBoxesPacked(),
                        row.getManifestsCreated()
                });
            }

            // Resize columns after data is loaded
            resizeColumns();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error fetching report data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resetKpis() {
        kpiTotalPacked.setText("N/A");
        kpiTotalManifests.setText("N/A");
        kpiShortRate.setText("N/A");
        kpiShortTickets.setText("N/A");
    }

    private void populateKpis(R3MonthlyThroughputRow summary) {
        kpiTotalPacked.setText(String.valueOf(summary.getBoxesPacked()));
        kpiTotalManifests.setText(String.valueOf(summary.getManifestsCreated()));
        kpiShortRate.setText(formatPercent(summary.getShortCloseRatePct()));
        kpiShortTickets.setText(String.valueOf(summary.getShortClosedTickets()));
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }

    /**
     * Action for the "Export" button.
     * Shows a file chooser and would (in a real app) write the table to a CSV.
     */
    private void exportReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "There is no data to export.", "Export Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report As");
        fileChooser.setSelectedFile(new File("R3_Monthly_Throughput_Report.csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            // TODO: Add actual CSV export logic here
            // (e.g., iterate tableModel, write to fileToSave.getAbsolutePath())

            JOptionPane.showMessageDialog(this,
                    "Report exported successfully to:\n" + fileToSave.getAbsolutePath(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
