package com.ccinfom.ui.report;

import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportTableModel;
import com.ccinfom.report.r3.R3MonthlyThroughputRow;
import com.ccinfom.report.r3.ReportR3Dao;
import com.ccinfom.report.r4.ReportFilters;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.ui.common.SimpleBarChartPanel;
import com.ccinfom.ui.common.StatusPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

public class ReportR3Form extends JFrame {

    private final ReportFilterPanel filterPanel = new ReportFilterPanel();
    private JTextField customerIdFilter;
    private JTextField branchIdFilter;
    private JTextField productCategoryFilter;
    private JButton exportCsvButton;
    private JButton exportTablePdfButton;
    private JButton exportChartPdfButton;
    private JButton runButton;
    private JTable reportTable;
    private ReportTableModel tableModel;
    private SimpleBarChartPanel chartPanel;
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    private final ReportR3Dao dao = new ReportR3Dao();

    public static void showWindow() {
        SwingUtilities.invokeLater(ReportR3Form::new);
    }

    public ReportR3Form() {
        super("R3 – Monthly Inventory / Pack-Dispatch Throughput");
        setLayout(new BorderLayout(8, 8));
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel filters = createFilterPanel();
        JPanel tablePanel = createTablePanel();
        JPanel buttons = createButtonPanel();

        JPanel top = new JPanel(new BorderLayout());
        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        filterWrapper.add(filters, BorderLayout.CENTER);
        top.add(filterWrapper, BorderLayout.NORTH);

        add(top, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        addListeners();

        setSize(1080, 720);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createFilterPanel() {
        filterPanel.setSupportedModes(EnumSet.of(ReportFilterPanel.PeriodMode.MONTH));
        customerIdFilter = new JTextField(10);
        branchIdFilter = new JTextField(10);
        productCategoryFilter = new JTextField(10);
        filterPanel.addFilterField("Customer ID", customerIdFilter);
        filterPanel.addFilterField("Branch ID", branchIdFilter);
        filterPanel.addFilterField("Product Category", productCategoryFilter);
        return filterPanel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        tableModel = new ReportTableModel();
        tableModel.setColumns(
                "Year-Month", "Customer", "Branch", "Total Tickets", "Short-Closed",
                "Short %", "Req. Qty", "Deliv. Qty", "Short Qty", "Fulfill %",
                "Est. Cost", "Boxes Packed", "Manifests"
        );
        reportTable = new JTable(tableModel);
        reportTable.setRowHeight(24);
        reportTable.setAutoCreateRowSorter(true);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

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

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(statusPanel, BorderLayout.CENTER);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        runButton = new JButton("Run");
        exportCsvButton = new JButton("Export CSV");
        exportTablePdfButton = new JButton("Export Table PDF");
        exportChartPdfButton = new JButton("Export Chart PDF");
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        btns.add(runButton);
        btns.add(exportCsvButton);
        btns.add(exportTablePdfButton);
        btns.add(exportChartPdfButton);
        btns.add(close);
        panel.add(btns, BorderLayout.EAST);
        return panel;
    }

    private void addListeners() {
        runButton.addActionListener(e -> onRun());
        exportCsvButton.addActionListener(e -> onExportCsv());
        exportTablePdfButton.addActionListener(e -> onExportTablePdf());
        exportChartPdfButton.addActionListener(e -> onExportChartPdf());
    }

    private void onRun() {
        ReportFilters filters = new ReportFilters();
        filters.setYear(filterPanel.getSelectedYear());
        filters.setMonth(filterPanel.getSelectedMonth());
        try {
            if (!customerIdFilter.getText().isBlank()) {
                filters.setCustomerId(Long.parseLong(customerIdFilter.getText().trim()));
            }
            if (!branchIdFilter.getText().isBlank()) {
                filters.setBranchId(Long.parseLong(branchIdFilter.getText().trim()));
            }
        } catch (NumberFormatException ex) {
            statusPanel.setError("Customer/Branch ID must be numeric.");
            return;
        }
        if (!productCategoryFilter.getText().isBlank()) {
            filters.setProductCategory(productCategoryFilter.getText().trim());
        }

        tableModel.setRowCount(0);
        statusPanel.setInfo("Running report...");

        try {
            List<R3MonthlyThroughputRow> rows = dao.findMonthlyThroughput(filters);
            if (rows.isEmpty()) {
                statusPanel.setInfo("No data for selected filters.");
            } else {
                for (R3MonthlyThroughputRow r : rows) {
                    tableModel.addRow(new Object[]{
                            r.getYearMonth(),
                            r.getCustomerName(),
                            r.getBranchName(),
                            r.getTotalTicketsClosed(),
                            r.getShortClosedTickets(),
                            fmtPct(r.getShortCloseRatePct()),
                            r.getTotalRequestedQty(),
                            r.getTotalDeliveredQty(),
                            r.getTotalShortQty(),
                            fmtPct(r.getFulfillmentRatePct()),
                            r.getEstimatedReturnCost(),
                            r.getBoxesPacked(),
                            r.getManifestsCreated()
                    });
                }
                resizeColumns();
                populateChart(rows);
                statusPanel.setSuccess("Loaded " + rows.size() + " row(s).");
            }
        } catch (SQLException ex) {
            statusPanel.setError("Error fetching data: " + ex.getMessage());
        }
    }

    private void resizeColumns() {
        int[] widths = {90, 140, 140, 90, 90, 80, 90, 90, 90, 80, 90, 90, 90};
        TableColumnModel cm = reportTable.getColumnModel();
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
            TableColumn c = cm.getColumn(i);
            c.setPreferredWidth(widths[i]);
        }
    }

    private void populateChart(List<R3MonthlyThroughputRow> rows) {
        if (rows == null || rows.isEmpty()) {
            chartPanel.setData(null, null, null, null);
            return;
        }
        java.util.List<String> cats = new java.util.ArrayList<>();
        java.util.List<Double> vals = new java.util.ArrayList<>();
        for (R3MonthlyThroughputRow r : rows) {
            cats.add(r.getYearMonth());
            vals.add((double) r.getBoxesPacked());
        }
        SimpleBarChartPanel.Series series = SimpleBarChartPanel.series("Boxes Packed", vals, Color.decode("#2d9cdb"));
        chartPanel.setData(cats, java.util.Collections.singletonList(series), "Boxes Packed per Month", "Boxes");
    }

    private void onExportCsv() {
        if (tableModel.getRowCount() == 0) {
            statusPanel.setError("No data to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export CSV");
        chooser.setSelectedFile(new File("R3_monthly_throughput.csv"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try (FileWriter w = new FileWriter(f)) {
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    if (c > 0) w.write(",");
                    w.write(tableModel.getColumnName(c));
                }
                w.write("\n");
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        if (c > 0) w.write(",");
                        Object val = tableModel.getValueAt(r, c);
                        w.write(val == null ? "" : val.toString());
                    }
                    w.write("\n");
                }
                statusPanel.setSuccess("Exported CSV to " + f.getAbsolutePath());
            } catch (IOException ex) {
                statusPanel.setError("CSV export failed: " + ex.getMessage());
            }
        }
    }

