package com.ccinfom.ui.t4;

import com.ccinfom.dao.impl.DispatchDaoImpl;
import com.ccinfom.dao.impl.PackDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.LookupValue;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import com.ccinfom.service.DispatchService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.DispatchServiceImpl;
import com.ccinfom.ui.common.ComboItem;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.UiTaskRunner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Phase E UI for T4 Dispatch workflow.
 */
public class DispatchForm extends JFrame {

    private final DispatchService dispatchService;

    private final JComboBox<ComboItem<Long>> ticketCombo = new JComboBox<>();
    private final JComboBox<ComboItem<Long>> vehicleCombo = new JComboBox<>();
    private final JComboBox<ComboItem<Long>> driverCombo = new JComboBox<>();
    private final JTextField manifestField = new JTextField();
    private final StatusPanel statusPanel = new StatusPanel();
    private final DefaultTableModel tableModel;
    private final JTable boxTable;

    private final List<DispatchLine> currentBoxes = new ArrayList<>();

    private Long currentDispatchId;
    private final String currentUser = System.getProperty("user.name", "ui");

    private final JButton loadManifestButton = new JButton("Create Manifest");
    private final JButton recordDepartureButton = new JButton("Record Departure");
    private final JButton recordArrivalButton = new JButton("Record Arrival");

    private static final Logger LOGGER = Logger.getLogger(DispatchForm.class.getName());
    private boolean suppressTicketEvents;

