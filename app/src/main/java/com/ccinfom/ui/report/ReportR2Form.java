package com.ccinfom.ui.report;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.Employee;
import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportTableModel;
import com.ccinfom.report.r2.R2WeeklyProductivityRow;
import com.ccinfom.report.r2.ReportR2Dao;
import com.ccinfom.ui.common.ComboItem;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.ui.common.SimpleBarChartPanel;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.UiTaskRunner;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Color;
import java.io.File;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Swing UI for R2 – Weekly Picker Productivity.
 */
public class ReportR2Form extends JFrame {

    private final ReportFilterPanel filterPanel;
    private final ReportTableModel tableModel;
    private final JTable table;
    private final SimpleBarChartPanel chartPanel;
    private final StatusPanel statusPanel;
    private final JComboBox<ComboItem<Employee>> pickerCombo;
    private final JComboBox<String> categoryCombo;
    private final ReportR2Dao dao;
    private final LookupDao lookupDao;

    public ReportR2Form() {
        super("R2 – Weekly Picker Productivity");
        this.dao = new ReportR2Dao();
        this.lookupDao = new LookupDaoImpl();
        this.filterPanel = new ReportFilterPanel();
        this.filterPanel.setSupportedModes(EnumSet.of(ReportFilterPanel.PeriodMode.ISO_WEEK));

        this.tableModel = new ReportTableModel();
        tableModel.setColumns(
            "ISO Year", 
            "ISO Week", 
            "Week Range", 
            "Picker", 
            "Category", 
            "Lines Picked", 
            "Units Picked", 
            "Units/Hour", 
            "Avg Pick Time (min)", 
            "Short/Error Lines"
        );
        this.table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        this.chartPanel = new SimpleBarChartPanel();

        this.statusPanel = new StatusPanel("Status: Ready");
        this.pickerCombo = new JComboBox<>();
        this.categoryCombo = new JComboBox<>();
        loadLookupCombos();
        
        filterPanel.addFilterField("Picker", pickerCombo);
        filterPanel.addFilterField("Category", categoryCombo);

        initLayout();
    }

    private void initLayout() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        filterWrapper.add(filterPanel, BorderLayout.CENTER);
        add(filterWrapper, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Table", new JScrollPane(table));
        tabs.addTab("Chart", chartPanel);
        add(tabs, BorderLayout.CENTER);

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> onRunReport());

        JButton exportButton = new JButton("Export PDF");
        exportButton.addActionListener(e -> onExportPdf());

        JButton exportChartButton = new JButton("Save Chart PNG");
        exportChartButton.addActionListener(e -> onExportChart());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        buttonPanel.add(runButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(exportChartButton);
        buttonPanel.add(closeButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statusPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.EAST);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(southPanel, BorderLayout.SOUTH);

        setSize(1024, 600);
        setLocationRelativeTo(null);
    }

    private void onRunReport() {
        int year = filterPanel.getSelectedYear();
        int isoWeek = filterPanel.getSelectedIsoWeek();
        Long pickerId = getSelectedPickerId();
        String category = getSelectedCategory();

        UiTaskRunner.run(
            statusPanel,
            "Running weekly productivity report...",
            null,
            () -> dao.findWeeklyProductivity(year, isoWeek, pickerId, category),
            this::onReportDataLoaded,
            this::onReportError
        );
    }

    private void onReportDataLoaded(List<R2WeeklyProductivityRow> rows) {
        SwingUtilities.invokeLater(() -> {
            tableModel.loadRows(rows.stream()
                .map(row -> new Object[]{
                    row.getIsoYear(),
                    row.getIsoWeek(),
                    row.getIsoWeekStart() + " to " + row.getIsoWeekEnd(),
                    row.getPickerFullName(),
                    row.getProductCategory(),
                    row.getLinesPicked(),
                    row.getUnitsPicked(),
                    row.getUnitsPerHour(),
                    row.getAvgPickTimeMin(),
                    row.getShortErrorLines()
                })
                .toList());

            updateChart(rows);

            String summary = buildFilterSummary(
                filterPanel.getSelectedYear(), 
                filterPanel.getSelectedIsoWeek(),
                getSelectedPickerId(), 
                getSelectedCategory()
            );
            
            if (rows.isEmpty()) {
                statusPanel.setInfo("No data found for " + summary + ".");
            } else {
                long totalLines = rows.stream().mapToLong(R2WeeklyProductivityRow::getLinesPicked).sum();
                double totalUnits = rows.stream().mapToDouble(row -> row.getUnitsPicked().doubleValue()).sum();
                
                statusPanel.setSuccess(String.format(
                    "Showing %d row(s) for %s. Total Lines: %d, Total Units: %.1f",
                    rows.size(),
                    summary,
                    totalLines,
                    totalUnits
                ));
            }
        });
    }

