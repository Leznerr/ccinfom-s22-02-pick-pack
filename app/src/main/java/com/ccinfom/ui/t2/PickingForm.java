package com.ccinfom.ui.t2;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.service.TicketService;
import com.ccinfom.ui.t1.TicketForm;
import com.ccinfom.model.Employee;

import java.awt.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

/**
 * CCINFOM Phase D
 *
 * Swing shell for Transaction T2 (Allocate & Pick).
 * This file intentionally contains TODO guides for Member 4 so the screen
 * stays aligned with our TicketForm implementation and service layer rules.
 */
public class PickingForm extends JFrame {

    // TODO: Member 4 - UI COMPONENTS
    // - Declare Swing fields with domain aware types:
    //     JComboBox<ComboItem<PickTicketHdr>> ticketComboBox   (label: ticket no + customer + branch)
    //     JComboBox<ComboItem<Employee>>      pickerComboBox   (active pickers only)
    //     JTable                              linesTable       (requested vs picked quantities)
    //     LineTableModel                      tableModel       (columns: Product ID, SKU, Name, Requested Qty, Picked Qty [editable], UOM)
    //     JButton                             startPickingButton (assign picker + create picking_hdr)
    //     JButton                             saveResultsButton  (persist picked quantities)
    //     Optional: JLabel or status footer showing current picking_id and validation messages.
    //
    // - Use helper ComboItem similar to TicketForm so combo boxes keep both id and label.
    private JComboBox<TicketForm.ComboItem<PickTicketHdr>> ticketComboBox;
    private JComboBox<TicketForm.ComboItem<Employee>> pickerBox;
    private JTable linesTable;
    private TicketForm.LineTableModel tableModel;
    private JButton startPickingButton;
    private JButton saveResults;
    private JLabel statusLabel;

    // TODO: Member 4 - SERVICE & DAO WIRING
    // - Add fields:
    //     private final PickingService pickingService;
    //     private final TicketService  ticketService;
    //     private final LookupDao      lookupDao;
    // - Provide:
    //     1) Default constructor that instantiates LookupDaoImpl, TicketDaoImpl, PickingDaoImpl,
    //        then wires ticketService and pickingService (mirroring TicketForm).
    //     2) Package-private constructor accepting these dependencies (supports UI tests).
    private final LookupDao lookupdao;
    private final TicketDao ticketDao;
    private final com.ccinfom.dao.interfaces.PickingDao pickingDao ;
    private final TicketService ticketService;
    private final com.ccinfom.service.PickingService pickingService;

    private Long currentPickingId;
    private PickTicketHdr currentTicket;

    public PickingForm() {
        // TODO: Member 4 - DEFAULT CONSTRUCTOR BODY
        // - Instantiate LookupDaoImpl, TicketDaoImpl, PickingDaoImpl.
        // - Build ticketService = new TicketService(ticketDao, lookupDao).
        // - Build pickingService = new PickingService(pickingDao, ticketDao, lookupDao).
        // - Delegate to an overloaded constructor to avoid duplicate setup logic.

        this.lookupdao = new LookupDaoImpl();
        this.ticketDao = new TicketDaoImpl();
        this.pickingDao = new PickingDaoImpl();
        this.ticketService = new TicketService(ticketDao,lookupdao);
        this.pickingService = new com.ccinfom.service.PickingService(pickingDao,ticketDao,lookupdao);

        // TODO: Member 4 - FRAME SETUP
        // - setTitle("T2: Picking Process");
        // - setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        // - setLayout(new BorderLayout()) and call helper methods:
        //       initComponents();
        //       layoutComponents();
        //       registerListeners();
        // - Follow TicketForm's GridBagLayout approach for consistency.
        setTitle("T2: Picking process");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800,600);//frame size x y


        initComponents();
        layoutComponents();
        registerListeners();