    public DispatchForm() {
        setTitle("Dispatch Management");
        setSize(940, 620);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        DispatchDao dispatchDao = new DispatchDaoImpl();
        PackDao packDao = new PackDaoImpl();
        TicketDao ticketDao = new TicketDaoImpl();
        this.dispatchService = new DispatchServiceImpl(dispatchDao, packDao, ticketDao);

        JPanel root = (JPanel) getContentPane();
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filterPanel = buildFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        String[] columns = {"Box ID", "Description", "Weight (kg)"};
        this.tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        this.boxTable = new JTable(tableModel);
        boxTable.setRowHeight(24);

        JScrollPane scrollPane = new JScrollPane(boxTable);
        scrollPane.setBorder(new EmptyBorder(10, 0, 10, 0));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(loadManifestButton);
        buttonPanel.add(recordDepartureButton);
        buttonPanel.add(recordArrivalButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        attachListeners();
        toggleManifestActions(false);
        loadDropdowns();

        setVisible(true);
    }

    private JPanel buildFilterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(0, 0, 10, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Pick Ticket:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(ticketCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Vehicle:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(vehicleCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        panel.add(new JLabel("Driver:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(driverCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        panel.add(new JLabel("Manifest No.:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(manifestField, gbc);

        return panel;
    }

    private void attachListeners() {
        ticketCombo.addActionListener(e -> {
            if (suppressTicketEvents) {
                return;
            }
            ComboItem<Long> selected = getSelected(ticketCombo);
            if (selected == null) {
                clearBoxes();
                return;
            }
            loadBoxesIntoTable(selected.getValue());
        });

        loadManifestButton.addActionListener(e -> onCreateManifest());
        recordDepartureButton.addActionListener(e -> onRecordTiming(true));
        recordArrivalButton.addActionListener(e -> onRecordTiming(false));
    }

    private void loadDropdowns() {
        UiTaskRunner.run(statusPanel,
                "Loading dispatch references...",
                null,
                () -> {
                    List<LookupValue> tickets = dispatchService.findReadyTickets();
                    List<LookupValue> vehicles = dispatchService.findAvailableVehicles();
                    List<LookupValue> drivers = dispatchService.findAvailableDrivers();
                    return new DropdownPayload(tickets, vehicles, drivers);
                },
                payload -> {
                    LOGGER.info(() -> String.format("[UI][T4_LOAD_REFERENCES] tickets=%d vehicles=%d drivers=%d",
                            payload.tickets().size(),
                            payload.vehicles().size(),
                            payload.drivers().size()));
                    suppressTicketEvents = true;
                    ticketCombo.setModel(toModel(payload.tickets()));
                    vehicleCombo.setModel(toModel(payload.vehicles()));
                    driverCombo.setModel(toModel(payload.drivers()));
                    ticketCombo.setSelectedItem(null);
                    suppressTicketEvents = false;
                    clearBoxes();
                    statusPanel.setSuccess("References loaded.");
                },
                err -> {
                    LOGGER.log(Level.SEVERE, "[UI][T4_LOAD_REFERENCES] FAILED", err);
                    statusPanel.setError("Failed to load references: " + err.getMessage());
                });
    }

    private void loadBoxesIntoTable(long ticketId) {
        LOGGER.info(() -> String.format("[UI][T4_LOAD_BOXES] ticket=%d BEGIN", ticketId));
        currentDispatchId = null;
        toggleManifestActions(false);
        UiTaskRunner.run(statusPanel,
                "Loading sealed boxes...",
                null,
                () -> dispatchService.findBoxesForTicket(ticketId),
                boxes -> {
                    LOGGER.info(() -> String.format("[UI][T4_LOAD_BOXES] ticket=%d boxes=%d", ticketId, boxes.size()));
                    currentBoxes.clear();
                    currentBoxes.addAll(boxes);
                    tableModel.setRowCount(0);
                    for (DispatchLine line : boxes) {
                        tableModel.addRow(new Object[]{
                                line.getBoxId(),
                                "Box " + line.getBoxId(),
                                "-"
                        });
                    }
                    statusPanel.setSuccess(boxes.isEmpty()
                            ? "No sealed boxes for this ticket."
                            : "Loaded " + boxes.size() + " sealed box(es).");
                },
                err -> {
                    LOGGER.log(Level.SEVERE,
                            String.format("[UI][T4_LOAD_BOXES] ticket=%d FAILED=%s", ticketId, err.getMessage()),
                            err);
                    statusPanel.setError("Failed to load boxes: " + err.getMessage());
                });
    }

    private void onCreateManifest() {
        ComboItem<Long> ticket = getSelected(ticketCombo);
        ComboItem<Long> vehicle = getSelected(vehicleCombo);
        ComboItem<Long> driver = getSelected(driverCombo);

        if (ticket == null) {
            statusPanel.setError("Select a packed ticket first.");
            return;
        }
        if (vehicle == null) {
            statusPanel.setError("Select an available vehicle.");
            return;
        }
        if (driver == null) {
            statusPanel.setError("Select an active driver.");
            return;
        }
        if (currentBoxes.isEmpty()) {
            statusPanel.setError("No sealed boxes to load for this ticket.");
            return;
        }

        String manifestNo = manifestField.getText();
        if (manifestNo == null || manifestNo.isBlank()) {
            manifestNo = "M" + System.currentTimeMillis();
        }
        final String finalManifestNo = manifestNo.trim();

        LOGGER.info(() -> String.format("[UI][T4_CREATE_MANIFEST] ticket=%d vehicle=%d driver=%d boxes=%d manifest=%s",
                ticket.getValue(), vehicle.getValue(), driver.getValue(), currentBoxes.size(), finalManifestNo));
        UiTaskRunner.run(statusPanel,
                "Creating dispatch manifest...",
                null,
                () -> {
                    DispatchHeader hdr = new DispatchHeader();
                    hdr.setPickTicketId(ticket.getValue());
                    hdr.setVehicleId(vehicle.getValue());
                    hdr.setDriverId(driver.getValue());
                    hdr.setManifestNo(finalManifestNo);
                    hdr.setCreatedBy(currentUser);
                    hdr.setUpdatedBy(currentUser);

                    List<DispatchLine> lines = new ArrayList<>();
                    for (DispatchLine box : currentBoxes) {
                        DispatchLine line = new DispatchLine();
                        line.setBoxId(box.getBoxId());
                        line.setSourceRef(box.getSourceRef());
                        line.setCreatedBy(currentUser);
                        line.setUpdatedBy(currentUser);
                        lines.add(line);
                    }
                    return dispatchService.createDispatch(hdr, lines);
                },
                hdr -> {
                    currentDispatchId = hdr.getDispatchId();
                    statusPanel.setSuccess("Dispatch created (ID " + currentDispatchId + ").");
                    toggleManifestActions(true);
                    manifestField.setText("");
                    LOGGER.info(() -> String.format("[UI][T4_CREATE_MANIFEST] SUCCESS dispatch=%d ticket=%d",
                            currentDispatchId, ticket.getValue()));

                    if (ticket != null) {
                        suppressTicketEvents = true;
                        ticketCombo.removeItem(ticket);
                        ticketCombo.setSelectedItem(null);
                        suppressTicketEvents = false;
                    }
                    vehicleCombo.setSelectedItem(null);
                    driverCombo.setSelectedItem(null);
                },
                err -> {
                    LOGGER.log(Level.WARNING,
                            String.format("[UI][T4_CREATE_MANIFEST] ticket=%d FAILED=%s",
                                    ticket.getValue(), err.getMessage()),
                            err);
                    statusPanel.setError(err.getMessage());
                });
    }

    private void onRecordTiming(boolean departure) {
        if (currentDispatchId == null) {
            statusPanel.setError("Create a manifest first before recording timings.");
            return;
        }

        LOGGER.info(() -> String.format("[UI][T4_RECORD_%s] dispatch=%d",
                departure ? "DEPARTURE" : "ARRIVAL", currentDispatchId));
        UiTaskRunner.run(statusPanel,
                departure ? "Recording departure..." : "Recording arrival...",
                null,
                () -> {
                    DispatchHeader hdr = new DispatchHeader();
                    if (departure) {
                        hdr.setDepartTs(LocalDateTime.now());
                    } else {
                        hdr.setArriveTs(LocalDateTime.now());
                    }
                    hdr.setUpdatedBy(currentUser);
                    if (departure) {
                        dispatchService.registerDeparture(currentDispatchId, hdr);
                    } else {
                        dispatchService.registerArrival(currentDispatchId, hdr);
                    }
                    return null;
                },
                ignored -> {
                    LOGGER.info(() -> String.format("[UI][T4_RECORD_%s] SUCCESS dispatch=%d",
                            departure ? "DEPARTURE" : "ARRIVAL", currentDispatchId));
                    statusPanel.setSuccess(departure
                            ? "Departure recorded."
                            : "Arrival recorded.");
                },
                err -> {
                    LOGGER.log(Level.WARNING,
                            String.format("[UI][T4_RECORD_%s] dispatch=%d FAILED=%s",
                                    departure ? "DEPARTURE" : "ARRIVAL", currentDispatchId, err.getMessage()),
                            err);
                    statusPanel.setError(err.getMessage());
                });
    }

    private void clearBoxes() {
        currentBoxes.clear();
        tableModel.setRowCount(0);
        currentDispatchId = null;
        toggleManifestActions(false);
    }

    private void toggleManifestActions(boolean dispatchCreated) {
        recordDepartureButton.setEnabled(dispatchCreated);
        recordArrivalButton.setEnabled(dispatchCreated);
    }

    private DefaultComboBoxModel<ComboItem<Long>> toModel(List<LookupValue> options) {
        DefaultComboBoxModel<ComboItem<Long>> model = new DefaultComboBoxModel<>();
        for (LookupValue option : options) {
            model.addElement(new ComboItem<>(option.getId(), option.getLabel()));
        }
        return model;
    }

    private ComboItem<Long> getSelected(JComboBox<ComboItem<Long>> comboBox) {
        return (ComboItem<Long>) comboBox.getSelectedItem();
    }

    private record DropdownPayload(List<LookupValue> tickets,
                                   List<LookupValue> vehicles,
                                   List<LookupValue> drivers) {
    }
}