    private void onReportError(Throwable throwable) {
        SwingUtilities.invokeLater(() -> {
            statusPanel.setError("Failed to run report: " + throwable.getMessage());
        });
    }

    private void onExportPdf() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, 
                "Run the report before exporting.", 
                "Export PDF",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Weekly Picker Productivity to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File(String.format(
            "r2-weekly-productivity-%s.pdf",
            buildFilterSummary(
                filterPanel.getSelectedYear(), 
                filterPanel.getSelectedIsoWeek(),
                getSelectedPickerId(), 
                getSelectedCategory()
            ).replace(", ", "_").replace(" ", "-")
        )));
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = ensurePdfExtension(chooser.getSelectedFile());
            try {
                PdfExporter.exportTable(
                    table, 
                    file,
                    String.format(
                        "R2 – Weekly Picker Productivity (%s)",
                        buildFilterSummary(
                            filterPanel.getSelectedYear(), 
                            filterPanel.getSelectedIsoWeek(),
                            getSelectedPickerId(), 
                            getSelectedCategory()
                        )
                    )
                );
                statusPanel.setSuccess("PDF exported to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Export failed: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                    "Export failed: " + ex.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onExportChart() {
        if (!chartPanel.hasData()) {
            JOptionPane.showMessageDialog(this,
                    "Run the report before exporting the chart.",
                    "Export Chart",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Chart as PNG");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Files", "png"));
        chooser.setSelectedFile(new File("r2-weekly-productivity-chart.png"));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            try {
                chartPanel.saveAsPng(file, 900, 500);
                statusPanel.setSuccess("Chart saved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Failed to save chart: " + ex.getMessage());
            }
        }
    }

    private void updateChart(List<R2WeeklyProductivityRow> rows) {
        if (rows == null || rows.isEmpty()) {
            chartPanel.setData(List.of(), List.of(), "Picker Productivity", "Lines Picked");
            return;
        }
        List<String> labels = rows.stream()
                .map(r -> String.format("%d-W%02d", r.getIsoYear(), r.getIsoWeek()))
                .collect(Collectors.toList());
        List<Double> lines = rows.stream()
                .map(r -> (double) r.getLinesPicked())
                .collect(Collectors.toList());
        chartPanel.setData(
                labels,
                List.of(SimpleBarChartPanel.series("Lines Picked", lines, new Color(79, 129, 189))),
                "Weekly Picker Productivity",
                "Lines"
        );
    }

    private File ensurePdfExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            return new File(file.getParentFile(), file.getName() + ".pdf");
        }
        return file;
    }

    private void loadLookupCombos() {
        try {
            // Load pickers using the enhanced LookupDao interface
            pickerCombo.removeAllItems();
            pickerCombo.addItem(new ComboItem<>(null, "All Pickers"));
            lookupDao.listActivePickers().forEach(picker ->
                pickerCombo.addItem(new ComboItem<>(picker, 
                    picker.getFirstName() + " " + picker.getLastName()))
            );

            // Load categories using the enhanced LookupDao interface
            categoryCombo.removeAllItems();
            categoryCombo.addItem("All Categories");
            lookupDao.listProductCategories().forEach(categoryCombo::addItem);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Failed to load lookup data: " + e.getMessage(),
                "Lookup Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private Long getSelectedPickerId() {
        @SuppressWarnings("unchecked")
        ComboItem<Employee> item = (ComboItem<Employee>) pickerCombo.getSelectedItem();
        return item != null && item.getValue() != null ? item.getValue().getEmployeeId() : null;
    }

    private String getSelectedCategory() {
        String selected = (String) categoryCombo.getSelectedItem();
        return "All Categories".equals(selected) ? null : selected;
    }

    private String buildFilterSummary(int year, int isoWeek, Long pickerId, String category) {
        StringBuilder summary = new StringBuilder(String.format("Year %d, Week %d", year, isoWeek));
        if (pickerId != null) {
            summary.append(", ").append(pickerCombo.getSelectedItem());
        }
        if (category != null) {
            summary.append(", Category=").append(category);
        }
        return summary.toString();
    }

    public static void showWindow() {
        SwingUtilities.invokeLater(() -> new ReportR2Form().setVisible(true));
    }
}
