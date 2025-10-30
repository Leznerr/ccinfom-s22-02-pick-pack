package com.ccinfom.ui.t1;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.model.Branch;
import com.ccinfom.model.Customer;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.model.Product;
import com.ccinfom.service.TicketService;
import com.ccinfom.service.ValidationException;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

/**
 * Phase D UI form for Transaction T1 - Create Pick Ticket.
 *
 * <p>This class delivers the Member 4 responsibilities for the T1 Swing shell:
 * wiring the UI to the service layer, loading dropdown values from lookup DAO,
 * and pushing validated ticket lines to {@link TicketService}.</p>
 */
public class TicketForm extends JFrame {

    private static final String DEFAULT_UPDATED_BY =
        System.getProperty("user.name", "ui-operator");

    private final TicketService ticketService;
    private final LookupDao lookupDao;

    // UI Components
    private JComboBox<ComboItem<Customer>> customerComboBox;
    private JComboBox<ComboItem<Branch>> branchComboBox;
    private JTextArea remarksArea;
    private JTextField productIdField;
    private JTextField quantityField;
    private JButton addLineButton;
    private JButton removeLineButton;
    private JTableWithModel lineTable;
    private JButton createTicketButton;

    /**
     * Default constructor used by the application. Instantiates the concrete DAO
     * implementations, wires the service, and initializes the form components.
     */
    public TicketForm() {
        this(new LookupDaoImpl(), new TicketDaoImpl());
    }

    private TicketForm(LookupDao lookupDao, TicketDao ticketDao) {
        this(new TicketService(ticketDao, lookupDao), lookupDao);
    }

    /**
     * Visible for testing / alternate wiring.
     *
     * @param ticketService pre-configured ticket service
     * @param lookupDao     lookup DAO for dropdown data
     */
    TicketForm(TicketService ticketService, LookupDao lookupDao) {
        super("T1: Create Pick Ticket");
        this.ticketService = Objects.requireNonNull(ticketService, "ticketService");
        this.lookupDao = Objects.requireNonNull(lookupDao, "lookupDao");

        initializeComponents();
        layoutComponents();
        registerEventHandlers();
        loadDropdownData();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
    }

