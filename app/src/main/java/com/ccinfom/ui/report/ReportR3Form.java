package com.ccinfom.ui.report;


import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportTableModel;
import com.ccinfom.report.r3.ReportR3Dao;
import com.ccinfom.report.r3.R3MonthlyThroughputRow;
import com.ccinfom.report.r4.ReportFilters;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.ui.common.SimpleBarChartPanel;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.Customer;
import com.ccinfom.model.Branch;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

/**
 * TODO[Phase F - R3]:
 *  - Build the monthly inventory / pack-dispatch throughput form with month/year filters plus optional product/branch selectors.
 *  - Display both detail rows and high-level KPIs sourced from ReportR3Dao.
 *  - Include export and SLA-highlighting logic if required.
 */
public class ReportR3Form extends JFrame {
    // ui components
    private final ReportFilterPanel filterPanel = new ReportFilterPanel();
    private JComboBox<ComboItem<Long>> customerFilter;
    private JComboBox<ComboItem<Long>> branchFilter;
    private JComboBox<String> productCategoryFilter;
    private JButton exportTablePdfButton;
    private JButton exportChartPngButton;
    private JButton runButton;
    private JTable reportTable;
    private ReportTableModel tableModel;
    private SimpleBarChartPanel chartPanel;

    // KPI Labels
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    // DAO
    private final ReportR3Dao reportDao = new ReportR3Dao();
    private static final class ComboItem<T> {
        private final T value;
        private final String label;
        ComboItem(T value, String label) {
            this.value = value;
            this.label = label;
        }
        public T getValue() { return value; }
        @Override public String toString() { return label; }
    }

