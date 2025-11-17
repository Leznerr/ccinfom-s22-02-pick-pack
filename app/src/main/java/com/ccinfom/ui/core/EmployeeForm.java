package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.EmployeeDaoImpl;
import com.ccinfom.model.Employee;
import com.ccinfom.service.EmployeeService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.EmployeeServiceImpl;
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

public class EmployeeForm extends JFrame {

    private final EmployeeService employeeService = new EmployeeServiceImpl(new EmployeeDaoImpl());
    private final EmployeeTableModel tableModel = new EmployeeTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    public EmployeeForm() {
        super("Employees – Core Record Management");
        initLayout();
        loadEmployees();
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
        apply.addActionListener(e -> loadEmployees());
        top.add(apply);
        showInactive.addActionListener(e -> loadEmployees());
        top.add(showInactive);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Employee");
        addBtn.addActionListener(e -> onAdd());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> onEdit());
        JButton toggleBtn = new JButton("Toggle Active");
        toggleBtn.addActionListener(e -> onToggle());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadEmployees());
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

    private void loadEmployees() {
        try {
            List<Employee> employees = employeeService.listEmployees(searchField.getText(), showInactive.isSelected());
            tableModel.setRows(employees);
            statusPanel.setSuccess(String.format("Loaded %d employee(s).", employees.size()));
        } catch (SQLException ex) {
            statusPanel.setError("Failed to load employees: " + ex.getMessage());
        }
    }

    private void onAdd() {
        EmployeeDialog dialog = new EmployeeDialog(this, "Add Employee", null);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(employee -> {
            try {
                employeeService.createEmployee(employee, "ui");
                statusPanel.setSuccess("Employee created.");
                loadEmployees();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Create failed: " + ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Employee selected = getSelectedEmployee();
        if (selected == null) {
            statusPanel.setInfo("Select an employee to edit.");
            return;
        }
        EmployeeDialog dialog = new EmployeeDialog(this, "Edit Employee", selected);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(employee -> {
            employee.setEmployeeId(selected.getEmployeeId());
            employee.setEmployeeStatus(selected.getEmployeeStatus());
            try {
                employeeService.updateEmployee(employee, "ui");
                statusPanel.setSuccess("Employee updated.");
                loadEmployees();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Update failed: " + ex.getMessage());
            }
        });
    }

    private void onToggle() {
        Employee selected = getSelectedEmployee();
        if (selected == null) {
            statusPanel.setInfo("Select an employee to activate/deactivate.");
            return;
        }
        boolean newState = selected.getEmployeeStatus() == Employee.Status.INACTIVE;
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you sure you want to %s employee %s %s?",
                        newState ? "activate" : "deactivate",
                        selected.getFirstName(), selected.getLastName()),
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            employeeService.setEmployeeActive(selected.getEmployeeId(), newState, "ui");
            statusPanel.setSuccess("Employee status updated.");
            loadEmployees();
        } catch (SQLException | ValidationException ex) {
            statusPanel.setError("Status change failed: " + ex.getMessage());
        }
    }

    private Employee getSelectedEmployee() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getEmployeeAt(modelRow);
    }

    private static class EmployeeTableModel extends AbstractTableModel {
        private final List<Employee> rows = new ArrayList<>();
        private final String[] columns = {
                "First Name", "Last Name", "Role", "Phone", "Email",
                "Status", "Updated", "Updated By"
        };
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void setRows(List<Employee> employees) {
            rows.clear();
            rows.addAll(employees);
            fireTableDataChanged();
        }

        public Employee getEmployeeAt(int row) {
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
            Employee e = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> e.getFirstName();
                case 1 -> e.getLastName();
                case 2 -> e.getEmployeeRole().name();
                case 3 -> e.getPhone();
                case 4 -> e.getEmail();
                case 5 -> e.getEmployeeStatus().name();
                case 6 -> e.getUpdatedAt() != null ? formatter.format(e.getUpdatedAt()) : "";
                case 7 -> e.getUpdatedBy();
                default -> "";
            };
        }
    }

    private static class EmployeeDialog extends JDialog {
        private final JTextField firstNameField = new JTextField(20);
        private final JTextField lastNameField = new JTextField(20);
        private final JComboBox<Employee.Role> roleCombo = new JComboBox<>(Employee.Role.values());
        private final JTextField phoneField = new JTextField(15);
        private final JTextField emailField = new JTextField(25);
        private Optional<Employee> result = Optional.empty();

        public EmployeeDialog(Frame owner, String title, Employee existing) {
            super(owner, title, true);
            initLayout();
            if (existing != null) {
                firstNameField.setText(existing.getFirstName());
                lastNameField.setText(existing.getLastName());
                roleCombo.setSelectedItem(existing.getEmployeeRole());
                phoneField.setText(existing.getPhone());
                emailField.setText(existing.getEmail());
            }
            pack();
            setLocationRelativeTo(owner);
        }

        private void initLayout() {
            setLayout(new BorderLayout());
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(new EmptyBorder(12, 12, 12, 12));
            form.add(new JLabel("First Name"));
            form.add(firstNameField);
            form.add(new JLabel("Last Name"));
            form.add(lastNameField);
            form.add(new JLabel("Role"));
            form.add(roleCombo);
            form.add(new JLabel("Phone"));
            form.add(phoneField);
            form.add(new JLabel("Email"));
            form.add(emailField);
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
            Employee employee = new Employee();
            employee.setFirstName(firstNameField.getText());
            employee.setLastName(lastNameField.getText());
            employee.setEmployeeRole((Employee.Role) roleCombo.getSelectedItem());
            employee.setPhone(phoneField.getText());
            employee.setEmail(emailField.getText());
            employee.setEmployeeStatus(Employee.Status.ACTIVE);
            result = Optional.of(employee);
            dispose();
        }

        public Optional<Employee> getResult() {
            return result;
        }
    }
}

