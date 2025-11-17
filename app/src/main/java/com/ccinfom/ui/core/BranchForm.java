package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.BranchDaoImpl;
import com.ccinfom.model.Branch;
import com.ccinfom.service.BranchService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.BranchServiceImpl;
import com.ccinfom.ui.common.StatusPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BranchForm extends JFrame {

    private final BranchService branchService = new BranchServiceImpl(new BranchDaoImpl());
    private final BranchTableModel tableModel = new BranchTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    public BranchForm() {
        super("Branches – Core Record Management");
        initLayout();
        loadBranches();
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
        JButton apply = new JButton("Apply");
        apply.addActionListener(e -> loadBranches());
        top.add(apply);
        showInactive.addActionListener(e -> loadBranches());
        top.add(showInactive);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Branch");
        addBtn.addActionListener(e -> onAdd());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> onEdit());
        JButton toggleBtn = new JButton("Toggle Active");
        toggleBtn.addActionListener(e -> onToggle());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadBranches());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(toggleBtn);
        actions.add(refreshBtn);
        actions.add(closeBtn);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(new EmptyBorder(0, 8, 8, 8));
        bottom.add(statusPanel, BorderLayout.CENTER);
        bottom.add(actions, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
    }

    private void loadBranches() {
        try {
            List<Branch> branches = branchService.listBranches(searchField.getText(), showInactive.isSelected());
            tableModel.setRows(branches);
            statusPanel.setSuccess(String.format("Loaded %d branch(es).", branches.size()));
        } catch (SQLException ex) {
            statusPanel.setError("Failed to load branches: " + ex.getMessage());
        }
    }

    private void onAdd() {
        BranchDialog dialog = new BranchDialog(this, "Add Branch", null);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(branch -> {
            try {
                branchService.createBranch(branch, "ui");
                statusPanel.setSuccess("Branch created.");
                loadBranches();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Create failed: " + ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Branch selected = getSelectedBranch();
        if (selected == null) {
            statusPanel.setInfo("Select a branch to edit.");
            return;
        }
        BranchDialog dialog = new BranchDialog(this, "Edit Branch", selected);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(branch -> {
            branch.setBranchId(selected.getBranchId());
            branch.setBranchStatus(selected.getBranchStatus());
            try {
                branchService.updateBranch(branch, "ui");
                statusPanel.setSuccess("Branch updated.");
                loadBranches();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Update failed: " + ex.getMessage());
            }
        });
    }

    private void onToggle() {
        Branch selected = getSelectedBranch();
        if (selected == null) {
            statusPanel.setInfo("Select a branch to activate/deactivate.");
            return;
        }
        boolean newState = !"active".equalsIgnoreCase(selected.getBranchStatus());
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you sure you want to %s branch %s?",
                        newState ? "activate" : "deactivate",
                        selected.getBranchName()),
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            branchService.setBranchActive(selected.getBranchId(), newState, "ui");
            statusPanel.setSuccess("Branch status updated.");
            loadBranches();
        } catch (SQLException | ValidationException ex) {
            statusPanel.setError("Status change failed: " + ex.getMessage());
        }
    }

    private Branch getSelectedBranch() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getBranchAt(modelRow);
    }

    private static class BranchTableModel extends AbstractTableModel {
        private final List<Branch> rows = new ArrayList<>();
        private final String[] columns = {
                "Name", "City", "Address", "Contact", "Phone", "Status", "Updated", "Updated By"
        };
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void setRows(List<Branch> branches) {
            rows.clear();
            rows.addAll(branches);
            fireTableDataChanged();
        }

        public Branch getBranchAt(int row) {
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
            Branch b = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> b.getBranchName();
                case 1 -> b.getCity();
                case 2 -> b.getAddress();
                case 3 -> b.getContactPerson();
                case 4 -> b.getPhone();
                case 5 -> b.getBranchStatus();
                case 6 -> b.getUpdatedAt() != null ? formatter.format(b.getUpdatedAt()) : "";
                case 7 -> b.getUpdatedBy();
                default -> "";
            };
        }
    }

    private static class BranchDialog extends JDialog {
        private final JTextField nameField = new JTextField(25);
        private final JTextField cityField = new JTextField(20);
        private final JTextField addressField = new JTextField(30);
        private final JTextField contactField = new JTextField(20);
        private final JTextField phoneField = new JTextField(15);
        private Optional<Branch> result = Optional.empty();

        public BranchDialog(Frame owner, String title, Branch existing) {
            super(owner, title, true);
            initLayout();
            if (existing != null) {
                nameField.setText(existing.getBranchName());
                cityField.setText(existing.getCity());
                addressField.setText(existing.getAddress());
                contactField.setText(existing.getContactPerson());
                phoneField.setText(existing.getPhone());
            }
            pack();
            setLocationRelativeTo(owner);
        }

        private void initLayout() {
            setLayout(new BorderLayout());
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(new EmptyBorder(12, 12, 12, 12));
            form.add(new JLabel("Branch Name"));
            form.add(nameField);
            form.add(new JLabel("City"));
            form.add(cityField);
            form.add(new JLabel("Full Address"));
            form.add(addressField);
            form.add(new JLabel("Contact Person"));
            form.add(contactField);
            form.add(new JLabel("Phone"));
            form.add(phoneField);
            add(form, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton save = new JButton("Save");
            save.addActionListener(e -> onSave());
            JButton cancel = new JButton("Cancel");
            cancel.addActionListener(e -> dispose());
            buttons.add(save);
            buttons.add(cancel);
            add(buttons, BorderLayout.SOUTH);
        }

        private void onSave() {
            Branch branch = new Branch();
            branch.setBranchName(nameField.getText());
            branch.setCity(cityField.getText());
            branch.setAddress(addressField.getText());
            branch.setContactPerson(contactField.getText());
            branch.setPhone(phoneField.getText());
            branch.setBranchStatus("active");
            result = Optional.of(branch);
            dispose();
        }

        public Optional<Branch> getResult() {
            return result;
        }
    }
}

