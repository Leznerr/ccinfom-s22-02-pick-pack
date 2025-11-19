package com.ccinfom.ui.t2;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.Branch;
import com.ccinfom.model.Customer;
import com.ccinfom.model.Employee;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;
import com.ccinfom.service.TicketService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.ui.common.ComboItem;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/**
 * Phase D Swing form for Transaction T2 (Allocate & Pick).
 * Mirrors the layering of TicketForm: UI -> Service -> DAO.
 */
public class PickingForm extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(PickingForm.class.getName());

    private static final String DEFAULT_UPDATED_BY =
        System.getProperty("user.name", "ui-operator");

    private final LookupDao lookupDao;
    private final TicketDao ticketDao;
    private final com.ccinfom.dao.interfaces.PickingDao pickingDao;
    private final TicketService ticketService;
    private final com.ccinfom.service.PickingService pickingService;

    private JComboBox<ComboItem<PickTicketHdr>> ticketComboBox;
    private JComboBox<ComboItem<Employee>> pickerComboBox;
    private JButton startPickingButton;
    private JButton saveResultsButton;
    private JLabel statusLabel;
    private PickingLineTableModel tableModel;
    private JTable linesTable;

    private PickTicketHdr currentTicket;
    private Long currentPickingId;

    public PickingForm() {
        this(new LookupDaoImpl(), new TicketDaoImpl(), new PickingDaoImpl());
    }

    PickingForm(LookupDao lookupDao,
                TicketDao ticketDao,
                com.ccinfom.dao.interfaces.PickingDao pickingDao) {
        super("T2 - Picking Process");
        this.lookupDao = Objects.requireNonNull(lookupDao, "lookupDao");
        this.ticketDao = Objects.requireNonNull(ticketDao, "ticketDao");
        this.pickingDao = Objects.requireNonNull(pickingDao, "pickingDao");
        this.ticketService = new TicketService(ticketDao, lookupDao);
        this.pickingService = new com.ccinfom.service.PickingService(pickingDao, ticketDao, lookupDao);

        initializeComponents();
        layoutComponents();
        registerListeners();
        loadPickersAndTickets();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
    }

    // -------------------------------------------------------------------------
    // Initialisation
    // -------------------------------------------------------------------------

    private void initializeComponents() {
        ticketComboBox = new JComboBox<>();
        pickerComboBox = new JComboBox<>();
        startPickingButton = new JButton("Start Picking");
        saveResultsButton = new JButton("Save Picked Qty");
        statusLabel = new JLabel("Status: Loading reference data...");

        tableModel = new PickingLineTableModel();
        linesTable = new JTable(tableModel);
        linesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        linesTable.setFillsViewportHeight(true);

        ticketComboBox.setEnabled(false);
        pickerComboBox.setEnabled(false);
        startPickingButton.setEnabled(false);
        saveResultsButton.setEnabled(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new GridBagLayout());
        header.setBorder(BorderFactory.createTitledBorder("Picking Session"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        header.add(new JLabel("Ticket"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        header.add(ticketComboBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.weightx = 0;
        header.add(new JLabel("Picker"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        header.add(pickerComboBox, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        header.add(statusLabel, gbc);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(startPickingButton);
        buttonPanel.add(saveResultsButton);

        JScrollPane tableScroll = new JScrollPane(linesTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Ticket Lines"));

        add(header, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void registerListeners() {
        ticketComboBox.addActionListener(e -> onTicketSelected());
        pickerComboBox.addActionListener(e -> {
            if (currentTicket != null && currentPickingId == null) {
                startPickingButton.setEnabled(pickerComboBox.getSelectedItem() != null);
            }
        });
        startPickingButton.addActionListener(e -> onStartPicking());
        saveResultsButton.addActionListener(e -> onSaveResults());
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadPickersAndTickets() {
        ticketComboBox.setEnabled(false);
        pickerComboBox.setEnabled(false);
        startPickingButton.setEnabled(false);
        saveResultsButton.setEnabled(false);
        statusLabel.setText("Status: Loading pickers and open tickets...");

        new SwingWorker<Void, Void>() {
            private List<Employee> pickers;
            private List<PickTicketHdr> tickets;
            private Map<Long, String> customerNames;
            private Map<Long, String> branchNames;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    pickers = lookupDao.listActivePickers();
                    tickets = ticketDao.listAllTickets();
                    customerNames = buildCustomerNameMap();
                    branchNames = buildBranchNameMap();
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load reference data: " + error.getMessage());
                    statusLabel.setText("Status: Failed to load reference data.");
                    return;
                }

                DefaultComboBoxModel<ComboItem<Employee>> pickerModel = new DefaultComboBoxModel<>();
                if (pickers != null) {
                    for (Employee emp : pickers) {
                        String display = emp.getLastName() + ", " + emp.getFirstName() + " (ID: " + emp.getEmployeeId() + ")";
                        pickerModel.addElement(new ComboItem<>(emp, display));
                    }
                }
                pickerComboBox.setModel(pickerModel);
                pickerComboBox.setEnabled(pickerModel.getSize() > 0);

                DefaultComboBoxModel<ComboItem<PickTicketHdr>> ticketModel = new DefaultComboBoxModel<>();
                if (tickets != null) {
                    for (PickTicketHdr ticket : tickets) {
                        if (ticket.getTicketStatus() == PickTicketHdr.TicketStatus.Open) {
                            String customerLabel = customerNames.getOrDefault(ticket.getCustomerId(), "Customer " + ticket.getCustomerId());
                            String branchLabel = branchNames.getOrDefault(ticket.getBranchId(), "Branch " + ticket.getBranchId());
                            String display = String.format("Ticket #%d - %s \u2192 %s",
                                    ticket.getPickTicketId(), customerLabel, branchLabel);
                            ticketModel.addElement(new ComboItem<>(ticket, display));
                        }
                    }
                }

                ticketComboBox.setModel(ticketModel);
                ticketComboBox.setEnabled(ticketModel.getSize() > 0);

                if (ticketModel.getSize() == 0) {
                    statusLabel.setText("Status: No open tickets available.");
                } else {
                    statusLabel.setText("Status: Select a ticket to begin.");
                }
            }
        }.execute();
    }

    private Map<Long, String> buildCustomerNameMap() {
        List<Customer> customers = lookupDao.listCustomers();
        Map<Long, String> map = new HashMap<>();
        for (Customer customer : customers) {
            map.put(customer.getCustomerId(), customer.getCustomerName());
        }
        return map;
    }

    private Map<Long, String> buildBranchNameMap() {
        List<Branch> branches = lookupDao.listBranches();
        Map<Long, String> map = new HashMap<>();
        for (Branch branch : branches) {
            map.put(branch.getBranchId(), branch.getBranchName());
        }
        return map;
    }

    private void loadTicketLines(long ticketId) {
        tableModel.clear();
        startPickingButton.setEnabled(false);
        saveResultsButton.setEnabled(false);
        statusLabel.setText("Status: Loading ticket lines...");

        new SwingWorker<Void, Void>() {
            private List<PickingLineEntry> entries = new ArrayList<>();
            private PickingHdr existingPicking;
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    List<PickTicketLine> lines = ticketService.listTicketLines(ticketId);
                    existingPicking = pickingDao.findByTicketId(ticketId);
                    for (PickTicketLine line : lines) {
                        com.ccinfom.model.Product product = lookupDao.findProductById(line.getProductId());
                        entries.add(PickingLineEntry.from(line, product));
                    }
                } catch (ValidationException | SQLException ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load ticket lines: " + error.getMessage());
                    statusLabel.setText("Status: Error loading ticket lines.");
                    return;
                }

                tableModel.setEntries(entries);

                if (existingPicking != null) {
                    currentPickingId = existingPicking.getPickingId();
                    startPickingButton.setEnabled(false);
                    saveResultsButton.setEnabled(true);
                    statusLabel.setText("Status: Picking session #" + currentPickingId + " active.");
                } else {
                    currentPickingId = null;
                    startPickingButton.setEnabled(pickerComboBox.getSelectedItem() != null);
                    saveResultsButton.setEnabled(false);
                    statusLabel.setText("Status: Ready to start picking.");
                }
            }
        }.execute();
    }

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    private void onTicketSelected() {
        ComboItem<PickTicketHdr> selected = (ComboItem<PickTicketHdr>) ticketComboBox.getSelectedItem();
        currentTicket = selected != null ? selected.getValue() : null;
        currentPickingId = null;
        tableModel.clear();

        if (currentTicket != null) {
            loadTicketLines(currentTicket.getPickTicketId());
        } else {
            statusLabel.setText("Status: Select a ticket to begin.");
            startPickingButton.setEnabled(false);
            saveResultsButton.setEnabled(false);
        }
    }

    private void onStartPicking() {
        ComboItem<PickTicketHdr> ticketItem = (ComboItem<PickTicketHdr>) ticketComboBox.getSelectedItem();
        ComboItem<Employee> pickerItem = (ComboItem<Employee>) pickerComboBox.getSelectedItem();

        if (ticketItem == null) {
            showWarning("Please select a ticket.");
            return;
        }
        if (pickerItem == null) {
            showWarning("Please select a picker.");
            return;
        }

        long ticketId = ticketItem.getValue().getPickTicketId();
        long pickerId = pickerItem.getValue().getEmployeeId();

        LOGGER.info(() -> String.format("[UI][T2_START_PICKING] ticket=%d picker=%d",
                ticketId, pickerId));
        startPickingButton.setEnabled(false);
        statusLabel.setText("Status: Starting picking session...");

        new SwingWorker<Long, Void>() {
            private Exception error;

            @Override
            protected Long doInBackground() {
                try {
                    pickingService.assignPickerAndStartPicking(ticketId, pickerId);
                    PickingHdr hdr = pickingDao.findByTicketId(ticketId);
                    return hdr != null ? hdr.getPickingId() : null;
                } catch (ValidationException | SQLException ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    LOGGER.log(Level.WARNING,
                            String.format("[UI][T2_START_PICKING] ticket=%d picker=%d error=%s",
                                    ticketId, pickerId, error.getMessage()),
                            error);
                    showWarning(error.getMessage());
                    startPickingButton.setEnabled(true);
                    statusLabel.setText("Status: Failed to start picking.");
                    return;
                }
                try {
                    currentPickingId = get();
                } catch (Exception e) {
                    currentPickingId = null;
                }

                if (currentPickingId != null) {
                    LOGGER.info(() -> String.format("[UI][T2_START_PICKING] SUCCESS ticket=%d picking=%d",
                            ticketId, currentPickingId));
                    statusLabel.setText("Status: Picking session #" + currentPickingId + " started.");
                    saveResultsButton.setEnabled(true);
                    ticketComboBox.setEnabled(false);
                    pickerComboBox.setEnabled(false);
                } else {
                    statusLabel.setText("Status: Unable to retrieve picking session id.");
                    LOGGER.warning(() -> String.format("[UI][T2_START_PICKING] ticket=%d missing_picking_id",
                            ticketId));
                }
            }
        }.execute();
    }

    private void onSaveResults() {
        if (currentPickingId == null) {
            showWarning("Start a picking session before saving results.");
            return;
        }

        List<PickingLineEntry> entries = tableModel.getEntries();
        if (entries.isEmpty()) {
            showWarning("No lines available to save.");
            return;
        }

        List<PickingLine> linesToSave;
        try {
            linesToSave = buildPickingLines(entries);
        } catch (ValidationException ex) {
            showWarning(ex.getMessage());
            return;
        }

        LOGGER.info(() -> String.format("[UI][T2_SAVE_PICKS] picking=%d lines=%d",
                currentPickingId, linesToSave.size()));
        saveResultsButton.setEnabled(false);
        statusLabel.setText("Status: Saving picked quantities...");

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    pickingService.savePickedItems(currentPickingId, linesToSave);
                } catch (ValidationException | SQLException ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    LOGGER.log(Level.WARNING,
                            String.format("[UI][T2_SAVE_PICKS] picking=%d error=%s",
                                    currentPickingId, error.getMessage()),
                            error);
                    showWarning(error.getMessage());
                    saveResultsButton.setEnabled(true);
                    statusLabel.setText("Status: Failed to save picked quantities.");
                    return;
                }
                LOGGER.info(() -> String.format("[UI][T2_SAVE_PICKS] SUCCESS picking=%d",
                        currentPickingId));
                statusLabel.setText("Status: Picked quantities saved.");
                JOptionPane.showMessageDialog(
                    PickingForm.this,
                    "Picking results saved for session #" + currentPickingId,
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // TODO[E-UI-T3-002] Trigger Pack workflow hand-off after saving picks.
                // Why: Phase E requires operator guidance to proceed to PackForm once picking completes.
                // Steps:
                //   1) Display deterministic dialog linking to PackForm or auto-open if policy allows.
                //   2) Refresh ticketComboBox (remove tickets now in Picking/Packed status as needed).
                // Acceptance:
                //   - Manual: After saving, user sees actionable prompt and ticket list reflects new status.
                //   - Demo: Phase E walkthrough shows transition from PickingForm to PackForm.
                // 
            }
        }.execute();
    }

    private List<PickingLine> buildPickingLines(List<PickingLineEntry> entries) throws ValidationException {
        List<PickingLine> result = new ArrayList<>();
        for (PickingLineEntry entry : entries) {
            BigDecimal picked = entry.getPickedQty();
            if (picked == null) {
                throw new ValidationException("Picked quantity cannot be empty.");
            }
            if (picked.compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Picked quantity cannot be negative.");
            }
            if (picked.compareTo(entry.getRequestedQty()) > 0) {
                throw new ValidationException("Picked quantity cannot exceed requested quantity for product " + entry.getProductId());
            }

            PickingLine line = new PickingLine();
            line.setPickingId(currentPickingId);
            line.setTicketLineId(entry.getTicketLineId());
            line.setProductId(entry.getProductId());
            line.setPickedQty(picked);
            line.setUom(entry.getUom());
            line.setUpdatedBy(DEFAULT_UPDATED_BY);
            result.add(line);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // -------------------------------------------------------------------------
    // Table model
    // -------------------------------------------------------------------------

    private static final class PickingLineTableModel extends AbstractTableModel {
        private static final String[] COLUMN_NAMES = {
            "Product ID", "SKU", "Product Name", "Requested Qty", "Picked Qty", "UOM"
        };

        private final List<PickingLineEntry> entries = new ArrayList<>();

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
            return switch (columnIndex) {
                case 0 -> Long.class;
                case 3, 4 -> BigDecimal.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 4;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PickingLineEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.getProductId();
                case 1 -> entry.getSku();
                case 2 -> entry.getProductName();
                case 3 -> entry.getRequestedQty();
                case 4 -> entry.getPickedQty();
                case 5 -> entry.getUom();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex != 4) {
                return;
            }
            PickingLineEntry entry = entries.get(rowIndex);
            try {
                BigDecimal newValue = new BigDecimal(aValue.toString());
                entry.setPickedQty(newValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            } catch (NumberFormatException ex) {
                // revert to old value, notify user silently
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        void setEntries(List<PickingLineEntry> newEntries) {
            entries.clear();
            entries.addAll(newEntries);
            fireTableDataChanged();
        }

        void clear() {
            if (entries.isEmpty()) {
                return;
            }
            entries.clear();
            fireTableDataChanged();
        }

        List<PickingLineEntry> getEntries() {
            return new ArrayList<>(entries);
        }
    }

    private static final class PickingLineEntry {
        private final long ticketLineId;
        private final long productId;
        private final String sku;
        private final String productName;
        private final BigDecimal requestedQty;
        private BigDecimal pickedQty;
        private final String uom;

        private PickingLineEntry(long ticketLineId,
                                 long productId,
                                 String sku,
                                 String productName,
                                 BigDecimal requestedQty,
                                 BigDecimal pickedQty,
                                 String uom) {
            this.ticketLineId = ticketLineId;
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.requestedQty = requestedQty;
            this.pickedQty = pickedQty;
            this.uom = uom;
        }

        static PickingLineEntry from(PickTicketLine line, com.ccinfom.model.Product product) {
            String sku = product != null ? product.getSku() : "N/A";
            String name = product != null ? product.getProductName() : "Unknown";
            String uom = product != null ? product.getUnitOfMeasure() : "EA";
            return new PickingLineEntry(
                line.getTicketLineId(),
                line.getProductId(),
                sku,
                name,
                line.getRequestedQty(),
                line.getRequestedQty(),
                uom
            );
        }

        long getTicketLineId() {
            return ticketLineId;
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

        BigDecimal getRequestedQty() {
            return requestedQty;
        }

        BigDecimal getPickedQty() {
            return pickedQty;
        }

        void setPickedQty(BigDecimal pickedQty) {
            this.pickedQty = pickedQty;
        }

        String getUom() {
            return uom;
        }
    }

    // -------------------------------------------------------------------------
    // Standalone launcher for manual testing
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PickingForm form = new PickingForm();
            form.setVisible(true);
        });
    }
}

