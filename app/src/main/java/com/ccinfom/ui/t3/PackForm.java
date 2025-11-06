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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase E UI form for Transaction T3 - Pack Boxes.
 *
 * <p>This class delivers the Member 1 responsibilities for the T3 Swing shell:
 * wiring the UI to the service layer, loading completed picking sessions from
 * {@link PickingDaoImpl}, displaying picking lines in a table, allowing the user
 * to edit packed quantities, adding lines to a {@link PackBox} via
 * {@link PackServiceImpl}, and sealing the box when packing is complete.</p>
 */
public class PackForm extends JFrame {

    private static final long serialVersionUID = 1L;

    private final PackServiceImpl packService;
    private final PickingDaoImpl pickingDao;

    // UI Components
    private JComboBox<ComboItem<Long>> pickingCombo;
    private JButton loadLinesButton;
    private PackLineTable lineTable;
    private JButton addButton;
    private JButton sealButton;
    private JButton resetButton;
    private StatusPanel statusPanel;

    private PackLineTableModel lineTableModel;

    // State
    private long currentBoxId = -1;
    private long currentPickTicketId = -1;
    private long currentPickingId = -1;

    public PackForm() {
        super("T3: Pack Boxes");

        // Services
        this.packService = new PackServiceImpl(
                new PackDaoImpl(),
                new PickingDaoImpl(),
                new TicketDaoImpl(),
                new LookupDaoImpl()
        );
        this.pickingDao = new PickingDaoImpl();

        initializeComponents();
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
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Top Panel - Picking Session
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

        // Action Panel
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        actionPanel.add(addButton);
        actionPanel.add(sealButton);
        actionPanel.add(resetButton);

        add(statusPanel, BorderLayout.NORTH);
        add(sessionPanel, BorderLayout.BEFORE_FIRST_LINE);
        add(tableScroll, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);
    }

    private void registerEventHandlers() {
        loadLinesButton.addActionListener(e -> onLoadPickingLines());
        addButton.addActionListener(e -> onAddToBox());
        sealButton.addActionListener(e -> onSealBox());
        resetButton.addActionListener(e -> resetForm());
    }

    private void loadPickingSessions() {
        UiTaskRunner.run(statusPanel,
                "Loading picking sessions...",
                "Picking sessions loaded.",
                () -> {
                    List<PickingHdr> list = new ArrayList<>();
                    for (long id = 1; id <= 10; id++) {
                        try {
                            PickingHdr hdr = pickingDao.findByTicketId(id);
                            if (hdr != null) list.add(hdr);
                        } catch (SQLException ignored) {}
                    }
                    return list;
                },
                sessions -> {
                    pickingCombo.removeAllItems();
                    for (PickingHdr hdr : sessions) {
                        pickingCombo.addItem(new ComboItem<>(hdr.getPickingId(),
                                "Picking #" + hdr.getPickingId() + " (Ticket " + hdr.getPickTicketId() + ")"));
                    }
                    if (sessions.isEmpty()) statusPanel.setInfo("No picking sessions available.");
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
                    for (PickingLine line : lines) {
                        PackLineEntry entry = new PackLineEntry(
                                line.getPickingLineId(),
                                line.getProductId(),
                                line.getPickedQty(),
                                line.getUom()
                        );
                        lineTableModel.addLine(entry);
                    }

                    if (!lines.isEmpty()) {
                        currentPickingId = pickingId;
                        try {
                            PickingHdr hdr = pickingDao.findByTicketId(lines.get(0).getTicketLineId());
                            currentPickTicketId = hdr != null ? hdr.getPickTicketId() : -1;
                        } catch (SQLException e) {
                            currentPickTicketId = -1;
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

        UiTaskRunner.run(statusPanel,
                "Packing lines...",
                "Lines packed successfully.",
                () -> {
                    if (currentBoxId < 0) {
                        PackBox newBox = new PackBox();
                        newBox.setPickTicketId(currentPickTicketId);
                        newBox.setPickingId(currentPickingId);
                        newBox.setSealedFlag(false);
                        newBox.setSourceRef("ui-T3");
                        newBox.setCreatedBy("ui");
                        newBox.setUpdatedBy("ui");
                        currentBoxId = packService.createBox(newBox);
                    }
                    packService.addLines(currentBoxId, lineTableModel.toPackBoxLines());
                    return true;
                },
                result -> statusPanel.setSuccess("Box #" + currentBoxId + " updated."),
                err -> {
                    String msg = (err instanceof ValidationException) ? ((ValidationException) err).getMessage() : err.getMessage();
                    statusPanel.setError("Failed to pack: " + msg);
                });
    }

    private void onSealBox() {
        if (currentBoxId < 0) {
            statusPanel.setError("No active box to seal.");
            return;
        }
        UiTaskRunner.run(statusPanel,
                "Sealing box...",
                "Box sealed successfully.",
                () -> {
                    packService.sealBox(currentBoxId, "tape", "ui");
                    return true;
                },
                ok -> {
                    statusPanel.setSuccess("Box #" + currentBoxId + " sealed.");
                    currentBoxId = -1;
                },
                err -> statusPanel.setError(err.getMessage()));
    }

    private void resetForm() {
        lineTableModel.clear();
        pickingCombo.removeAllItems();
        currentBoxId = currentPickingId = currentPickTicketId = -1;
        statusPanel.setInfo("Ready");
        loadPickingSessions();
    }

    // --- Inner Classes for Table Model and Entry ---

    public static class PackLineEntry {
        private final long pickingLineId;
        private final long productId;
        private final BigDecimal pickedQty;
        private BigDecimal packedQty;
        private final String uom;

        public PackLineEntry(long pickingLineId, long productId, BigDecimal pickedQty, String uom) {
            this.pickingLineId = pickingLineId;
            this.productId = productId;
            this.pickedQty = pickedQty;
            this.packedQty = pickedQty;
            this.uom = uom;
        }

        public long getPickingLineId() { return pickingLineId; }
        public long getProductId() { return productId; }
        public BigDecimal getPickedQty() { return pickedQty; }
        public BigDecimal getPackedQty() { return packedQty; }
        public void setPackedQty(BigDecimal packedQty) { this.packedQty = packedQty; }
        public String getUom() { return uom; }

        public PackBoxLine toPackBoxLine() {
            PackBoxLine line = new PackBoxLine();
            line.setPickingLineId(pickingLineId);
            line.setPackedQty(packedQty);
            line.setUom(uom);
            line.setSourceRef("ui-T3-pack");
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
        public boolean isCellEditable(int row, int column) { return column == 3; }

        @Override
        public Object getValueAt(int row, int column) {
            PackLineEntry line = lines.get(row);
            return switch(column) {
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
            if(column == 3) {
                try {
                    BigDecimal packed = new BigDecimal(value.toString());
                    lines.get(row).setPackedQty(packed);
                    fireTableCellUpdated(row, column);
                } catch (NumberFormatException ignored) {}
            }
        }

        public void addLine(PackLineEntry line) { lines.add(line); fireTableRowsInserted(lines.size()-1, lines.size()-1); }
        public List<PackLineEntry> getLines() { return new ArrayList<>(lines); }
        public List<PackBoxLine> toPackBoxLines() {
            List<PackBoxLine> boxLines = new ArrayList<>();
            for(PackLineEntry entry : lines) boxLines.add(entry.toPackBoxLine());
            return boxLines;
        }
        public void clear() { int size = lines.size(); lines.clear(); if(size>0) fireTableRowsDeleted(0, size-1); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PackForm().setVisible(true));
    }
}
