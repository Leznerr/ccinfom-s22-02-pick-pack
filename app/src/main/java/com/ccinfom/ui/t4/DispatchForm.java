package com.ccinfom.ui.t4;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.ccinfom.dao.impl.DispatchDaoImpl;
import com.ccinfom.dao.impl.PackDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import com.ccinfom.service.DispatchService;
import com.ccinfom.service.impl.DispatchServiceImpl;
import com.ccinfom.ui.common.StatusPanel;
import com.ccinfom.ui.common.UiTaskRunner;

public class DispatchForm extends JFrame {

    private static final long serialVersionUID = 1L;

    // Class fields
    private DefaultTableModel tableModel;
    private Long currentDispatchId;
    private String currentUser = "ui"; // Replace with actual logged-in user

    private JComboBox<String> ticketCombo;
    private JComboBox<String> vehicleCombo;
    private JComboBox<String> driverCombo;
    private StatusPanel statusPanel;

    private DispatchService dispatchService;

    public DispatchForm() {
        // === Window setup ===
        setTitle("Dispatch Management");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null); // center

        // Initialize DAOs and Service
        DispatchDao dispatchDao = new DispatchDaoImpl();
        PackDao packDao = new PackDaoImpl();
        TicketDao ticketDao = new TicketDaoImpl();
        dispatchService = new DispatchServiceImpl(dispatchDao, packDao, ticketDao);