        // TODO: Member 4 - INITIAL DATA LOAD
        // - Call loadPickersAndTickets(); this should use SwingWorker to prevent UI freeze.
        //     * lookupDao.listActivePickers() => populate picker combo.
        //     * ticketService.listOpenTickets() (add helper that filters ticketDao.listAllTickets()).
        // - After load completes, enable combos and preselect the first entries if available.
        loadPickersAndTickets();
        // TODO: Member 4 - STATE VARIABLES
        // - Track Long currentPickingId (null until assignPickerAndStartPicking succeeds).
        // - Track PickTicketHdr currentTicket (for validations / refresh).
    }
    private void initComponents(){
        ticketComboBox = new JComboBox<>();
        pickerBox = new JComboBox<>();
        tableModel = new TicketForm.LineTableModel();
        linesTable = new JTable(tableModel);
        startPickingButton = new JButton("Start Picking");
        saveResults = new JButton("Save Results");
        statusLabel = new JLabel("STATUS: READY");

        ticketComboBox.setEnabled(false);
        pickerBox.setEnabled(false);
        startPickingButton.setEnabled(false);
        saveResults.setEnabled(false);
    }

    private void layoutComponents(){
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5,5,5,5);
        c.fill = GridBagConstraints.HORIZONTAL;

        //ticket
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(new JLabel("TICKET: "), c);


        c.gridx = 1;
        c.weightx = 1.0;
        mainPanel.add(ticketComboBox, c);

        //picker
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        mainPanel.add(new JLabel("PICKER: "), c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        mainPanel.add(pickerBox, c);

        //start
        c.gridx = 0;
        c.weightx = 0;
        mainPanel.add(startPickingButton, c);

        //table
        c.gridx = 3;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        mainPanel.add(new JScrollPane(linesTable), c);

        //save
        c.gridy = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        mainPanel.add(saveResults, c);

        //status
        c.gridy = 5;
        mainPanel.add(statusLabel, c);

        add(mainPanel,BorderLayout.CENTER);
    }

    // TODO: Member 4 - EVENT HANDLERS
    // - ticketComboBox listener:
    //     * When selection changes, call loadTicketLines(selectedTicketId).
    //     * If a picking session already exists (pickingService via pickingDao.findByTicketId),
    //       store the picking_id and disable start button.
    // - pickerComboBox listener:
    //     * Optional: display picker details in a label.
    // - startPickingButton:
    //     * Validate ticket + picker chosen.
    //     * Invoke pickingService.assignPickerAndStartPicking(ticketId, pickerId).
    //     * After success, fetch picking_id, disable ticket combo, enable save button, show success dialog.
    //     * Catch ValidationException (show JOptionPane warning) and SQLException (error dialog) and re-enable controls.
    // - saveResultsButton:
    //     * Read edited values from table model (collectPickedLines()).
    //     * Ensure pickedQty >= 0 and <= requestedQty per line before calling service.
    //     * Call pickingService.savePickedItems(currentPickingId, pickedLines).

    //     * On success, show confirmation, refresh ticket list (removing the ticket if fully picked), clear table.
    private void registerListeners() {
        ticketComboBox.addActionListener(e -> onTicketSelected());
        startPickingButton.addActionListener(e -> onStartPicking());
        saveResults.addActionListener(e -> onSaveResults);
    }

    private void onTicketSelected(){
        TicketForm.ComboItem<PickTicketHdr> selected = (TicketForm.ComboItem<PickTicketHdr>) ticketComboBox.getSelectedItem();
        if(selected!=null){
            //currentTicket = selected.getData();
            loadTicketLines(currentPickingId);
        }
    }

    private void onStartPicking(){
        TicketForm.ComboItem<PickTicketHdr> ticketItem =
                (TicketForm.ComboItem<PickTicketHdr>) ticketComboBox.getSelectedItem();
        TicketForm.ComboItem<Employee> pickerItem =
                (TicketForm.ComboItem<Employee>) pickerBox.getSelectedItem();

        if(pickerItem ==null || pickerItem == null){
            JOptionPane.showMessageDialog(this, "Please select BOTH ticket and picker",
                    "ERROR: Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        }
        startPickingButton.setEnabled(false);
        statusLabel.setText("STATUS: starting picking");

        SwingWorker<Void, Void> worker = new SwingWorker<>(){
            private Exception error;

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    pickingService.savePickedItems(currentPickingId);
                }catch (Exception e){
                    error = e;
                }
                return null;
            }
        };
    }

    // TODO: Member 4 - DATA HELPERS
    // - void loadPickersAndTickets():
    //     * Use SwingWorker; disable combos while loading.
    //     * Handle SQLException by showing an error dialog and leaving controls disabled for retry.

    private void loadPickersAndTickets(){
        SwingWorker<Void, Void> worker = new SwingWorker<>(){
            private List<Employee> pickers;
            private List<PickTicketHdr> tickets;
            private Exception error;
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    pickers = lookupdao.listActivePickers();//package location of employee issue here; lowk dk what the issue is anymore
                    tickets = ticketDao.listAllTickets();
                } catch (SQLException e) {
                    error = e;
                }
                return null;
            }
            @Override
            protected void done() {
                if(error != null){
                    JOptionPane.showMessageDialog(
                            PickingForm.this,
                            "Failed to load data" + error.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                DefaultComboBoxModel<TicketForm.ComboItem<Employee>> pickerModel
                        = new  DefaultComboBoxModel<>();
                for(Employee emp : pickers){
                    pickerModel.addElement(new TicketForm.ComboItem<>(emp.getEmployeeId(),
                            emp.getFirstName()+""+emp.getLastName(),
                            emp));
                }
                pickerBox.setModel(pickerModel);

                DefaultComboBoxModel<TicketForm.ComboItem<PickTicketHdr>> ticketModel
                        = new DefaultComboBoxModel<>();
                for(PickTicketHdr ticket : tickets){
                    if ("OPEN".equals(ticket.getTicketStatus())){
                        ticketModel.addElement(new TicketForm.ComboItem<>(ticket.getPickTicketId(),//comboitem method kinda broke atm fix later
                                "TICKET NO: " + ticket.getPickTicketId() +
                                " - CUSTOMER: " + ticket.getCustomerId() +
                                " - BRANCH: " + ticket.getBranchId(),
                                ticket
                        ));
                    }
                }
                ticketComboBox.setModel(ticketModel);

                ticketComboBox.setEnabled(true);
                pickerBox.setEnabled(true);
                startPickingButton.setEnabled(ticketModel.getSize() > 0 &&
                        pickerModel.getSize() > 0);
            }
        };
        worker.execute();
    }
    // - void loadTicketLines(long ticketId):
    //     * Fetch PickTicketLine list via ticketService.listTicketLines(ticketId).
    //     * Optionally join product metadata via lookupDao.findProductById to display SKU/name/UOM.
    //     * Reset table model rows with requested qty defaulting picked qty equal to requested qty (user may adjust down).
    // - List<PickingLine> collectPickedLines():
    //     * Convert table rows into PickingLine objects (set ticketLineId, productId, qtyPicked, updatedBy).
    //     * Use the same audit user as TicketForm (System.getProperty("user.name", "ui-operator")) until login is available.
    private void loadTicketLines(long ticketId){
        tableModel.clear();
        statusLabel.setText("STATUS: loading ticket lines");
        SwingWorker<Void, Void> worker = new SwingWorker<>(){
            private List<PickTicketLine> lines;
            private Exception error;
            @Override
            protected Void doInBackground(){
                try {
                    lines = ticketService.listTicketLines(currentTicket.getPickTicketId());//missing method(?)
                }catch (Exception e){
                    error = e;
                };
                return null;
            }
            @Override
            protected void done() {
                if(error != null){
                    JOptionPane.showMessageDialog(PickingForm.this,
                            "ERROR LOADING TICKET LINES: " +
                                    error.getMessage(), "ERROR",
                            JOptionPane.ERROR_MESSAGE);
                    //statusLabel.setText("Status: ERROR LOADING LINES");
                }
                List<TicketForm.LineEntry> entries = new ArrayList<>();
                for(PickTicketLine line : lines){
                    try{
                        com.ccinfom.model.Product product = lookupdao.findProductById(line.getProductId());//package import issue also here
                        entries.add(new TicketForm.LineEntry(line.getTicketLineId(),//package import issue here
                                line.getProductId(),
                                product != null ? product.getSku() : "N/A",
                                product != null ? product.getProductName() : "Unknown",
                                line.getRequestedQty(),
                                line.getRequestedQty(),
                                product != null ? product.getUnitOfMeasure() : "EA"

                        ));
                    }catch(Exception e){
                        System.err.println("ERROR: error loading product" + line.getProductId() + ": " + e.getMessage());
                    }
                }
                tableModel.addLine(entries);
                statusLabel.setText("STATUS: READY");
            }
        };
        worker.execute();
    }
    }
    // TODO: Member 4 - TABLE MODEL
    // - Create an inner class LineTableModel extends AbstractTableModel similar to TicketForm but with editable column.
    // - Override isCellEditable to allow edits only on the picked quantity column.
    // - Provide helper methods:
    //       setLines(List<PickTicketLine>),
    //       List<LineEntry> getLines(),
    //       void clear().

    private static class LineTableModel extends AbstractTableModel{
    private final String[] columnNames = {
            "Product ID","SKU","Name","Requested QTY","Picked QTY", "UOM"
    };
        private List<TicketForm.LineEntry> lines = new ArrayList<>();

        @Override
        public int getRowCOunt() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            TicketForm.LineEntry entry = lines.get(rowIndex);
            switch(columnIndex){
                case 0: return entry.getProductId();
                case 1: return entry.getSku();
                case 2: return entry.getProductName();
                case 3: return  entry.getQuantity();
                case 4: return entry.getUom();
                default: return null;
            }
        }
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 4;
        }
        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if(columnIndex == 4){
                try{
                    int pickedQty = Integer.parseInt(value.toString());
                    lines.get(rowIndex).getQuantity();
                }catch(NumberFormatException e){
                    //ignore
                }
            }
        }
        public void setLines(List<TicketForm.LineEntry> lines){
            this.lines = new ArrayList<>(lines);
            fireTableDataChanged();
        }
        public List<TicketForm.LineEntry> getLines(){
            return new ArrayList<>(lines);
        }
        public void clear(){
            lines.clear();
            fireTableDataChanged();
        }
    }

    static class LineEntry{
        long ticketLineId;
        long productId;
        String sku;
        String productName;
        int quantity;
        String uom;

        LineEntry(long ticketLineId, long productId, String sku, String productName, int quantity, String uom){
            this.ticketLineId = ticketLineId;
            this.productId = productId;
            this.sku = sku;
            this.productName = productName;
            this.quantity = quantity;
            this.uom = uom;
        }
    }

    // TODO: Member 4 - ERROR HANDLING
    // - Wrap service calls in SwingWorker to keep UI responsive.
    // - Show ValidationException messages via JOptionPane.WARNING_MESSAGE.
    // - Show SQLException messages via JOptionPane.ERROR_MESSAGE and log to System.err for Phase D.
    // - Ensure buttons are disabled during background work and re-enabled in done().

    // TODO: Member 4 - MAIN METHOD FOR STANDALONE TEST
    // - Provide:
           public static void main(String[] args) {
               SwingUtilities.invokeLater(() -> {
                  PickingForm form = new PickingForm();
                   form.setVisible(true);
               });
           }
    // - Optional: call DbConnection.ping() before showing to print a friendly message if config is missing.
}
