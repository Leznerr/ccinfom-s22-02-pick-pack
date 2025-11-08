package main.java.com.ccinfom.ui.t5;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.CloseDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.CloseDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.infra.InventoryHelper;
import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import com.ccinfom.service.CloseService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.CloseServiceImpl;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.UiTaskRunner;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// TODO[E-UI-T5-001] Implement CloseForm Swing UI.
// Requirements:
//   - Dropdown for dispatched tickets awaiting close.
//   - Table showing delivered vs requested vs short quantities with editable fields.
//   - Buttons: "Close as Delivered", "Close as Short", "Refresh".
//   - Display PoD fields (pod_ref, pod_ts) and notes input.
// Acceptance:
//   - Manual QA: closing a ticket via UI sets correct status and inventory adjustments.
//   - Demo-full-flow showcases delivered + short-close scenarios.

public class CloseForm extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CloseForm.class.getName());
    private static final String SOURCE_REF = "ui-T5-close";

    private final CloseService closeService;
    private final String currentUser = System.getProperty("user.name", "ui");

    // UI Components
    private JComboBox<DispatchedTicketItem> ticketCombo;
    private JTextField podRefField;
    private JTextField podTsField;
    private JTextArea notesArea;
    private JButton refreshBtn;
    private JButton closeDeliveredBtn;
    private JButton closeShortBtn;
    private StatusPanel statusPanel;

    private DefaultTableModel tableModel;
    private JTable quantitiesTable;

    // State
    private List<TicketLineItem> currentTicketLines;

    public CloseForm() {
        super("T5: Close Dispatch Ticket");

        // Initialize service with dependencies
        CloseDao closeDao = new CloseDaoImpl();
        TicketDao ticketDao = new TicketDaoImpl();
        InventoryHelper inventoryHelper = new InventoryHelper();
        this.closeService = new CloseServiceImpl(closeDao, ticketDao, inventoryHelper);

        initializeComponents();
        layoutComponents();
        registerListeners();
        loadDispatchedTickets();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    // TODO[E-UI-T5-002] Build UI components, data bindings, and ValidationException messaging.
    private void initializeComponents() {
        ticketCombo = new JComboBox<>();

        podRefField = new JTextField(20);
        podTsField = new JTextField(20);
        podTsField.setEditable(false);
        podTsField.setText(LocalDateTime.now().toString());

        notesArea = new JTextArea(3, 40);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        refreshBtn = new JButton("Refresh");
        closeDeliveredBtn = new JButton("Close as Delivered");
        closeShortBtn = new JButton("Close as Short");

        statusPanel = new StatusPanel();

        String[] columnNames = {"Item Name", "Item Code", "Requested Qty", "Delivered Qty", "Short Qty", "Reason"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 5; // Delivered Qty and Reason editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex >= 2 && columnIndex <= 4) return BigDecimal.class;
                return String.class;
            }
        };

        quantitiesTable = new JTable(tableModel);
        quantitiesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        quantitiesTable.setRowHeight(25);
        quantitiesTable.setFillsViewportHeight(true);
        quantitiesTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        quantitiesTable.getColumnModel().getColumn(1).setPreferredWidth(120);
        quantitiesTable.getColumnModel().getColumn(5).setPreferredWidth(180);

        currentTicketLines = new ArrayList<>();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Status panel at top
        add(statusPanel, BorderLayout.NORTH);

        // Center panel
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        // Selection panel
        JPanel selectionPanel = new JPanel(new GridBagLayout());
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Dispatched Ticket"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        selectionPanel.add(new JLabel("Select Ticket:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        selectionPanel.add(ticketCombo, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        selectionPanel.add(refreshBtn, gbc);

        // PoD panel
        JPanel podPanel = new JPanel(new GridBagLayout());
        podPanel.setBorder(BorderFactory.createTitledBorder("Proof of Delivery"));
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        podPanel.add(new JLabel("PoD Reference:*"), gbc);
        gbc.gridx = 1;
        podPanel.add(podRefField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        podPanel.add(new JLabel("PoD Timestamp:"), gbc);
        gbc.gridx = 1;
        podPanel.add(podTsField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        podPanel.add(new JLabel("Notes:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        podPanel.add(new JScrollPane(notesArea), gbc);

        // Top section
        JPanel topSection = new JPanel(new BorderLayout(8, 8));
        topSection.add(selectionPanel, BorderLayout.NORTH);
        topSection.add(podPanel, BorderLayout.CENTER);

        // Table panel
        JScrollPane tableScroll = new JScrollPane(quantitiesTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Item Quantities (Edit 'Delivered Qty' column)"));

        centerPanel.add(topSection, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(closeDeliveredBtn);
        buttonPanel.add(closeShortBtn);

        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void registerListeners() {
        ticketCombo.addActionListener(e -> onTicketSelected());

        // Auto-calculate short quantity when delivered qty changes
        tableModel.addTableModelListener(e -> {
            if (e.getColumn() == 3) { // Delivered Qty changed
                int row = e.getFirstRow();
                try {
                    BigDecimal requested = (BigDecimal) tableModel.getValueAt(row, 2);
                    Object deliveredObj = tableModel.getValueAt(row, 3);
                    BigDecimal delivered = convertToBigDecimal(deliveredObj);

                    if (requested != null && delivered != null) {
                        BigDecimal shortage = requested.subtract(delivered).max(BigDecimal.ZERO);
                        tableModel.setValueAt(shortage, row, 4);
                    }
                } catch (Exception ex) {
                    // Handle invalid input silently
                }
            }
        });

        refreshBtn.addActionListener(e -> loadDispatchedTickets());
        closeDeliveredBtn.addActionListener(e -> onCloseAsDelivered());
        closeShortBtn.addActionListener(e -> onCloseAsShort());
    }

    private void loadDispatchedTickets() {
        LOGGER.info("[UI][T5_LOAD_TICKETS] BEGIN");
        UiTaskRunner.run(statusPanel,
                "Loading dispatched tickets...",
                null,
                () -> {
                    String query = """
                        SELECT dh.dispatch_id, dh.pick_ticket_id, dh.dispatch_ts,
                               pth.ticket_number, pth.request_id
                        FROM dispatch_hdr dh
                        JOIN pick_ticket_hdr pth ON dh.pick_ticket_id = pth.pick_ticket_id
                        WHERE pth.status = 'Dispatched'
                          AND NOT EXISTS (
                            SELECT 1 FROM close_hdr ch 
                            WHERE ch.dispatch_id = dh.dispatch_id
                          )
                        ORDER BY dh.dispatch_ts DESC
                    """;

                    List<DispatchedTicketItem> tickets = new ArrayList<>();
                    try (Connection conn = DbConnection.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(query)) {

                        while (rs.next()) {
                            tickets.add(new DispatchedTicketItem(
                                    rs.getLong("dispatch_id"),
                                    rs.getLong("pick_ticket_id"),
                                    rs.getString("ticket_number"),
                                    rs.getLong("request_id"),
                                    rs.getTimestamp("dispatch_ts")
                            ));
                        }
                    }
                    return tickets;
                },
                tickets -> {
                    LOGGER.info(() -> String.format("[UI][T5_LOAD_TICKETS] count=%d", tickets.size()));
                    ticketCombo.removeAllItems();
                    for (DispatchedTicketItem ticket : tickets) {
                        ticketCombo.addItem(ticket);
                    }

                    if (tickets.isEmpty()) {
                        statusPanel.setInfo("No dispatched tickets awaiting close.");
                    } else {
                        statusPanel.setSuccess("Loaded " + tickets.size() + " dispatched ticket(s).");
                    }
                },
                ex -> {
                    LOGGER.log(Level.SEVERE, "[UI][T5_LOAD_TICKETS] FAILED", ex);
                    statusPanel.setError("Failed to load tickets: " + ex.getMessage());
                });
    }

    private void onTicketSelected() {
        DispatchedTicketItem selected = (DispatchedTicketItem) ticketCombo.getSelectedItem();
        if (selected == null) {
            tableModel.setRowCount(0);
            currentTicketLines.clear();
            return;
        }

        tableModel.setRowCount(0);
        currentTicketLines.clear();
        podRefField.setText("POD-" + selected.dispatchId);
        notesArea.setText("");

        LOGGER.info(() -> String.format("[UI][T5_LOAD_LINES] ticket=%d BEGIN", selected.ticketId));
        UiTaskRunner.run(statusPanel,
                "Loading ticket items...",
                null,
                () -> {
                    String query = """
                        SELECT ptl.ticket_line_id, ptl.item_id, ptl.requested_qty,
                               i.item_name, i.item_code
                        FROM pick_ticket_line ptl
                        JOIN item i ON ptl.item_id = i.item_id
                        WHERE ptl.pick_ticket_id = ?
                        ORDER BY ptl.ticket_line_id
                    """;

                    List<TicketLineItem> lines = new ArrayList<>();
                    try (Connection conn = DbConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(query)) {
                        pstmt.setLong(1, selected.ticketId);

                        try (ResultSet rs = pstmt.executeQuery()) {
                            while (rs.next()) {
                                lines.add(new TicketLineItem(
                                        rs.getLong("ticket_line_id"),
                                        rs.getString("item_name"),
                                        rs.getString("item_code"),
                                        rs.getBigDecimal("requested_qty")
                                ));
                            }
                        }
                    }
                    return lines;
                },
                lines -> {
                    LOGGER.info(() -> String.format("[UI][T5_LOAD_LINES] ticket=%d lines=%d",
                            selected.ticketId, lines.size()));
                    currentTicketLines.addAll(lines);

                    for (TicketLineItem line : lines) {
                        tableModel.addRow(new Object[]{
                                line.itemName,
                                line.itemCode,
                                line.requestedQty,
                                line.requestedQty,    // Default delivered = requested
                                BigDecimal.ZERO,       // Default short = 0
                                ""                     // Empty reason
                        });
                    }

                    statusPanel.setSuccess("Loaded " + lines.size() + " item(s) for " + selected.ticketNumber);
                },
                ex -> {
                    LOGGER.log(Level.SEVERE,
                            String.format("[UI][T5_LOAD_LINES] ticket=%d FAILED", selected.ticketId),
                            ex);
                    statusPanel.setError("Failed to load items: " + ex.getMessage());
                });
    }

    private void onCloseAsDelivered() {
        DispatchedTicketItem selected = (DispatchedTicketItem) ticketCombo.getSelectedItem();
        if (selected == null) {
            statusPanel.setError("Please select a ticket to close.");
            return;
        }

        // Validate inputs
        try {
            validateInputs();

            // Check all delivered quantities match requested
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                BigDecimal requested = (BigDecimal) tableModel.getValueAt(i, 2);
                BigDecimal delivered = convertToBigDecimal(tableModel.getValueAt(i, 3));
                if (requested.compareTo(delivered) != 0) {
                    statusPanel.setError("Cannot close as delivered: Item '" + tableModel.getValueAt(i, 0) +
                            "' has shortage. Use 'Close as Short' instead.");
                    return;
                }
            }
        } catch (Exception ex) {
            statusPanel.setError(ex.getMessage());
            return;
        }

        closeTicket(selected, CloseHeader.FinalStatus.Delivered);
    }

    private void onCloseAsShort() {
        DispatchedTicketItem selected = (DispatchedTicketItem) ticketCombo.getSelectedItem();
        if (selected == null) {
            statusPanel.setError("Please select a ticket to close.");
            return;
        }

        // Validate inputs
        try {
            validateInputs();

            // Check if there are shortages and reasons
            boolean hasShortage = false;
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                BigDecimal shortage = (BigDecimal) tableModel.getValueAt(i, 4);
                if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                    hasShortage = true;
                    String reason = (String) tableModel.getValueAt(i, 5);
                    if (reason == null || reason.trim().isEmpty()) {
                        statusPanel.setError("Please provide a reason for shortage on item: " +
                                tableModel.getValueAt(i, 0));
                        return;
                    }
                }
            }

            if (!hasShortage) {
                int confirm = JOptionPane.showConfirmDialog(this,
                        "No shortages detected. Close as delivered instead?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    closeTicket(selected, CloseHeader.FinalStatus.Delivered);
                }
                return;
            }
        } catch (Exception ex) {
            statusPanel.setError(ex.getMessage());
            return;
        }

        closeTicket(selected, CloseHeader.FinalStatus.ShortClosed);
    }

    private void validateInputs() throws Exception {
        if (podRefField.getText().trim().isEmpty()) {
            throw new Exception("PoD Reference is required.");
        }

        if (tableModel.getRowCount() == 0) {
            throw new Exception("No items to close.");
        }

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            try {
                BigDecimal delivered = convertToBigDecimal(tableModel.getValueAt(i, 3));
                BigDecimal requested = (BigDecimal) tableModel.getValueAt(i, 2);
                if (delivered.compareTo(BigDecimal.ZERO) < 0 || delivered.compareTo(requested) > 0) {
                    throw new Exception("Invalid delivered quantity for item: " + tableModel.getValueAt(i, 0));
                }
            } catch (NumberFormatException ex) {
                throw new Exception("Please enter valid numbers for delivered quantities.");
            }
        }
    }

    private void closeTicket(DispatchedTicketItem selected, CloseHeader.FinalStatus finalStatus) {
        LOGGER.info(() -> String.format("[UI][T5_CLOSE] ticket=%d dispatch=%d status=%s",
                selected.ticketId, selected.dispatchId, finalStatus));

        UiTaskRunner.run(statusPanel,
                "Closing ticket...",
                null,
                () -> {
                    // Build header
                    CloseHeader header = new CloseHeader();
                    header.setPickTicketId(selected.ticketId);
                    header.setDispatchId(selected.dispatchId);
                    header.setFinalStatus(finalStatus);
                    header.setPodRef(podRefField.getText().trim());
                    header.setPodTs(LocalDateTime.parse(podTsField.getText()));
                    header.setNotes(notesArea.getText().trim());
                    header.setSourceRef(SOURCE_REF);
                    header.setCreatedBy(currentUser);
                    header.setUpdatedBy(currentUser);

                    // Build variances
                    List<CloseVariance> variances = new ArrayList<>();
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        TicketLineItem lineItem = currentTicketLines.get(i);

                        BigDecimal requested = (BigDecimal) tableModel.getValueAt(i, 2);
                        BigDecimal delivered = convertToBigDecimal(tableModel.getValueAt(i, 3));
                        BigDecimal shortage = (BigDecimal) tableModel.getValueAt(i, 4);
                        String reason = (String) tableModel.getValueAt(i, 5);

                        CloseVariance variance = new CloseVariance();
                        variance.setTicketLineId(lineItem.ticketLineId);
                        variance.setRequestedQty(requested);
                        variance.setDeliveredQty(delivered);
                        variance.setShortQty(shortage);
                        variance.setReason(reason != null && !reason.trim().isEmpty() ? reason.trim() : null);
                        variance.setSourceRef(SOURCE_REF);
                        variance.setCreatedBy(currentUser);
                        variance.setUpdatedBy(currentUser);

                        variances.add(variance);
                    }

                    closeService.closeTicket(header, variances);
                    return selected;
                },
                ticket -> {
                    LOGGER.info(() -> String.format("[UI][T5_CLOSE] SUCCESS ticket=%d status=%s",
                            ticket.ticketId, finalStatus));

                    JOptionPane.showMessageDialog(this,
                            "Ticket " + ticket.ticketNumber + " closed successfully as " + finalStatus.getDbValue(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);

                    statusPanel.setSuccess("Ticket closed: " + ticket.ticketNumber + " [" + finalStatus.getDbValue() + "]");

                    // Clear and refresh
                    tableModel.setRowCount(0);
                    currentTicketLines.clear();
                    podRefField.setText("");
                    notesArea.setText("");
                    loadDispatchedTickets();
                },
                ex -> {
                    LOGGER.log(Level.WARNING,
                            String.format("[UI][T5_CLOSE] ticket=%d FAILED=%s",
                                    selected.ticketId, ex.getMessage()),
                            ex);
                    if (ex instanceof ValidationException) {
                        statusPanel.setError(((ValidationException) ex).getCode() + ": " + ex.getMessage());
                    } else {
                        statusPanel.setError(ex.getMessage());
                    }
                });
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        } else if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        return BigDecimal.ZERO;
    }

    // Inner classes
    private static class DispatchedTicketItem {
        final long dispatchId;
        final long ticketId;
        final String ticketNumber;
        final long requestId;
        final Timestamp dispatchTs;

        DispatchedTicketItem(long dispatchId, long ticketId, String ticketNumber,
                             long requestId, Timestamp dispatchTs) {
            this.dispatchId = dispatchId;
            this.ticketId = ticketId;
            this.ticketNumber = ticketNumber;
            this.requestId = requestId;
            this.dispatchTs = dispatchTs;
        }

        @Override
        public String toString() {
            return ticketNumber + " (Dispatch ID: " + dispatchId + ")";
        }
    }

    private static class TicketLineItem {
        final long ticketLineId;
        final String itemName;
        final String itemCode;
        final BigDecimal requestedQty;

        TicketLineItem(long ticketLineId, String itemName, String itemCode, BigDecimal requestedQty) {
            this.ticketLineId = ticketLineId;
            this.itemName = itemName;
            this.itemCode = itemCode;
            this.requestedQty = requestedQty;
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CloseForm().setVisible(true));
    }
}