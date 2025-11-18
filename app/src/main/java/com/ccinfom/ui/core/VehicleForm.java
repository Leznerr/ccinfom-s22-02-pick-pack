package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.VehicleDaoImpl;
import com.ccinfom.model.Vehicle;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.VehicleService;
import com.ccinfom.service.impl.VehicleServiceImpl;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.config.DbConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class VehicleForm extends JFrame {

    private final VehicleService vehicleService = new VehicleServiceImpl(new VehicleDaoImpl());
    private final VehicleTableModel tableModel = new VehicleTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    public VehicleForm() {
        super("Vehicles - Core Record Management");
        initLayout();
        loadVehicles();
    }

    private void initLayout() {
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(new EmptyBorder(8, 8, 0, 8));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        JButton applyBtn = new JButton("Apply");
        applyBtn.addActionListener(e -> loadVehicles());
        top.add(applyBtn);
        showInactive.addActionListener(e -> loadVehicles());
        top.add(showInactive);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(0, 8, 8, 8));
        bottom.add(statusPanel, BorderLayout.CENTER);

        JButton addBtn = new JButton("Add Vehicle");
        addBtn.addActionListener(e -> showAddDialog());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> showEditDialog());
        JButton toggleBtn = new JButton("Delete");
        toggleBtn.addActionListener(e -> toggleStatus());
        JButton viewBtn = new JButton("View Details");
        viewBtn.addActionListener(e -> showDetails());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadVehicles());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        tint(addBtn, new Color(46, 160, 67));    // create = green
        tint(editBtn, new Color(10, 132, 255));  // update = blue
        tint(toggleBtn, new Color(219, 68, 55)); // delete = red

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(toggleBtn);
        actions.add(viewBtn);
        actions.add(refreshBtn);
        actions.add(closeBtn);

        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadVehicles() {
        try {
            List<Vehicle> vehicles = vehicleService.listVehicles(
                    searchField.getText(),
                    showInactive.isSelected());
            tableModel.setRows(vehicles);
            statusPanel.setSuccess("Loaded " + vehicles.size() + " vehicles.");
        } catch (SQLException ex) {
            statusPanel.setError("Failed to load vehicles: " + ex.getMessage());
        }
    }

    private Optional<Vehicle> selectedVehicle() {
        int row = table.getSelectedRow();
        if (row < 0) return Optional.empty();
        int modelRow = table.convertRowIndexToModel(row);
        return Optional.of(tableModel.getRow(modelRow));
    }

    private void showAddDialog() {
        VehicleDialog dialog = new VehicleDialog(this, "Add Vehicle", null);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(v -> {
            try {
                vehicleService.createVehicle(v, "ui");
                loadVehicles();
                statusPanel.setSuccess("Vehicle created.");
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError(ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void showEditDialog() {
        Vehicle vehicle = selectedVehicle().orElse(null);
        if (vehicle == null) {
            JOptionPane.showMessageDialog(this, "Select a vehicle first.");
            return;
        }
        VehicleDialog dialog = new VehicleDialog(this, "Edit Vehicle", vehicle);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(v -> {
            try {
                v.setVehicleId(vehicle.getVehicleId());
                vehicleService.updateVehicle(v, "ui");
                loadVehicles();
                statusPanel.setSuccess("Vehicle updated.");
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError(ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void toggleStatus() {
        Vehicle vehicle = selectedVehicle().orElse(null);
        if (vehicle == null) {
            JOptionPane.showMessageDialog(this, "Select a vehicle first.");
            return;
        }
        boolean makeActive = vehicle.getVehicleStatus().equalsIgnoreCase("inactive");
        int confirm = JOptionPane.showConfirmDialog(this,
                "Set vehicle to " + (makeActive ? "available" : "inactive") + "?",
                "Confirm", JOptionPane.OK_CANCEL_OPTION);
        if (confirm != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            vehicleService.setVehicleActive(vehicle.getVehicleId(), makeActive, "ui");
            loadVehicles();
            statusPanel.setSuccess("Vehicle status updated.");
        } catch (SQLException | ValidationException ex) {
            statusPanel.setError(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Show vehicle details plus recent dispatches/manifests.
     */
    private void showDetails() {
        Vehicle vehicle = selectedVehicle().orElse(null);
        if (vehicle == null) {
            JOptionPane.showMessageDialog(this, "Select a vehicle first.");
            return;
        }

        JDialog dlg = new JDialog(this, "Vehicle Details - " + vehicle.getPlateNumber(), true);
        dlg.setLayout(new BorderLayout(8, 8));

        JPanel details = new JPanel(new GridLayout(0, 2, 6, 6));
        details.setBorder(new EmptyBorder(10, 10, 10, 10));
        details.add(new JLabel("Plate:")); details.add(new JLabel(vehicle.getPlateNumber()));
        details.add(new JLabel("Type:")); details.add(new JLabel(vehicle.getVehicleType()));
        details.add(new JLabel("Capacity:")); details.add(new JLabel(String.valueOf(vehicle.getCapacity())));
        details.add(new JLabel("SLA Hours:")); details.add(new JLabel(String.valueOf(vehicle.getSlaHours())));
        details.add(new JLabel("Status:")); details.add(new JLabel(vehicle.getVehicleStatus()));
        details.add(new JLabel("Updated:")); details.add(new JLabel(vehicle.getUpdatedAt() != null ? vehicle.getUpdatedAt().toString() : ""));
        details.add(new JLabel("Updated By:")); details.add(new JLabel(vehicle.getUpdatedBy()));

        JTable dispatchTable = buildDispatchTable(vehicle.getVehicleId());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Details", new JScrollPane(details));
        tabs.addTab("Recent Dispatches", new JScrollPane(dispatchTable));

        dlg.add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dlg.add(south, BorderLayout.SOUTH);

        dlg.setSize(780, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JTable buildDispatchTable(long vehicleId) {
        String[] headers = {"Dispatch ID", "Manifest", "Driver", "Customer", "Branch", "Built", "Depart", "Arrive", "Status"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = """
            SELECT dh.dispatch_id, dh.manifest_no,
                   CONCAT(e.first_name, ' ', e.last_name) AS driver_name,
                   c.customer_name, b.branch_name,
                   dh.dispatched_at, dh.depart_ts, dh.arrive_ts, dh.dispatch_status
            FROM dispatch_hdr dh
            JOIN employees e ON e.employee_id = dh.driver_id
            JOIN pick_ticket_hdr pth ON pth.pick_ticket_id = dh.pick_ticket_id
            JOIN customers c ON c.customer_id = pth.customer_id
            JOIN branches b ON b.branch_id = pth.branch_id
            WHERE dh.vehicle_id = ?
            ORDER BY dh.created_at DESC
            LIMIT 20
        """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getObject("dispatch_id"),
                            rs.getObject("manifest_no"),
                            rs.getObject("driver_name"),
                            rs.getObject("customer_name"),
                            rs.getObject("branch_name"),
                            rs.getObject("dispatched_at"),
                            rs.getObject("depart_ts"),
                            rs.getObject("arrive_ts"),
                            rs.getObject("dispatch_status")
                    });
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load dispatches: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"No data for selected record."});
        }
        JTable tbl = new JTable(model);
        tbl.setAutoCreateRowSorter(true);
        return tbl;
    }

    private static class VehicleTableModel extends AbstractTableModel {
        private final List<Vehicle> rows = new ArrayList<>();
        private final String[] columns = {"Plate", "Type", "Capacity", "SLA (hrs)", "Status", "Updated At", "Updated By"};
        private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void setRows(List<Vehicle> vehicles) {
            rows.clear();
            rows.addAll(vehicles);
            fireTableDataChanged();
        }

        public Vehicle getRow(int row) {
            return rows.get(row);
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Vehicle v = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> v.getPlateNumber();
                case 1 -> v.getVehicleType();
                case 2 -> v.getCapacity();
                case 3 -> v.getSlaHours();
                case 4 -> v.getVehicleStatus();
                case 5 -> v.getUpdatedAt() != null ? fmt.format(v.getUpdatedAt()) : "";
                case 6 -> v.getUpdatedBy();
                default -> "";
            };
        }
    }

    private static class VehicleDialog extends JDialog {
        private final JTextField plateField = new JTextField(12);
        private final JComboBox<String> typeField = new JComboBox<>(new String[]{"van", "truck", "motorcycle"});
        private final JTextField capacityField = new JTextField(10);
        private final JTextField slaField = new JTextField(6);
        private Vehicle result;

        VehicleDialog(Frame owner, String title, Vehicle vehicle) {
            super(owner, title, true);
            setLayout(new BorderLayout(8, 8));
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(new EmptyBorder(12, 12, 12, 12));
            form.add(new JLabel("Plate Number:"));
            form.add(plateField);
            form.add(new JLabel("Vehicle Type:"));
            form.add(typeField);
            form.add(new JLabel("Capacity:"));
            form.add(capacityField);
            form.add(new JLabel("SLA Hours:"));
            form.add(slaField);

            if (vehicle != null) {
                plateField.setText(vehicle.getPlateNumber());
                typeField.setSelectedItem(vehicle.getVehicleType());
                capacityField.setText(String.valueOf(vehicle.getCapacity()));
                slaField.setText(String.valueOf(vehicle.getSlaHours()));
            }

            JButton saveBtn = new JButton("Save");
            saveBtn.addActionListener(e -> onSave());
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());
            tint(saveBtn, new Color(10, 132, 255)); // save/update = blue
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttons.add(saveBtn);
            buttons.add(cancelBtn);

            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
            pack();
            setLocationRelativeTo(owner);
        }

        private void onSave() {
            try {
                Vehicle v = new Vehicle();
                v.setPlateNumber(plateField.getText());
                v.setVehicleType(typeField.getSelectedItem().toString());
                v.setCapacity(parseDecimal(capacityField.getText(), "Capacity"));
                v.setSlaHours(parseInt(slaField.getText(), "SLA Hours"));
                this.result = v;
                dispose();
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid input", JOptionPane.ERROR_MESSAGE);
            }
        }

        private BigDecimal parseDecimal(String text, String field) {
            try {
                return new BigDecimal(text.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException(field + " must be a valid number.");
            }
        }

        private int parseInt(String text, String field) {
            try {
                return Integer.parseInt(text.trim());
            } catch (Exception ex) {
                throw new IllegalArgumentException(field + " must be a whole number.");
            }
        }

        Optional<Vehicle> getResult() {
            return Optional.ofNullable(result);
        }
    }

    private static void tint(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }
}