    public ReportR3Form() {
        super("R3 – Monthly Inventory / Pack-Dispatch Throughput");

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel filters = createFilterPanel();
        JPanel tablePanel = createTablePanel();
        JPanel buttonPanel = createButtonPanel();

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        filterWrapper.add(filters, BorderLayout.CENTER);
        topPanel.add(filterWrapper, BorderLayout.NORTH);

        add(topPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        addListeners();

        setSize(1100, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void showWindow() {
        SwingUtilities.invokeLater(ReportR3Form::new);
    }

    /**
     * Creates the top panel containing all report filters.
     * Refactored to use text fields matching the new ReportFilters object.
     */
    private JPanel createFilterPanel() {
        // Configure filterPanel to only show Month mode
        filterPanel.setSupportedModes(EnumSet.of(ReportFilterPanel.PeriodMode.MONTH));

        // Dropdown filters
        LookupDao lookupDao = new LookupDaoImpl();
        customerFilter = new JComboBox<>();
        branchFilter = new JComboBox<>();
        productCategoryFilter = new JComboBox<>();

        customerFilter.addItem(new ComboItem<>(null, "(All)"));
        branchFilter.addItem(new ComboItem<>(null, "(All)"));
        productCategoryFilter.addItem("(All)");

        try {
            List<Customer> customers = lookupDao.listCustomers();
            for (Customer c : customers) {
                customerFilter.addItem(new ComboItem<>(c.getCustomerId(), c.getCustomerName()));
            }
            List<Branch> branches = lookupDao.listBranches();
            for (Branch b : branches) {
                branchFilter.addItem(new ComboItem<>(b.getBranchId(), b.getBranchName()));
            }
        } catch (Exception ignored) {
            // If lookup fails, keep only (All)
        }

        // static categories from seed; could be dynamic
        productCategoryFilter.addItem("Electronics");
        productCategoryFilter.addItem("Hardware");
        productCategoryFilter.addItem("Grocery");

        filterPanel.addFilterField("Customer", customerFilter);
        filterPanel.addFilterField("Branch", branchFilter);
        filterPanel.addFilterField("Product Category", productCategoryFilter);
        return filterPanel;
    }

    /**
     * Creates the panel containing the main data table.
     * Refactored to show aggregated columns from R3MonthlyThroughputRow.
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // New columns matching R3MonthlyThroughputRow
        tableModel = new ReportTableModel();
        tableModel.setColumns(
                "Year-Month", "Customer", "Branch", "Total Tickets", "Short-Closed",
                "Short %", "Req. Qty", "Deliv. Qty", "Short Qty", "Fulfill %",
                "Est. Cost", "Boxes Packed", "Manifests"
        );
        reportTable = new JTable(tableModel);

        reportTable.setFillsViewportHeight(true);
        reportTable.setRowHeight(24);
        reportTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Allow horizontal scroll
        reportTable.setAutoCreateRowSorter(true);

        chartPanel = new SimpleBarChartPanel();

        JScrollPane scrollPane = new JScrollPane(reportTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table", scrollPane);
        tabs.addTab("Chart", chartPanel);

        panel.add(tabs, BorderLayout.CENTER);
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(statusPanel, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        runButton = new JButton("Run");
        exportTablePdfButton = new JButton("Export Table PDF");
        exportChartPngButton = new JButton("Save Chart PNG");
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        buttons.add(runButton);
        buttons.add(exportTablePdfButton);
        buttons.add(exportChartPngButton);
        buttons.add(closeBtn);
        panel.add(buttons, BorderLayout.EAST);
        return panel;
    }

    /**
     * Attaches action listeners to buttons.
     */
    private void addListeners() {
        runButton.addActionListener(e -> loadReportData());
        exportTablePdfButton.addActionListener(e -> exportReportPdf());
        exportChartPngButton.addActionListener(e -> exportChartPng());
    }

    /**
     * Action for the "Run Report" button.
     * Fetches data from the DAO and populates the table and KPIs.
     */
    private void loadReportData() {
        // 1. Get filter values and build ReportFilters object
        ReportFilters filters = new ReportFilters();

        filters.setYear(filterPanel.getSelectedYear());
        filters.setMonth(filterPanel.getSelectedMonth());

        ComboItem<Long> cust = (ComboItem<Long>) customerFilter.getSelectedItem();
        if (cust != null) {
            filters.setCustomerId(cust.getValue());
        }

        ComboItem<Long> br = (ComboItem<Long>) branchFilter.getSelectedItem();
        if (br != null) {
            filters.setBranchId(br.getValue());
        }

        String cat = (String) productCategoryFilter.getSelectedItem();
        if (cat != null && !"(All)".equals(cat)) {
            filters.setProductCategory(cat);
        }

        // 2. Clear previous data
        tableModel.setRowCount(0);
        statusPanel.setInfo("Running report...");

        try {
            // 3. Call DAO for Detail Rows
            List<R3MonthlyThroughputRow> detailRows = reportDao.findMonthlyThroughput(filters);
            if (detailRows.isEmpty()) {
                statusPanel.setInfo("No data for selected filters.");
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
            populateChart(detailRows);
            statusPanel.setSuccess(String.format("Loaded %d row(s).", detailRows.size()));

        } catch (SQLException e) {
            statusPanel.setError("Error fetching report data: " + e.getMessage());
        }
    }

    private void populateChart(List<R3MonthlyThroughputRow> rows) {
        if (chartPanel == null || rows == null) {
            return;
        }
        List<String> categories = new java.util.ArrayList<>();
        List<Double> values = new java.util.ArrayList<>();
        for (R3MonthlyThroughputRow row : rows) {
            categories.add(row.getYearMonth() != null ? row.getYearMonth() : "");
            values.add((double) row.getBoxesPacked());
        }
        SimpleBarChartPanel.Series series = new SimpleBarChartPanel.Series("Boxes Packed", values, Color.decode("#2d9cdb"));
        chartPanel.setData(categories, java.util.Collections.singletonList(series), "Boxes Packed per Month", "Boxes");
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return value.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }

    private void exportReportPdf() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "There is no data to export.", "Export Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Table to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File("R3_Monthly_Throughput_Table.pdf"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensurePdfExtension(chooser.getSelectedFile());
            try {
                PdfExporter.exportTable(reportTable, file, "R3 – Monthly Inventory / Pack-Dispatch Throughput");
                statusPanel.setSuccess("Exported table PDF to " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Failed to export PDF: " + ex.getMessage());
            }
        }
    }

    private void exportChartPng() {
        if (chartPanel == null || !chartPanel.hasData()) {
            JOptionPane.showMessageDialog(this, "Run the report before exporting the chart.", "Export Chart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Chart as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        chooser.setSelectedFile(new File("R3_Monthly_Throughput_Chart.png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensurePngExtension(chooser.getSelectedFile());
            try {
                int w = Math.max(chartPanel.getPreferredSize().width, 900);
                int h = Math.max(chartPanel.getPreferredSize().height, 500);
                // Leverage the shared helper to render offscreen at a consistent size.
                chartPanel.saveAsPng(file, w, h);
                statusPanel.setSuccess("Chart saved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Failed to save chart: " + ex.getMessage());
            }
        }
    }

    private File ensurePdfExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            return new File(file.getParentFile(), file.getName() + ".pdf");
        }
        return file;
    }

    private File ensurePngExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".png")) {
            return new File(file.getParentFile(), file.getName() + ".png");
        }
        return file;
    }
}
