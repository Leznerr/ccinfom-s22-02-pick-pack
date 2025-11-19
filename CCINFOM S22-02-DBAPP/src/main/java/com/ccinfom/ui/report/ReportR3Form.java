package com.ccinfom.ui.report;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.Branch;
import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportTableModel;
import com.ccinfom.report.r3.R3MonthlyThroughputRow;
import com.ccinfom.report.r3.ReportR3Dao;
import com.ccinfom.report.r4.ReportFilters;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.ui.common.SimpleBarChartPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Color;
import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.math.RoundingMode;

/**
 * R3 - Monthly Pack-to-Dispatch Performance by Branch.
 * Filters: Year/Month (required) and optional Branch dropdown.
 * Metrics: shipments packed, avg/median minutes from pack to dispatch, SLA% (vehicle SLA hours), exception count.
 */
public class ReportR3Form extends JFrame {

    private final ReportFilterPanel filterPanel = new ReportFilterPanel();
    private JComboBox<ComboItem<Long>> branchFilter;
    private JButton runButton;
    private JButton exportPdfButton;
    private JButton exportChartButton;
    private JButton closeButton;
    private JTable reportTable;
    private ReportTableModel tableModel;
    private SimpleBarChartPanel chartPanel;
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");
    private final ReportR3Dao reportDao = new ReportR3Dao();

    private static final class ComboItem<T> {
        private final T value;
        private final String label;

        ComboItem(T value, String label) {
            this.value = value;
            this.label = label;
        }

        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public ReportR3Form() {
        super("R3 - Monthly Pack-to-Dispatch Performance");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        initFilters();
        initTable();
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterTabs(), BorderLayout.CENTER);
        add(buildSouthPanel(), BorderLayout.SOUTH);

        setSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void showWindow() {
        SwingUtilities.invokeLater(ReportR3Form::new);
    }

    private void initFilters() {
        filterPanel.setSupportedModes(EnumSet.of(ReportFilterPanel.PeriodMode.MONTH));
        branchFilter = new JComboBox<>();
        branchFilter.addItem(new ComboItem<>(null, "(All branches)"));
        try {
            LookupDao lookupDao = new LookupDaoImpl();
            List<Branch> branches = lookupDao.listBranches();
            for (Branch b : branches) {
                branchFilter.addItem(new ComboItem<>(b.getBranchId(), b.getBranchName()));
            }
        } catch (Exception e) {
            // keep default "(All branches)" if lookup fails
        }
        filterPanel.addFilterField("Branch", branchFilter);
    }

    private void initTable() {
        tableModel = new ReportTableModel();
        tableModel.setColumns(
                "Month",
                "Branch",
                "Shipments",
                "Avg Minutes",
                "Median Minutes",
                "On-Time %",
                "Exceptions"
        );
        reportTable = new JTable(tableModel);
        reportTable.setFillsViewportHeight(true);
        reportTable.setAutoCreateRowSorter(true);
        reportTable.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        chartPanel = new SimpleBarChartPanel();
    }

    private JTabbedPane buildCenterTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table", new JScrollPane(reportTable));
        tabs.addTab("Chart", chartPanel);
        return tabs;
    }

    private JPanel buildTopPanel() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        wrapper.add(filterPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildSouthPanel() {
        runButton = new JButton("Run Report");
        exportPdfButton = new JButton("Export PDF");
        exportChartButton = new JButton("Save Chart PNG");
        closeButton = new JButton("Close");

        runButton.addActionListener(e -> loadReportData());
        exportPdfButton.addActionListener(e -> exportPdf());
        exportChartButton.addActionListener(e -> exportChart());
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        buttonPanel.add(runButton);
        buttonPanel.add(exportPdfButton);
        buttonPanel.add(exportChartButton);
        buttonPanel.add(closeButton);

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        south.add(statusPanel, BorderLayout.CENTER);
        south.add(buttonPanel, BorderLayout.EAST);
        return south;
    }

    private void loadReportData() {
        ReportFilters filters = new ReportFilters();
        filters.setYear(filterPanel.getSelectedYear());
        filters.setMonth(filterPanel.getSelectedMonth());

        ComboItem<Long> branch = (ComboItem<Long>) branchFilter.getSelectedItem();
        if (branch != null) {
            filters.setBranchId(branch.getValue());
        }

        if (filters.getYear() == null || filters.getMonth() == null) {
            statusPanel.setError("Year and month are required.");
            return;
        }

        tableModel.setRowCount(0);
        statusPanel.setInfo("Running report...");

        try {
            List<R3MonthlyThroughputRow> rows = reportDao.findMonthlyThroughput(filters);
            if (rows.isEmpty()) {
                statusPanel.setInfo("No data for selected filters.");
                return;
            }

            for (R3MonthlyThroughputRow row : rows) {
                tableModel.addRow(new Object[]{
                        row.getYearMonth(),
                        row.getBranchName(),
                        row.getShipmentsPacked(),
                        formatMinutes(row.getAvgMinutesToDispatch()),
                        formatMinutes(row.getMedianMinutesToDispatch()),
                        formatPercent(row.getSlaWithinPct()),
                        row.getExceptionsCount()
                });
            }
            autoResizeColumns();
            populateChart(rows);
            statusPanel.setSuccess(String.format("Loaded %d row(s).", rows.size()));
        } catch (SQLException ex) {
            statusPanel.setError("Error loading report: " + ex.getMessage());
        }
    }

    private void autoResizeColumns() {
        TableColumnModel cols = reportTable.getColumnModel();
        for (int i = 0; i < cols.getColumnCount(); i++) {
            int pref = 140;
            if (i == 1) pref = 180;
            cols.getColumn(i).setPreferredWidth(pref);
        }
    }

    private String formatMinutes(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null) {
            return "N/A";
        }
        return value.setScale(2, RoundingMode.HALF_UP) + "%";
    }

    private void exportPdf() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Run the report before exporting.", "Export", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Table to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File("R3_Pack_to_Dispatch.pdf"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensurePdfExtension(chooser.getSelectedFile());
            try {
                PdfExporter.exportTable(reportTable, file, "R3 - Monthly Pack-to-Dispatch Performance");
                statusPanel.setSuccess("Exported table PDF to " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Failed to export PDF: " + ex.getMessage());
            }
        }
    }

    private void exportChart() {
        if (chartPanel == null || !chartPanel.hasData()) {
            JOptionPane.showMessageDialog(this, "Run the report before exporting the chart.", "Export Chart", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Chart as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        chooser.setSelectedFile(new File("R3_Pack_to_Dispatch.png"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = ensurePngExtension(chooser.getSelectedFile());
            try {
                int w = Math.max(chartPanel.getPreferredSize().width, 900);
                int h = Math.max(chartPanel.getPreferredSize().height, 500);
                chartPanel.saveAsPng(file, w, h);
                statusPanel.setSuccess("Chart saved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Failed to save chart: " + ex.getMessage());
            }
        }
    }

    private void populateChart(List<R3MonthlyThroughputRow> rows) {
        if (chartPanel == null) {
            return;
        }
        List<String> categories = new java.util.ArrayList<>();
        List<Double> values = new java.util.ArrayList<>();
        for (R3MonthlyThroughputRow row : rows) {
            categories.add(row.getBranchName() != null ? row.getBranchName() : "");
            values.add((double) row.getShipmentsPacked());
        }
        SimpleBarChartPanel.Series series = new SimpleBarChartPanel.Series("Shipments", values, Color.decode("#2d9cdb"));
        chartPanel.setData(categories, java.util.Collections.singletonList(series), "Shipments per Branch", "Shipments");
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