    private void initializeComponents() {
        customerComboBox = new JComboBox<>();
        branchComboBox = new JComboBox<>();
        remarksArea = new JTextArea(3, 30);
        remarksArea.setLineWrap(true);
        remarksArea.setWrapStyleWord(true);

        productIdField = new JTextField(10);
        quantityField = new JTextField(8);
        addLineButton = new JButton("Add Line");
        removeLineButton = new JButton("Remove Selected");

        lineTable = new JTableWithModel();
        lineTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lineTable.setFillsViewportHeight(true);

        createTicketButton = new JButton("Create Ticket");
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setBorder(BorderFactory.createTitledBorder("Ticket Details"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        headerPanel.add(new JLabel("Customer"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        headerPanel.add(customerComboBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        headerPanel.add(new JLabel("Branch"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        headerPanel.add(branchComboBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        headerPanel.add(new JLabel("Remarks"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        remarksArea.setRows(3);
        remarksArea.setColumns(30);
        headerPanel.add(new JScrollPane(remarksArea), gbc);
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy++;
        gbc.gridx = 0;
        headerPanel.add(new JLabel("Product ID"), gbc);
        gbc.gridx = 1;
        JPanel productPanel = new JPanel(new GridBagLayout());
        GridBagConstraints productConstraints = new GridBagConstraints();
        productConstraints.insets = new Insets(0, 0, 0, 4);
        productConstraints.gridx = 0;
        productPanel.add(productIdField, productConstraints);
        productConstraints.gridx++;
        productPanel.add(new JLabel("Quantity"), productConstraints);
        productConstraints.gridx++;
        productPanel.add(quantityField, productConstraints);
        productConstraints.gridx++;
        productPanel.add(addLineButton, productConstraints);
        productConstraints.gridx++;
        productPanel.add(removeLineButton, productConstraints);
        headerPanel.add(productPanel, gbc);

    add(headerPanel, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(lineTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Requested Lines"));
        add(tableScroll, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel();
        footerPanel.add(createTicketButton);
        add(footerPanel, BorderLayout.SOUTH);
    }

    private void registerEventHandlers() {
        addLineButton.addActionListener(evt -> handleAddLine());
        removeLineButton.addActionListener(evt -> handleRemoveSelected());
        createTicketButton.addActionListener(evt -> handleCreateTicket());
    }

    private void loadDropdownData() {
        customerComboBox.setEnabled(false);
        branchComboBox.setEnabled(false);
        new SwingWorker<Void, Void>() {
            private List<Customer> customers;
            private List<Branch> branches;

            @Override
            protected Void doInBackground() {
                customers = lookupDao.listCustomers();
                branches = lookupDao.listBranches();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // surface background exceptions if any
                    DefaultComboBoxModel<ComboItem<Customer>> custModel = new DefaultComboBoxModel<>();
                    for (Customer customer : customers) {
                        custModel.addElement(new ComboItem<>(customer, formatCustomerLabel(customer)));
                    }
                    customerComboBox.setModel(custModel);

                    DefaultComboBoxModel<ComboItem<Branch>> branchModel = new DefaultComboBoxModel<>();
                    for (Branch branch : branches) {
                        branchModel.addElement(new ComboItem<>(branch, formatBranchLabel(branch)));
                    }
                    branchComboBox.setModel(branchModel);

                    if (custModel.getSize() > 0) {
                        customerComboBox.setSelectedIndex(0);
                    }
                    if (branchModel.getSize() > 0) {
                        branchComboBox.setSelectedIndex(0);
                    }

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    showError("Loading interrupted. Please reopen the form.");
                } catch (ExecutionException ee) {
                    showError("Failed to load lookup data: " + ee.getCause().getMessage());
                } finally {
                    customerComboBox.setEnabled(true);
                    branchComboBox.setEnabled(true);
                }
            }
        }.execute();
    }

    private void handleAddLine() {
        String productText = productIdField.getText().trim();
        String qtyText = quantityField.getText().trim();

        if (productText.isEmpty() || qtyText.isEmpty()) {
            showWarning("Product ID and Quantity are required.");
            return;
        }

        long productId;
        BigDecimal qty;
        try {
            productId = Long.parseLong(productText);
        } catch (NumberFormatException nfe) {
            showWarning("Product ID must be a whole number.");
            return;
        }

        try {
            qty = new BigDecimal(qtyText);
        } catch (NumberFormatException nfe) {
            showWarning("Quantity must be a numeric value.");
            return;
        }

        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            showWarning("Quantity must be greater than zero.");
            return;
        }

        if (lineTable.getModel().containsProduct(productId)) {
            showWarning("Product already added to this ticket.");
            return;
        }

        Product product;
        try {
            product = lookupDao.findProductById(productId);
        } catch (SQLException e) {
            showError("Unable to look up product: " + e.getMessage());
            return;
        }

        if (product == null) {
            showWarning("Product ID " + productId + " does not exist.");
            return;
        }

        if (!product.isActiveFlag()) {
            showWarning("Product ID " + productId + " is inactive.");
            return;
        }

        lineTable.getModel().addLine(new LineEntry(
            product.getProductId(),
            product.getSku(),
            product.getProductName(),
            product.getUnitOfMeasure(),
            qty
        ));

        productIdField.setText("");
        quantityField.setText("");
        productIdField.requestFocusInWindow();
    }

    private void handleRemoveSelected() {
        int selectedRow = lineTable.getSelectedRow();
        if (selectedRow == -1) {
            showWarning("Select a line to remove.");
            return;
        }
        lineTable.getModel().removeLine(selectedRow);
    }

    private void handleCreateTicket() {
        ComboItem<Customer> customerItem = (ComboItem<Customer>) customerComboBox.getSelectedItem();
        ComboItem<Branch> branchItem = (ComboItem<Branch>) branchComboBox.getSelectedItem();

        if (customerItem == null) {
            showWarning("Select a customer.");
            return;
        }
        if (branchItem == null) {
            showWarning("Select a branch.");
            return;
        }
        if (lineTable.getModel().isEmpty()) {
            showWarning("Add at least one product line.");
            return;
        }

        PickTicketHdr hdr = new PickTicketHdr();
        hdr.setCustomerId(customerItem.getValue().getCustomerId());
        hdr.setBranchId(branchItem.getValue().getBranchId());
        hdr.setRemarks(remarksArea.getText().isBlank() ? null : remarksArea.getText().trim());
        hdr.setUpdatedBy(DEFAULT_UPDATED_BY);

        List<PickTicketLine> lines = new ArrayList<>();
        for (LineEntry entry : lineTable.getModel().getEntries()) {
            PickTicketLine line = new PickTicketLine();
            line.setProductId(entry.getProductId());
            line.setRequestedQty(entry.getQuantity());
            line.setUom(entry.getUom());
            line.setUpdatedBy(hdr.getUpdatedBy());
            lines.add(line);
        }

        createTicketButton.setEnabled(false);
        new SwingWorker<Long, Void>() {
            private ValidationException validationError;
            private SQLException sqlError;

            @Override
            protected Long doInBackground() {
                try {
                    return ticketService.createPickTicket(hdr, lines);
                } catch (ValidationException ve) {
                    validationError = ve;
                } catch (SQLException se) {
                    sqlError = se;
                }
                return null;
            }

            @Override
            protected void done() {
                createTicketButton.setEnabled(true);
                if (validationError != null) {
                    showWarning(validationError.getMessage());
                    return;
                }
                if (sqlError != null) {
                    showError("Failed to create ticket: " + sqlError.getMessage());
                    return;
                }
                try {
                    Long newTicketId = get();
                    if (newTicketId != null) {
                        JOptionPane.showMessageDialog(
                            TicketForm.this,
                            "Ticket #" + newTicketId + " created successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        clearForm();
                    } else {
                        showError("Ticket creation failed. Please retry.");
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    showError("Ticket creation interrupted.");
                } catch (ExecutionException ee) {
                    showError("Unexpected error: " + ee.getCause().getMessage());
                }
            }
        }.execute();
    }

    private void clearForm() {
        productIdField.setText("");
        quantityField.setText("");
        remarksArea.setText("");
        lineTable.getModel().clear();
        if (customerComboBox.getItemCount() > 0) {
            customerComboBox.setSelectedIndex(0);
        }
        if (branchComboBox.getItemCount() > 0) {
            branchComboBox.setSelectedIndex(0);
        }
    }

    private String formatCustomerLabel(Customer customer) {
        return customer.getCustomerName() + " (ID: " + customer.getCustomerId() + ")";
    }

    private String formatBranchLabel(Branch branch) {
        String city = branch.getCity() != null ? branch.getCity() : "";
        return branch.getBranchName() + (city.isBlank() ? "" : " - " + city) +
               " (ID: " + branch.getBranchId() + ")";
    }

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Convenience helper to start the form in isolation.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TicketForm form = new TicketForm();
            form.setVisible(true);
        });
    }

    /**
     * Type-safe wrapper for combo box entries holding both display text and the
     * source model object.
     *
     * @param <T> domain model type
     */
    public static final class ComboItem<T> {
        private final T value;
        private final String label;

        ComboItem(T value, String label) {
            this.value = value;
            this.label = label;
        }

        T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Lightweight holder of ticket line data for the table model.
     */
    private static final class LineEntry {
        private final long productId;
        private final String sku;
        private final String productName;
        private final String uom;
        private final BigDecimal quantity;

        LineEntry(long productId, String sku, String productName, String uom, BigDecimal quantity) {
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.uom = uom;
            this.quantity = quantity;
        }

        long getProductId() {
            return productId;
        }

        String getSku() {
            return sku;
        }

        String getProductName() {
            return productName;
        }

        String getUom() {
            return uom;
        }

        BigDecimal getQuantity() {
            return quantity;
        }
    }

    /**
     * Table component bound to the {@link LineTableModel}. Keeping an explicit
     * subclass helps avoid casting noise across the form logic.
     */
    private static final class JTableWithModel extends JTable {
        private final LineTableModel model;

        JTableWithModel() {
            this.model = new LineTableModel();
            setModel(model);
        }

        @Override
        public LineTableModel getModel() {
            return model;
        }
    }

    /**
     * Custom table model managing the product lines staged for ticket creation.
     */
    public static final class LineTableModel extends AbstractTableModel {

        private static final String[] COLUMN_NAMES = {
            "Product ID", "SKU", "Product Name", "Requested Qty", "UOM"
        };

        private final List<LineEntry> entries = new ArrayList<>();

        @Override
        public int getRowCount() {
            return entries.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return Long.class;
                case 3:
                    return BigDecimal.class;
                default:
                    return String.class;
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LineEntry entry = entries.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return entry.getProductId();
                case 1:
                    return entry.getSku();
                case 2:
                    return entry.getProductName();
                case 3:
                    return entry.getQuantity();
                case 4:
                    return entry.getUom();
                default:
                    return "";
            }
        }

        void addLine(LineEntry entry) {
            entries.add(entry);
            int newRow = entries.size() - 1;
            fireTableRowsInserted(newRow, newRow);
        }

        void removeLine(int rowIndex) {
            if (rowIndex >= 0 && rowIndex < entries.size()) {
                entries.remove(rowIndex);
                fireTableRowsDeleted(rowIndex, rowIndex);
            }
        }

        boolean containsProduct(long productId) {
            return entries.stream().anyMatch(entry -> entry.getProductId() == productId);
        }

        boolean isEmpty() {
            return entries.isEmpty();
        }

        List<LineEntry> getEntries() {
            return new ArrayList<>(entries);
        }

        void clear() {
            if (entries.isEmpty()) {
                return;
            }
            int lastIndex = entries.size() - 1;
            entries.clear();
            fireTableRowsDeleted(0, lastIndex);
        }
    }
}