    private void onExportTablePdf() {
        if (tableModel.getRowCount() == 0) {
            statusPanel.setError("No data to export.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Table to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File("R3_monthly_throughput_table.pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ensurePdf(chooser.getSelectedFile());
            try {
                PdfExporter.exportTable(reportTable, f, "R3 – Monthly Inventory / Pack-Dispatch Throughput");
                statusPanel.setSuccess("Exported table PDF to " + f.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Table PDF export failed: " + ex.getMessage());
            }
        }
    }

    private void onExportChartPdf() {
        if (chartPanel == null || !chartPanel.hasData()) {
            statusPanel.setError("Run the report before exporting the chart.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Chart to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File("R3_monthly_throughput_chart.pdf"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = ensurePdf(chooser.getSelectedFile());
            try {
                int w = Math.max(chartPanel.getWidth(), 800);
                int h = Math.max(chartPanel.getHeight(), 400);
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = img.createGraphics();
                chartPanel.paintAll(g2);
                g2.dispose();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                com.lowagie.text.Image pdfImg = com.lowagie.text.Image.getInstance(baos.toByteArray());
                pdfImg.scaleToFit(800, 500);
                com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4.rotate());
                com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(f));
                doc.open();
                doc.add(pdfImg);
                doc.close();
                statusPanel.setSuccess("Exported chart PDF to " + f.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Chart PDF export failed: " + ex.getMessage());
            }
        }
    }

    private File ensurePdf(File f) {
        if (!f.getName().toLowerCase().endsWith(".pdf")) {
            return new File(f.getParentFile(), f.getName() + ".pdf");
        }
        return f;
    }

    private String fmtPct(BigDecimal v) {
        if (v == null) return "N/A";
        return v.setScale(2, BigDecimal.ROUND_HALF_UP) + "%";
    }
}
