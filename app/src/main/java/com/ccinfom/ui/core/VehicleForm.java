package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.VehicleDaoImpl;
import com.ccinfom.model.Vehicle;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.VehicleService;
import com.ccinfom.service.impl.VehicleServiceImpl;
import com.ccinfom.ui.common.StatusPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
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
        JButton toggleBtn = new JButton("Toggle Active");
        toggleBtn.addActionListener(e -> toggleStatus());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadVehicles());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(toggleBtn);
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
}