        // === Top Panel (Filters) ===
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(topPanel, BorderLayout.NORTH);

        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Dispatch Filters"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ticket
        gbc.gridx = 0; gbc.gridy = 0;
        filterPanel.add(new JLabel("Pick Ticket:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        ticketCombo = new JComboBox<>();
        filterPanel.add(ticketCombo, gbc);

        // Vehicle
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        filterPanel.add(new JLabel("Vehicle:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        vehicleCombo = new JComboBox<>();
        filterPanel.add(vehicleCombo, gbc);

        // Driver
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        filterPanel.add(new JLabel("Driver:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        driverCombo = new JComboBox<>();
        filterPanel.add(driverCombo, gbc);

        topPanel.add(filterPanel, BorderLayout.CENTER);

        // === Center Panel (Table) ===
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        add(centerPanel, BorderLayout.CENTER);

        String[] columns = {"Box ID", "Description", "Weight (kg)", "Sealed"};
        tableModel = new DefaultTableModel(new Object[0][columns.length], columns) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return (columnIndex == 3) ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // Only Sealed column editable
            }
        };

        JTable boxTable = new JTable(tableModel);
        boxTable.setRowHeight(24);
        boxTable.setFillsViewportHeight(true);
        boxTable.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(boxTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Sealed Boxes"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // === Bottom Panel (Actions + Status) ===
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        add(bottomPanel, BorderLayout.SOUTH);

        statusPanel = new StatusPanel();
        statusPanel.setInfo("Ready");

        JButton btnLoadManifest = new JButton("Load to Manifest");
        JButton btnRecordDeparture = new JButton("Record Departure");
        JButton btnRecordArrival = new JButton("Record Arrival");

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnLoadManifest);
        buttonPanel.add(btnRecordDeparture);
        buttonPanel.add(btnRecordArrival);

        bottomPanel.add(buttonPanel, BorderLayout.EAST);
        bottomPanel.add(statusPanel, BorderLayout.SOUTH);

        // === Populate Dropdowns ===
        loadDropdowns();

        // === Populate Table on Ticket Selection ===
        ticketCombo.addActionListener(e -> {
            String selectedTicket = (String) ticketCombo.getSelectedItem();
            if (selectedTicket != null) {
                Long ticketId = Long.parseLong(selectedTicket); // assuming ID string
                loadBoxesIntoTable(ticketId);
            }
        });

        // === Button Actions ===
        btnLoadManifest.addActionListener(e -> UiTaskRunner.runAsync(statusPanel, () -> {
            try {
                DispatchHeader hdr = new DispatchHeader();
                hdr.setPickTicketId(Long.parseLong((String) ticketCombo.getSelectedItem()));
                hdr.setVehicleId(Long.parseLong((String) vehicleCombo.getSelectedItem()));
                hdr.setDriverId(Long.parseLong((String) driverCombo.getSelectedItem()));
                hdr.setManifestNo("M" + System.currentTimeMillis());
                hdr.setCreatedBy(currentUser);

                List<DispatchLine> lines = collectLinesFromTable();
                hdr = dispatchService.createDispatch(hdr, lines);
                currentDispatchId = hdr.getDispatchId();

                statusPanel.setSuccess("Dispatch created: ID " + currentDispatchId);
            } catch (Exception ex) {
                statusPanel.setError("Failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }));

        btnRecordDeparture.addActionListener(e -> UiTaskRunner.runAsync(statusPanel, () -> {
            try {
                DispatchHeader hdr = new DispatchHeader();
                hdr.setDepartTs(LocalDateTime.now());
                hdr.setUpdatedBy(currentUser);
                dispatchService.registerDeparture(currentDispatchId, hdr);
                statusPanel.setSuccess("Departure recorded successfully.");
            } catch (Exception ex) {
                statusPanel.setError("Failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }));

        btnRecordArrival.addActionListener(e -> UiTaskRunner.runAsync(statusPanel, () -> {
            try {
                DispatchHeader hdr = new DispatchHeader();
                hdr.setArriveTs(LocalDateTime.now());
                hdr.setUpdatedBy(currentUser);
                dispatchService.registerArrival(currentDispatchId, hdr);
                statusPanel.setSuccess("Arrival recorded successfully.");
            } catch (Exception ex) {
                statusPanel.setError("Failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }));

        setVisible(true);
    }

    // === Collect lines from table ===
    private List<DispatchLine> collectLinesFromTable() {
        List<DispatchLine> lines = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object boxIdObj = tableModel.getValueAt(i, 0);
            if (boxIdObj != null) {
                Long boxId = Long.parseLong(boxIdObj.toString());
                DispatchLine line = new DispatchLine();
                line.setBoxId(boxId);
                line.setCreatedAt(LocalDateTime.now());
                line.setCreatedBy(currentUser);
                lines.add(line);
            }
        }
        return lines;
    }

    // === Load dropdowns dynamically ===
    private void loadDropdowns() {
        UiTaskRunner.runAsync(statusPanel, () -> {
            try {
                // convert List<String> to String[]
                String[] tickets = dispatchService.findReadyTickets().toArray(new String[0]);
                String[] vehicles = dispatchService.findAvailableVehicles().toArray(new String[0]);
                String[] drivers = dispatchService.findAvailableDrivers().toArray(new String[0]);

                SwingUtilities.invokeLater(() -> {
                    ticketCombo.setModel(new DefaultComboBoxModel<>(tickets));
                    vehicleCombo.setModel(new DefaultComboBoxModel<>(vehicles));
                    driverCombo.setModel(new DefaultComboBoxModel<>(drivers));
                });

                statusPanel.setSuccess("Ready");
            } catch (Exception ex) {
                statusPanel.setError("Failed to load dropdowns: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }


    // === Load boxes into table dynamically ===
    private void loadBoxesIntoTable(Long ticketId) {
        UiTaskRunner.runAsync(statusPanel, () -> {
            try {
                List<DispatchLine> boxes = dispatchService.findBoxesForTicket(ticketId); // returns DispatchLine or Pack data

                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0);
                    for (DispatchLine box : boxes) {
                        Object[] row = new Object[] {
                                box.getBoxId(),
                                "Box " + box.getBoxId(), // description placeholder
                                0.0, // weight placeholder
                                Boolean.TRUE
                        };
                        tableModel.addRow(row);
                    }
                });
            } catch (Exception ex) {
                statusPanel.setError("Failed to load boxes: " + ex.getMessage());
                ex.printStackTrace();
            }
        });
    }
}
