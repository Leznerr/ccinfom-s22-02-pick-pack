package com.ccinfom.ui.core;

import com.ccinfom.dao.impl.CustomerDaoImpl;
import com.ccinfom.model.Customer;
import com.ccinfom.service.CustomerService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.CustomerServiceImpl;
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
import java.util.Optional;

/**
 * Swing form to manage customer records.
 */
public class CustomerForm extends JFrame {

    private final CustomerService customerService = new CustomerServiceImpl(new CustomerDaoImpl());
    private final CustomerTableModel tableModel = new CustomerTableModel();
    private final JTable table = new JTable(tableModel);
    private final JTextField searchField = new JTextField(20);
    private final JCheckBox showInactive = new JCheckBox("Show inactive");
    private final StatusPanel statusPanel = new StatusPanel("Status: Ready");

    public CustomerForm() {
        super("Customers - Core Record Management");
        initLayout();
        loadCustomers();
    }

    private void initLayout() {
        setSize(980, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBorder(new EmptyBorder(8, 8, 0, 8));
        top.add(new JLabel("Search:"));
        top.add(searchField);
        JButton searchBtn = new JButton("Apply");
        searchBtn.addActionListener(e -> loadCustomers());
        top.add(searchBtn);
        showInactive.addActionListener(e -> loadCustomers());
        top.add(showInactive);
        add(top, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Customer");
        addBtn.addActionListener(e -> onAdd());
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> onEdit());
        JButton toggleBtn = new JButton("Delete");
        toggleBtn.addActionListener(e -> onToggle());
        JButton viewDetailsBtn = new JButton("View Details");
        viewDetailsBtn.addActionListener(e -> onViewDetails());
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadCustomers());
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

    private void loadCustomers() {
        try {
            List<Customer> customers = customerService.listCustomers(searchField.getText(), showInactive.isSelected());
            tableModel.setRows(customers);
            statusPanel.setSuccess(String.format("Loaded %d customer(s).", customers.size()));
        } catch (SQLException ex) {
            statusPanel.setError("Failed to load customers: " + ex.getMessage());
        }
    }

    private void onAdd() {
        CustomerDialog dialog = new CustomerDialog(this, "Add Customer", null);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(customer -> {
            try {
                customerService.createCustomer(customer, "ui");
                statusPanel.setSuccess("Customer created.");
                loadCustomers();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Create failed: " + ex.getMessage());
            }
        });
    }

    private void onEdit() {
        Customer selected = getSelectedCustomer();
        if (selected == null) {
            statusPanel.setInfo("Select a customer to edit.");
            return;
        }
        CustomerDialog dialog = new CustomerDialog(this, "Edit Customer", selected);
        dialog.setVisible(true);
        dialog.getResult().ifPresent(customer -> {
            customer.setCustomerId(selected.getCustomerId());
            customer.setCustomerStatus(selected.getCustomerStatus());
            try {
                customerService.updateCustomer(customer, "ui");
                statusPanel.setSuccess("Customer updated.");
                loadCustomers();
            } catch (SQLException | ValidationException ex) {
                statusPanel.setError("Update failed: " + ex.getMessage());
            }
        });
    }

    private void onToggle() {
        Customer selected = getSelectedCustomer();
        if (selected == null) {
            statusPanel.setInfo("Select a customer to activate/deactivate.");
            return;
        }
        boolean newState = !"active".equalsIgnoreCase(selected.getCustomerStatus());
        int confirm = JOptionPane.showConfirmDialog(
                this,
                String.format("Are you sure you want to %s customer %s?",
                        newState ? "activate" : "deactivate",
                        selected.getCustomerName()),
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            customerService.setCustomerActive(selected.getCustomerId(), newState, "ui");
            statusPanel.setSuccess("Customer status updated.");
            loadCustomers();
        } catch (SQLException | ValidationException ex) {
            statusPanel.setError("Status change failed: " + ex.getMessage());
        }
    }

    /**
     * Show customer details plus related pick tickets and dispatches.
     */
    private void onViewDetails() {
        Customer selected = getSelectedCustomer();
        if (selected == null) {
            statusPanel.setInfo("Select a customer to view details.");
            return;
        }

        JDialog dlg = new JDialog(this, "Customer Details - " + selected.getCustomerName(), true);
        dlg.setLayout(new BorderLayout(8, 8));

        JPanel details = new JPanel(new GridLayout(0, 2, 6, 6));
        details.setBorder(new EmptyBorder(10, 10, 10, 10));
        details.add(new JLabel("Name:")); details.add(new JLabel(selected.getCustomerName()));
        details.add(new JLabel("Contact:")); details.add(new JLabel(selected.getContactPerson()));
        details.add(new JLabel("Phone:")); details.add(new JLabel(selected.getPhone()));
        details.add(new JLabel("Email:")); details.add(new JLabel(selected.getEmail()));
        details.add(new JLabel("Address:")); details.add(new JLabel(selected.getDefaultDeliveryAddress()));
        details.add(new JLabel("Status:")); details.add(new JLabel(selected.getCustomerStatus()));
        details.add(new JLabel("Updated:")); details.add(new JLabel(selected.getUpdatedAt() != null ? selected.getUpdatedAt().toString() : ""));
        details.add(new JLabel("Updated By:")); details.add(new JLabel(selected.getUpdatedBy()));

        JTable ticketTable = buildRelatedTable(
                "SELECT pick_ticket_id AS ticket_id, ticket_status, created_at, updated_at " +
                        "FROM pick_ticket_hdr WHERE customer_id = ? ORDER BY created_at DESC LIMIT 15",
                selected.getCustomerId(),
                new String[]{"Ticket ID", "Status", "Created", "Updated"}
        );

        JTable dispatchTable = buildRelatedTable(
                "SELECT dh.dispatch_id, dh.manifest_no, dh.depart_ts AS dispatched_at, dh.depart_ts, dh.arrive_ts, dh.dispatch_status " +
                        "FROM dispatch_hdr dh " +
                        "JOIN pick_ticket_hdr pth ON pth.pick_ticket_id = dh.pick_ticket_id " +
                        "WHERE pth.customer_id = ? ORDER BY dh.created_at DESC LIMIT 15",
                selected.getCustomerId(),
                new String[]{"Dispatch ID", "Manifest", "Built", "Depart", "Arrive", "Status"}
        );

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Details", new JScrollPane(details));
        tabs.addTab("Pick Tickets", new JScrollPane(ticketTable));
        tabs.addTab("Dispatches", new JScrollPane(dispatchTable));

        dlg.add(tabs, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        dlg.add(south, BorderLayout.SOUTH);

        dlg.setSize(700, 400);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    /**
     * Helper to build a non-editable table from a query filtered by customer_id.
     */
    private JTable buildRelatedTable(String sql, long customerId, String[] headers) {
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    model.addRow(row);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load related data: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"No data for selected record."});
        }
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private Customer getSelectedCustomer() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        return tableModel.getCustomerAt(modelRow);
    }

