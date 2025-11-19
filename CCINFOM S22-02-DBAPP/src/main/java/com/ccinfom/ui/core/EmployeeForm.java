package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.EmployeeDaoImpl;
import com.ccinfom.model.Employee;
import com.ccinfom.service.EmployeeService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.EmployeeServiceImpl;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.config.DbConnection;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
        super("Employees - Core Record Management");
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
        JButton toggleBtn = new JButton("Delete");
        toggleBtn.addActionListener(e -> onToggle());
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(e -> onViewDetails());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadEmployees());
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        tint(addBtn, new Color(46, 160, 67));    // create = green
        tint(editBtn, new Color(10, 132, 255));  // update = blue
        tint(toggleBtn, new Color(219, 68, 55)); // delete = red
        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(toggleBtn);
        actions.add(viewDetailsBtn);
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

    /**
     * Show employee profile plus picks/packs/dispatches they handled.
     */
    private void onViewDetails() {
        Employee selected = getSelectedEmployee();
        if (selected == null) {
            statusPanel.setInfo("Select an employee to view details.");
            return;
        }

        JDialog dlg = new JDialog(this, "Employee Details - " + selected.getFirstName() + " " + selected.getLastName(), true);
        dlg.setLayout(new BorderLayout(8, 8));

        JPanel details = new JPanel(new GridLayout(0, 2, 6, 6));
        details.setBorder(new EmptyBorder(10, 10, 10, 10));
        details.add(new JLabel("Name:")); details.add(new JLabel(selected.getFirstName() + " " + selected.getLastName()));
        details.add(new JLabel("Role:")); details.add(new JLabel(selected.getEmployeeRole().name()));
        details.add(new JLabel("Phone:")); details.add(new JLabel(selected.getPhone()));
        details.add(new JLabel("Email:")); details.add(new JLabel(selected.getEmail()));
        details.add(new JLabel("Status:")); details.add(new JLabel(selected.getEmployeeStatus().name()));
        details.add(new JLabel("Updated:")); details.add(new JLabel(selected.getUpdatedAt() != null ? selected.getUpdatedAt().toString() : ""));
        details.add(new JLabel("Updated By:")); details.add(new JLabel(selected.getUpdatedBy()));

        Integer days = askDaysBack();
        JTable pickingTable = buildPickingTable(selected.getEmployeeId(), days);
        JTable packingTable = buildPackingTable(selected.getEmployeeId(), days);
        JTable dispatchTable = buildDispatchTable(selected.getEmployeeId(), days);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Details", new JScrollPane(details));
        tabs.addTab("Picking", new JScrollPane(pickingTable));
        tabs.addTab("Packing", new JScrollPane(packingTable));
        tabs.addTab("Dispatches (as Driver)", new JScrollPane(dispatchTable));

        dlg.add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dlg.add(south, BorderLayout.SOUTH);

        dlg.setSize(750, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JTable buildPickingTable(long employeeId, Integer daysBack) {
        String[] headers = {"Picking ID", "Ticket ID", "Started", "Completed", "Status"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = """
            SELECT picking_id, pick_ticket_id, started_at, completed_at, picking_status
            FROM picking_hdr
            WHERE picker_employee_id = ?
            %s
            ORDER BY started_at DESC
            LIMIT 20
        """.formatted(dateFilterClause("started_at", daysBack));
        fillTable(model, sql, employeeId);
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"No data for selected record."});
        }
        JTable tbl = new JTable(model);
        tbl.setAutoCreateRowSorter(true);
        return tbl;
    }

    private JTable buildPackingTable(long employeeId, Integer daysBack) {
        String[] headers = {"Box ID", "Ticket ID", "Created", "Sealed", "Status"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = """
            SELECT box_id, pick_ticket_id, created_at, sealed_at,
                   CASE WHEN sealed_flag = TRUE THEN 'Sealed' ELSE 'Open' END AS status
            FROM pack_box_hdr
            WHERE created_by = ?
            %s
            ORDER BY created_at DESC
            LIMIT 20
        """.formatted(dateFilterClause("created_at", daysBack));
        fillTable(model, sql, employeeId);
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"No data for selected record."});
        }
        JTable tbl = new JTable(model);
        tbl.setAutoCreateRowSorter(true);
        return tbl;
    }

    private JTable buildDispatchTable(long employeeId, Integer daysBack) {
        String[] headers = {"Dispatch ID", "Manifest", "Ticket ID", "Depart", "Arrive", "Status"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        String sql = """
            SELECT dispatch_id, manifest_no, pick_ticket_id, depart_ts, arrive_ts, dispatch_status
            FROM dispatch_hdr
            WHERE driver_id = ?
            %s
            ORDER BY created_at DESC
            LIMIT 20
        """.formatted(dateFilterClause("created_at", daysBack));
        fillTable(model, sql, employeeId);
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"No data for selected record."});
        }
        JTable tbl = new JTable(model);
        tbl.setAutoCreateRowSorter(true);
        return tbl;
    }

    /**
     * Ask user how many days back to include; null means no filter.
     */
    private Integer askDaysBack() {
        String val = JOptionPane.showInputDialog(this,
                "Show activity for how many days back? (blank = all)", "30");
        if (val == null || val.isBlank()) return null;
        try {
            int days = Integer.parseInt(val.trim());
            return days > 0 ? days : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String dateFilterClause(String column, Integer daysBack) {
        if (daysBack == null) return "";
        return " AND " + column + " >= DATE_SUB(NOW(), INTERVAL " + daysBack + " DAY) ";
    }

    private void fillTable(DefaultTableModel model, String sql, long employeeId) {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                int cols = model.getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 0; i < cols; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    model.addRow(row);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load related data: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
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
            tint(save, new Color(10, 132, 255)); // save/update = blue
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

    private static void tint(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
    }
}
