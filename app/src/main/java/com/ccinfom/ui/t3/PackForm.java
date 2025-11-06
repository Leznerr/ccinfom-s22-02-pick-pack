package com.ccinfom.ui.t3;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PackDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.PackServiceImpl;
import com.ccinfom.ui.common.ComboItem;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.UiTaskRunner;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

/**
 * Phase E UI form for Transaction T3 - Pack Boxes.
 *
 * <p>Handles loading "Done" picking sessions, displaying lines,
 * adding them to boxes, and sealing when complete.</p>
 */
public class PackForm extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String STATUS_DONE = "Done";
    private static final String SOURCE_REF = "ui-T3-pack";
    private static final String SEAL_METHOD = "tape";

    private final PackServiceImpl packService;
    private final PickingDaoImpl pickingDao;
    private final TicketDaoImpl ticketDao;

    // UI Components
    private JComboBox<ComboItem<Long>> pickingCombo;
    private JButton loadLinesButton;
    private PackLineTable lineTable;
    private JButton addButton;
    private JButton sealButton;
    private JButton resetButton;
    private StatusPanel statusPanel;

    private PackLineTableModel lineTableModel;
    private boolean boxReadyToSeal;

    // State
    private long currentBoxId = -1;
    private long currentPickTicketId = -1;
    private long currentPickingId = -1;

    public PackForm() {
        super("T3: Pack Boxes");

        // Initialize services
        this.packService = new PackServiceImpl(
                new PackDaoImpl(),
                new PickingDaoImpl(),
                new TicketDaoImpl(),
                new LookupDaoImpl()
        );
        this.pickingDao = new PickingDaoImpl();
        this.ticketDao = new TicketDaoImpl();

        initializeComponents();
        enterSelectionState();
        layoutComponents();
        registerEventHandlers();
        loadPickingSessions();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);
    }

    private void initializeComponents() {
        pickingCombo = new JComboBox<>();
        loadLinesButton = new JButton("Load Lines");

        lineTableModel = new PackLineTableModel();
        lineTable = new PackLineTable(lineTableModel);

        addButton = new JButton("Add to Box");
        sealButton = new JButton("Seal Box");
        resetButton = new JButton("Reset");

        statusPanel = new StatusPanel();

        sealButton.setEnabled(false);
        boxReadyToSeal = false;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Status Panel
        add(statusPanel, BorderLayout.NORTH);

        // Center Panel
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        // Picking Session Panel
        JPanel sessionPanel = new JPanel(new GridBagLayout());
        sessionPanel.setBorder(BorderFactory.createTitledBorder("Picking Session"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        sessionPanel.add(new JLabel("Picking Session:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        sessionPanel.add(pickingCombo, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        sessionPanel.add(loadLinesButton, gbc);

        // Table Panel
        JScrollPane tableScroll = new JScrollPane(lineTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Picking Lines"));

        // Action Panel (bottom buttons)
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        actionPanel.add(addButton);
        actionPanel.add(sealButton);
        actionPanel.add(resetButton);

        // Add components to center panel
        centerPanel.add(sessionPanel, BorderLayout.NORTH);
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        centerPanel.add(actionPanel, BorderLayout.SOUTH);

        // Add center panel to frame
        add(centerPanel, BorderLayout.CENTER);
    }

    private void registerEventHandlers() {
        loadLinesButton.addActionListener(e -> onLoadPickingLines());
        addButton.addActionListener(e -> onAddToBox());
        sealButton.addActionListener(e -> onSealBox());
        resetButton.addActionListener(e -> onReset());
    }

    // Loads only picking sessions that are "Done" (ready to pack)
    private void loadPickingSessions() {
        loadPickingSessions(null, null);
    }

    private void loadPickingSessions(String followUpMessage, StatusType followUpType) {
        UiTaskRunner.run(statusPanel,
                "Loading picking sessions...",
                null,
                () -> pickingDao.listByStatus(STATUS_DONE),
                sessions -> {
                    pickingCombo.removeAllItems();
                    pickingCombo.setSelectedItem(null);
                    pickingCombo.revalidate();
                    pickingCombo.repaint();
                    for (PickingHdr hdr : sessions) {
                        pickingCombo.addItem(new ComboItem<>(hdr.getPickingId(),
                                "Picking #" + hdr.getPickingId() + " (Ticket " + hdr.getPickTicketId() + ")"));
                    }

                    if (followUpMessage != null) {
                        if (followUpType == StatusType.SUCCESS) {
                            statusPanel.setSuccess(followUpMessage);
                        } else {
                            statusPanel.setInfo(followUpMessage);
                        }
                    } else if (sessions.isEmpty()) {
                        statusPanel.setInfo("No 'Done' picking sessions available.");
                    } else {
                        statusPanel.setSuccess("Picking sessions loaded.");
                    }
                },
                ex -> statusPanel.setError("Failed to load sessions: " + ex.getMessage()));
    }

    private void onLoadPickingLines() {
        ComboItem<Long> selected = (ComboItem<Long>) pickingCombo.getSelectedItem();
        if (selected == null) {
            statusPanel.setError("Please select a picking session first.");
            return;
        }

        long pickingId = selected.getValue();
        UiTaskRunner.run(statusPanel,
                "Loading picking lines...",
                "Picking lines loaded.",
                () -> pickingDao.listLinesByPickingId(pickingId),
                lines -> {
                    lineTableModel.clear();

                    Map<Long, PackBoxLine> existingLines = new HashMap<>();
                    currentBoxId = -1;
                    try {
                        Optional<PackBox> openBox = packService.findOpenBox(pickingId);
                        if (openBox.isPresent()) {
                            currentBoxId = openBox.get().getBoxId();
                            List<PackBoxLine> packedLines = packService.listLinesByBoxId(currentBoxId);
                            for (PackBoxLine packed : packedLines) {
                                existingLines.put(packed.getPickingLineId(), packed);
                            }
                        }
                    } catch (SQLException e) {
                        statusPanel.setError("Failed to load existing box: " + e.getMessage());
                        return;
                    }

                    for (PickingLine line : lines) {
                        PackLineEntry entry = new PackLineEntry(
                                line.getPickingLineId(),
                                line.getProductId(),
                                line.getPickedQty(),
                                line.getUom()
                        );
                        PackBoxLine packed = existingLines.get(line.getPickingLineId());
                        if (packed != null) {
                            entry.setPackedQty(packed.getPackedQty());
                            entry.setBoxed(true);
                        }
                        lineTableModel.addLine(entry);
                    }

                    if (!lines.isEmpty()) {
                        currentPickingId = pickingId;
                        try {
                            PickingHdr hdr = pickingDao.findByPickingId(pickingId);
                            currentPickTicketId = hdr != null ? hdr.getPickTicketId() : -1;
                        } catch (SQLException e) {
                            currentPickTicketId = -1;
                        }

                        if (currentBoxId > 0) {
                            if (lineTableModel.hasPendingLines()) {
                                statusPanel.setInfo("Box #" + currentBoxId + " reopened. Pack remaining lines then seal.");
                                enterSelectionState();
                            } else {
                                boxReadyToSeal = true;
                                enterSealingState();
                                statusPanel.setInfo("Box #" + currentBoxId + " already packed. Seal to complete.");
                            }
                        } else {
                            enterSelectionState();
                        }
                    } else {
                        statusPanel.setInfo("No picking lines found for this session.");
                    }
                },
                ex -> statusPanel.setError("Failed to load lines: " + ex.getMessage()));
    }

    private void onAddToBox() {
        if (lineTableModel.getRowCount() == 0) {
            statusPanel.setError("No picking lines to pack.");
            return;
        }

        try {
            for (PackLineEntry line : lineTableModel.getLines()) {
                BigDecimal packedQty = line.getPackedQty();
                BigDecimal pickedQty = line.getPickedQty();
                if (packedQty.compareTo(BigDecimal.ZERO) <= 0 || packedQty.compareTo(pickedQty) > 0) {
                    throw new IllegalArgumentException("Invalid packed quantity for line " + line.getPickingLineId());
                }
            }
        } catch (Exception ex) {
            statusPanel.setError("Invalid input: " + ex.getMessage());
            return;
        }

        final String user = currentUser();
        List<PackBoxLine> linesToAdd = lineTableModel.toPackBoxLines(user, SOURCE_REF);
        if (linesToAdd.isEmpty()) {
            boxReadyToSeal = true;
            enterSealingState();
            statusPanel.setInfo("All picking lines are already boxed. Seal the box to complete packing.");
            return;
        }

        List<Long> lineIdsToMark = new ArrayList<>();
        for (PackBoxLine boxLine : linesToAdd) {
            lineIdsToMark.add(boxLine.getPickingLineId());
        }

        UiTaskRunner.run(statusPanel,
                "Packing lines...",
                null,
                () -> {
                    String ref = SOURCE_REF;

                    if (currentPickTicketId < 0) {
                        throw new ValidationException("NO_TICKET", "Missing pick ticket for selected picking session.");
                    }

                    // Create new box if none exists
                    if (currentBoxId < 0) {
                        PackBox newBox = new PackBox();
                        newBox.setPickTicketId(currentPickTicketId);
                        newBox.setPickingId(currentPickingId);
                        newBox.setSealedFlag(false);
                        newBox.setSourceRef(ref);
                        newBox.setCreatedBy(user);
                        newBox.setUpdatedBy(user);
                        newBox.setCreatedAt(LocalDateTime.now());
                        newBox.setUpdatedAt(LocalDateTime.now());
                        currentBoxId = packService.createBox(newBox);
                    }
                    packService.addLines(currentBoxId, linesToAdd);
                    return lineIdsToMark;
                },
                ids -> {
                    lineTableModel.markAsBoxed(ids);
                    if (lineTableModel.hasPendingLines()) {
                        boxReadyToSeal = false;
                        enterSelectionState();
                        statusPanel.setSuccess("Box #" + currentBoxId + " updated. Pack remaining lines or seal when finished.");
                    } else {
                        boxReadyToSeal = true;
                        enterSealingState();
                        statusPanel.setSuccess("Box #" + currentBoxId + " updated. Seal the box to complete packing.");
                    }
                },
                err -> {
                    if (err instanceof ValidationException ve)
                        statusPanel.setError(ve.getCode() + ": " + ve.getMessage());
                    else
                        statusPanel.setError("Failed to pack: " + err.getMessage());
                });
    }

    private void onSealBox() {
        if (currentBoxId < 0) {
            statusPanel.setError("No active box to seal.");
            return;
        }

        // Ask user for confirmation
        int confirm = JOptionPane.showConfirmDialog(this,
                "Seal this box?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        UiTaskRunner.run(statusPanel,
                "Sealing box...",
                null,
                () -> {
                    String user = currentUser();
                    packService.sealBox(currentBoxId, SEAL_METHOD, user);
                    return true;
                },
                ok -> {
                    String followUpMessage;
                    StatusType followUpType;
                    try {
                        boolean packed = packService.isTicketPacked(currentPickTicketId);
                        if (packed) {
                            followUpMessage = "Box #" + currentBoxId + " sealed. Ticket is now Packed.";
                            followUpType = StatusType.SUCCESS;
                        } else {
                            followUpMessage = "Box sealed, but ticket not yet updated to 'Packed'.";
                            followUpType = StatusType.INFO;
                        }
                    } catch (SQLException e) {
                        boxReadyToSeal = false;
                        resetForm("Box sealed, but status verification failed: " + e.getMessage(), StatusType.INFO);
                        return;
                    }
                    boxReadyToSeal = false;
                    resetForm(followUpMessage, followUpType);
                },
                err -> statusPanel.setError(err.getMessage()));
    }

    private void onReset() {
        if (boxReadyToSeal && currentBoxId > 0) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "This will abandon Box #" + currentBoxId + " that is waiting to be sealed. Continue?",
                    "Confirm Reset", JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        resetForm();
    }

    private void resetForm() {
        resetForm(null, null);
    }

    private void resetForm(String followUpMessage, StatusType type) {
        lineTableModel.clear();
        pickingCombo.removeAllItems();
        pickingCombo.setSelectedItem(null);
        currentBoxId = currentPickingId = currentPickTicketId = -1;
        boxReadyToSeal = false;
        enterSelectionState();
        if (followUpMessage == null) {
            statusPanel.setInfo("Ready");
        }
        loadPickingSessions(followUpMessage, type);
    }

    private String currentUser() {
        return System.getProperty("user.name", "ui");
    }

    private enum StatusType {
        SUCCESS,
        INFO
    }

    private void enterSelectionState() {
        pickingCombo.setEnabled(true);
        loadLinesButton.setEnabled(true);
        lineTable.setEnabled(true);
        addButton.setEnabled(true);
        sealButton.setEnabled(false);
    }

    private void enterSealingState() {
        pickingCombo.setEnabled(false);
        loadLinesButton.setEnabled(false);
        lineTable.setEnabled(false);
        addButton.setEnabled(false);
        sealButton.setEnabled(true);
    }

    // --- Inner Classes for Table Model and Entry ---

    public static class PackLineEntry {
        private final long pickingLineId;
        private final long productId;
        private final BigDecimal pickedQty;
        private BigDecimal packedQty;
        private boolean boxed;
        private final String uom;

        public PackLineEntry(long pickingLineId, long productId, BigDecimal pickedQty, String uom) {
            this.pickingLineId = pickingLineId;
            this.productId = productId;
            this.pickedQty = pickedQty;
            this.packedQty = pickedQty;
            this.uom = uom;
            this.boxed = false;
        }

        public long getPickingLineId() { return pickingLineId; }
        public long getProductId() { return productId; }
        public BigDecimal getPickedQty() { return pickedQty; }
        public BigDecimal getPackedQty() { return packedQty; }
        public void setPackedQty(BigDecimal packedQty) { this.packedQty = packedQty; }
        public String getUom() { return uom; }
        public boolean isBoxed() { return boxed; }
        public void setBoxed(boolean boxed) { this.boxed = boxed; }

        public PackBoxLine toPackBoxLine(String user, String ref) {
            PackBoxLine line = new PackBoxLine();
            line.setPickingLineId(pickingLineId);
            line.setPackedQty(packedQty);
            line.setUom(uom);
            line.setSourceRef(ref);
            line.setCreatedBy(user);
            line.setUpdatedBy(user);
            line.setCreatedAt(LocalDateTime.now());
            line.setUpdatedAt(LocalDateTime.now());
            return line;
        }
    }

    private static final class PackLineTable extends JTable {
        PackLineTable(PackLineTableModel model) {
            super(model);
            setFillsViewportHeight(true);
            setRowHeight(24);
        }
        @Override
        public PackLineTableModel getModel() { return (PackLineTableModel) super.getModel(); }
    }

    private static final class PackLineTableModel extends AbstractTableModel {
        private final List<PackLineEntry> lines = new ArrayList<>();
        private static final String[] COLUMN_NAMES = {"Picking Line ID", "Product ID", "Picked Qty", "Packed Qty", "UOM"};

        @Override
        public int getRowCount() { return lines.size(); }
        @Override
        public int getColumnCount() { return COLUMN_NAMES.length; }
        @Override
        public String getColumnName(int column) { return COLUMN_NAMES[column]; }
        @Override
        public boolean isCellEditable(int row, int column) {
            if (column != 3) return false;
            return !lines.get(row).isBoxed();
        }

        @Override
        public Object getValueAt(int row, int column) {
            PackLineEntry line = lines.get(row);
            return switch (column) {
                case 0 -> line.getPickingLineId();
                case 1 -> line.getProductId();
                case 2 -> line.getPickedQty();
                case 3 -> line.getPackedQty();
                case 4 -> line.getUom();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column == 3) {
                try {
                    BigDecimal packed = new BigDecimal(value.toString());
                    PackLineEntry entry = lines.get(row);
                    if (!entry.isBoxed()) {
                        entry.setPackedQty(packed);
                    }
                    fireTableCellUpdated(row, column);
                } catch (NumberFormatException ignored) {}
            }
        }

        public void addLine(PackLineEntry line) {
            lines.add(line);
            fireTableRowsInserted(lines.size() - 1, lines.size() - 1);
        }

        public List<PackLineEntry> getLines() { return new ArrayList<>(lines); }

        public List<PackBoxLine> toPackBoxLines(String user, String ref) {
            List<PackBoxLine> boxLines = new ArrayList<>();
            for (PackLineEntry entry : lines) {
                if (!entry.isBoxed()) {
                    boxLines.add(entry.toPackBoxLine(user, ref));
                }
            }
            return boxLines;
        }

        public void clear() {
            int size = lines.size();
            lines.clear();
            if (size > 0) fireTableRowsDeleted(0, size - 1);
        }

        public boolean hasPendingLines() {
            for (PackLineEntry entry : lines) {
                if (!entry.isBoxed()) {
                    return true;
                }
            }
            return false;
        }

        public void markAsBoxed(List<Long> pickingLineIds) {
            if (pickingLineIds == null || pickingLineIds.isEmpty()) return;
            for (int i = 0; i < lines.size(); i++) {
                PackLineEntry entry = lines.get(i);
                if (pickingLineIds.contains(entry.getPickingLineId())) {
                    entry.setBoxed(true);
                    fireTableRowsUpdated(i, i);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PackForm().setVisible(true));
    }
}