    private static class CustomerTableModel extends AbstractTableModel {
        private final List<Customer> rows = new ArrayList<>();
        private final String[] columns = {
                "Name", "Contact", "Phone", "Email", "Address",
                "Status", "Updated", "Updated By"
        };
        private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        public void setRows(List<Customer> customers) {
            rows.clear();
            rows.addAll(customers);
            fireTableDataChanged();
        }

        public Customer getCustomerAt(int row) {
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
            Customer c = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> c.getCustomerName();
                case 1 -> c.getContactPerson();
                case 2 -> c.getPhone();
                case 3 -> c.getEmail();
                case 4 -> c.getDefaultDeliveryAddress();
                case 5 -> c.getCustomerStatus();
                case 6 -> c.getUpdatedAt() != null ? formatter.format(c.getUpdatedAt()) : "";
                case 7 -> c.getUpdatedBy();
                default -> "";
            };
        }
    }

    private static class CustomerDialog extends JDialog {
        private final JTextField nameField = new JTextField(25);
        private final JTextField contactField = new JTextField(20);
        private final JTextField phoneField = new JTextField(15);
        private final JTextField emailField = new JTextField(25);
        private final JTextField addressField = new JTextField(30);
        private Optional<Customer> result = Optional.empty();

        public CustomerDialog(Frame owner, String title, Customer existing) {
            super(owner, title, true);
            initLayout();
            if (existing != null) {
                nameField.setText(existing.getCustomerName());
                contactField.setText(existing.getContactPerson());
                phoneField.setText(existing.getPhone());
                emailField.setText(existing.getEmail());
                addressField.setText(existing.getDefaultDeliveryAddress());
            }
            pack();
            setLocationRelativeTo(owner);
        }

        private void initLayout() {
            setLayout(new BorderLayout());
            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.setBorder(new EmptyBorder(12, 12, 12, 12));
            form.add(new JLabel("Customer Name"));
            form.add(nameField);
            form.add(new JLabel("Contact Person"));
            form.add(contactField);
            form.add(new JLabel("Phone"));
            form.add(phoneField);
            form.add(new JLabel("Email"));
            form.add(emailField);
            form.add(new JLabel("Default Address"));
            form.add(addressField);
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
            Customer customer = new Customer();
            customer.setCustomerName(nameField.getText());
            customer.setContactPerson(contactField.getText());
            customer.setPhone(phoneField.getText());
            customer.setEmail(emailField.getText());
            customer.setDefaultDeliveryAddress(addressField.getText());
            customer.setCustomerStatus("active");
            result = Optional.of(customer);
            dispose();
        }

        public Optional<Customer> getResult() {
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
