/**
 * TODO[Phase F - R4]:
 *  - Create the On-Time Delivery & PoD compliance UI with filters for month/week, vehicle, driver, customer, etc.
 *  - Bind to ReportR4Dao to show on-time flags and PoD indicators, highlighting late deliveries.
 *  - Support export and optional detail drill-ins.
 */

package com.ccinfom.ui.report;

import com.ccinfom.report.ReportFilterPanel;
import com.ccinfom.report.ReportFilterPanel.PeriodMode;
import com.ccinfom.report.r4.ReportR4Dao;
import com.ccinfom.ui.common.PdfExporter;
import com.ccinfom.report.r4.R4OnTimeDeliveryRow; // <-- your R4 row DTO

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ReportR4Form extends JFrame {

    private final ReportFilterPanel filterPanel;
    private final JComboBox<String> vehicleCombo;
    private final JComboBox<String> driverCombo;

    private final JButton runButton;
    private final JButton exportButton;

    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JLabel statusLabel;

    public ReportR4Form() {
        super("R4 – On-Time Delivery & PoD Compliance");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLayout(new BorderLayout(8, 8));

        // === 1. FILTER PANEL ===
        filterPanel = new ReportFilterPanel();
        filterPanel.setSupportedModes(
                java.util.EnumSet.of(PeriodMode.MONTH, PeriodMode.ISO_WEEK)
        );

        vehicleCombo = new JComboBox<>();
        driverCombo = new JComboBox<>();

        filterPanel.addFilterField("Vehicle", vehicleCombo);
        filterPanel.addFilterField("Driver", driverCombo);

        loadVehicleList();
        loadDriverList();

        add(filterPanel, BorderLayout.NORTH);

        // === 2. TABLE ===
        tableModel = new DefaultTableModel(
            new String[] {
                "Period", "Customer", "Product", "Category",
                "On-Time Deliveries", "Late Deliveries",
                "Short Closed", "On-Time %"
            },
            0
        );

        table = new JTable(tableModel);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // === 3. BOTTOM ===
        JPanel bottomPanel = new JPanel(new BorderLayout());

        JPanel buttonPanel = new JPanel();
        runButton = new JButton("Run");
        exportButton = new JButton("Export");

        buttonPanel.add(runButton);
        buttonPanel.add(exportButton);

        bottomPanel.add(buttonPanel, BorderLayout.WEST);

        statusLabel = new JLabel(" ");
        bottomPanel.add(statusLabel, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);

        // === 4. EVENTS ===
        runButton.addActionListener(e -> runReport());
        exportButton.addActionListener(e -> exportReport());
    }

    // ============================================================
    // LOAD COMBOS
    // ============================================================

    private void loadVehicleList() {
        try (Connection conn = com.ccinfom.config.DbConnection.getConnection()) {

            var stmt = conn.prepareStatement("SELECT plate_number FROM vehicles ORDER BY plate_number");
            var rs = stmt.executeQuery();

            vehicleCombo.addItem("(All)");
            while (rs.next()) {
                vehicleCombo.addItem(rs.getString(1));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load vehicles: " + ex.getMessage());
        }
    }

    private void loadDriverList() {
        try (Connection conn = com.ccinfom.config.DbConnection.getConnection()) {

            var stmt = conn.prepareStatement(
                    "SELECT CONCAT(first_name, ' ', last_name) " +
                    "FROM employees WHERE employee_role='driver' ORDER BY first_name"
            );
            var rs = stmt.executeQuery();

            driverCombo.addItem("(All)");
            while (rs.next()) {
                driverCombo.addItem(rs.getString(1));
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load drivers: " + ex.getMessage());
        }
    }

    // ============================================================
    // RUN REPORT
    // ============================================================

    private void runReport() {
        statusLabel.setText("Running report...");
        tableModel.setRowCount(0);

        int year = filterPanel.getSelectedYear();
        boolean isMonth = filterPanel.getPeriodMode() == PeriodMode.MONTH;
        int period = isMonth
                ? filterPanel.getSelectedMonth()
                : filterPanel.getSelectedIsoWeek();

        String vehicle = vehicleCombo.getSelectedItem().toString();
        String driver = driverCombo.getSelectedItem().toString();

        if ("(All)".equals(vehicle)) vehicle = null;
        if ("(All)".equals(driver)) driver = null;

        try (Connection conn = com.ccinfom.config.DbConnection.getConnection()) {

            ReportR4Dao dao = new ReportR4Dao(conn);

            // ★ FIXED: correct DTO name
            List<R4OnTimeDeliveryRow> rows =
                    dao.findR4(year, isMonth, period, vehicle, driver);

            if (rows.isEmpty()) {
                statusLabel.setText("No data found for selected filters.");
                return;
            }

            for (R4OnTimeDeliveryRow r : rows) {
                tableModel.addRow(new Object[]{
                    r.getYearMonthLabel(),
                    r.getCustomerName(),
                    r.getProductName(),
                    r.getCategory(),
                    r.getOnTimeDeliveries(),
                    r.getLateDeliveries(),
                    r.getShortClosedShipments(),
                    r.getOnTimePercentage()
                });
            }

            statusLabel.setText("Loaded " + rows.size() + " records.");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error running report: " + ex.getMessage());
            statusLabel.setText("Error.");
        }
    }

    // ============================================================
    // EXPORT (Stage F)
    // ============================================================

    private void exportReport() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Run the report before exporting.", "Export PDF",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Report to PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));

        // Default filename, e.g., r4-on-time-2025-11.pdf
        String defaultFileName = String.format("r4-on-time-%s.pdf",
                filterPanel.getSelectedYear() + "-" +
                (filterPanel.getPeriodMode() == PeriodMode.MONTH
                        ? filterPanel.getSelectedMonth()
                        : filterPanel.getSelectedIsoWeek()));
        chooser.setSelectedFile(new File(defaultFileName));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = ensurePdfExtension(chooser.getSelectedFile());

            try {
                PdfExporter.exportTable(table, file,
                        String.format("R4 – On-Time Delivery & PoD (%s)",
                                filterPanel.getPeriodMode() == PeriodMode.MONTH
                                        ? "Month " + filterPanel.getSelectedMonth() + " " + filterPanel.getSelectedYear()
                                        : "ISO Week " + filterPanel.getSelectedIsoWeek() + " " + filterPanel.getSelectedYear()));
                statusLabel.setText("PDF exported to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                        "Export PDF", JOptionPane.ERROR_MESSAGE);
                statusLabel.setText("Export failed.");
            }
        }
    }

    private File ensurePdfExtension(File file) {
    String path = file.getAbsolutePath();
    if (!path.toLowerCase().endsWith(".pdf")) {
        file = new File(path + ".pdf");
    }
    return file;
}

    // ============================================================
    // SHOW WINDOW (for MainApp)
    // ============================================================

    public static void showWindow() {
        SwingUtilities.invokeLater(() -> {
            ReportR4Form frm = new ReportR4Form();
            frm.setVisible(true);
        });
    }

} // end
