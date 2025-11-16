package com.ccinfom.ui.report;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.Branch;
import com.ccinfom.model.Customer;
import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportTableModel;
import com.ccinfom.report.r1.R1DailyOutcomeRow;
import com.ccinfom.report.r1.ReportR1Dao;
import com.ccinfom.ui.common.ComboItem;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.ui.common.StatusPanel;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;

/**
 * Swing UI for R1 – Daily Pick & Pack Outcomes.
 */
public class ReportR1Form extends JFrame {

    private final ReportFilterPanel filterPanel;
    private final ReportTableModel tableModel;
    private final JTable table;
    private final StatusPanel statusPanel;
    private final JComboBox<ComboItem<Customer>> customerCombo;
    private final JComboBox<ComboItem<Branch>> branchCombo;
    private final ReportR1Dao dao;

    public ReportR1Form() {
        super("R1 – Daily Pick & Pack Outcomes");
        this.dao = new ReportR1Dao();
        this.filterPanel = new ReportFilterPanel();
        this.filterPanel.setSupportedModes(EnumSet.of(ReportFilterPanel.PeriodMode.MONTH));

        this.tableModel = new ReportTableModel();
        tableModel.setColumns("Date", "Day", "Delivered", "Short-Closed", "Tickets", "Shortage Lines", "Shortage Units");
        this.table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        this.statusPanel = new StatusPanel("Status: Ready");
        this.customerCombo = new JComboBox<>();
        this.branchCombo = new JComboBox<>();
        loadLookupCombos();
        filterPanel.addFilterField("Customer", customerCombo);
        filterPanel.addFilterField("Branch", branchCombo);

        initLayout();
    }

    private void initLayout() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        filterWrapper.add(filterPanel, BorderLayout.CENTER);
        add(filterWrapper, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        JButton runButton = new JButton("Run");
        runButton.addActionListener(e -> onRunReport());

        JButton exportButton = new JButton("Export PDF");
        exportButton.addActionListener(e -> onExportPdf());

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        buttonPanel.add(runButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(statusPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.EAST);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        add(southPanel, BorderLayout.SOUTH);

        setSize(980, 560);
        setLocationRelativeTo(null);
    }

    private void onRunReport() {
        int year = filterPanel.getSelectedYear();
        int month = filterPanel.getSelectedMonth();
        Long customerId = getSelectedCustomerId();
        Long branchId = getSelectedBranchId();
        try {
            List<R1DailyOutcomeRow> rows = dao.findDailyOutcomes(year, month, customerId, branchId);
            tableModel.loadRows(rows.stream()
                    .map(r -> new Object[]{
                            r.getCalendarDate(),
                            r.getDayNameLabel(),
                            r.getDeliveredTicketCount(),
                            r.getShortClosedTicketCount(),
                            r.getTicketsClosed(),
                            r.getShortageLineCount(),
                            r.getShortageUnits()
                    })
                    .toList());

            String summary = buildFilterSummary(year, month, customerId, branchId);
            if (rows.isEmpty()) {
                statusPanel.setInfo("No data for " + summary + ".");
            } else {
                statusPanel.setSuccess(String.format("Showing %d row(s) for %s.", rows.size(), summary));
            }
        } catch (SQLException ex) {
            statusPanel.setError("Failed to run report: " + ex.getMessage());
        }
    }

    private void onExportPdf() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Run the report before exporting.", "Export PDF",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Report to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        chooser.setSelectedFile(new File(String.format("r1-daily-outcomes-%s.pdf",
                buildFilterSummary(filterPanel.getSelectedYear(), filterPanel.getSelectedMonth(),
                        getSelectedCustomerId(), getSelectedBranchId()).replace(", ", "_"))));
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = ensurePdfExtension(chooser.getSelectedFile());
            try {
                PdfExporter.exportTable(table, file,
                        String.format("R1 – Daily Pick & Pack Outcomes (%s)",
                                buildFilterSummary(filterPanel.getSelectedYear(), filterPanel.getSelectedMonth(),
                                        getSelectedCustomerId(), getSelectedBranchId())));
                statusPanel.setSuccess("PDF exported to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                statusPanel.setError("Export failed: " + ex.getMessage());
            }
        }
    }

    private File ensurePdfExtension(File file) {
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            return new File(file.getParentFile(), file.getName() + ".pdf");
        }
        return file;
    }

    private void loadLookupCombos() {
        LookupDao lookupDao = new LookupDaoImpl();
        try {
            customerCombo.removeAllItems();
            customerCombo.addItem(new ComboItem<>(null, "All Customers"));
            lookupDao.listActiveCustomers().forEach(customer ->
                    customerCombo.addItem(new ComboItem<>(customer, customer.getCustomerName())));

            branchCombo.removeAllItems();
            branchCombo.addItem(new ComboItem<>(null, "All Branches"));
            lookupDao.listActiveBranches().forEach(branch ->
                    branchCombo.addItem(new ComboItem<>(branch, branch.getBranchName())));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load customer/branch lookups: " + e.getMessage(),
                    "Lookup Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private Long getSelectedCustomerId() {
        @SuppressWarnings("unchecked")
        ComboItem<Customer> item = (ComboItem<Customer>) customerCombo.getSelectedItem();
        return item != null && item.getValue() != null ? item.getValue().getCustomerId() : null;
    }

    private Long getSelectedBranchId() {
        @SuppressWarnings("unchecked")
        ComboItem<Branch> item = (ComboItem<Branch>) branchCombo.getSelectedItem();
        return item != null && item.getValue() != null ? item.getValue().getBranchId() : null;
    }

    private String buildFilterSummary(int year, int month, Long customerId, Long branchId) {
        StringBuilder summary = new StringBuilder(String.format("%d-%02d", year, month));
        if (customerId != null) {
            summary.append(", Customer=").append(customerCombo.getSelectedItem());
        }
        if (branchId != null) {
            summary.append(", Branch=").append(branchCombo.getSelectedItem());
        }
        return summary.toString();
    }

    public static void showWindow() {
        SwingUtilities.invokeLater(() -> new ReportR1Form().setVisible(true));
    }
}
