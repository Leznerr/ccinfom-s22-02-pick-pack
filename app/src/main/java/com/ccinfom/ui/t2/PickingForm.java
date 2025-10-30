package com.ccinfom.ui.t2;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
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
import com.ccinfom.ui.t1.TicketForm;


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
    private JComboBox<TicketForm.ComboItem<com.ccinfom.model.Employee>> pickerBox;
    private JTable linesTable;
    private TicketForm.LineTableModel tableModel;
    private JButton startPickingButton;
    private JButton saveResults;


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
        setLayout(new BorderLayout());


        // TODO: Member 4 - INITIAL DATA LOAD
        // - Call loadPickersAndTickets(); this should use SwingWorker to prevent UI freeze.
        //     * lookupDao.listActivePickers() => populate picker combo.
        //     * ticketService.listOpenTickets() (add helper that filters ticketDao.listAllTickets()).
        // - After load completes, enable combos and preselect the first entries if available.

        // TODO: Member 4 - STATE VARIABLES
        // - Track Long currentPickingId (null until assignPickerAndStartPicking succeeds).
        // - Track PickTicketHdr currentTicket (for validations / refresh).
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

    // TODO: Member 4 - DATA HELPERS
    // - void loadPickersAndTickets():
    //     * Use SwingWorker; disable combos while loading.
    //     * Handle SQLException by showing an error dialog and leaving controls disabled for retry.

    private void loadPickersAndTickets(){
        SwingWorker<Void, Void> worker = new SwingWorker<>(){
            private List<com.ccinfom.model.Employee> pickers;
            private List<PickTicketHdr> tickets;
            private Exception error;
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    pickers = lookupdao.listActivePickers(com.ccinfom.model.Employee);//package location of employee issue here
                    tickets = ticketDao.listAllTickets();
                } catch (SQLException e) {
                    error = e;
                }
                return null;
            }
            @Override
            protected void done() {
                if(error == null){
                    JOptionPane.showMessageDialog(
                            PickingForm.this,
                            "Failed to load data" + error.getMessage(),
                            "Database Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                DefaultComboBoxModel<TicketForm.ComboItem<com.ccinfom.model.Employee>> pickerModel = new  DefaultComboBoxModel<>();
                for(com.ccinfom.model.Employee emp : pickers){
                    pickerModel.addElement(new TicketForm.ComboItem<>(emp.getEmployeeId(),//same here combo item also borke; method in ticketform allegedly in private                                                                emp.getFirstName()+""+emp.getLastName(),emp
                                                                        ));
                }
                pickerBox.setModel(pickerModel);

                DefaultComboBoxModel<TicketForm.ComboItem<PickTicketHdr>> ticketModel = new   DefaultComboBoxModel<>();
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

    }
    // TODO: Member 4 - TABLE MODEL
    // - Create an inner class LineTableModel extends AbstractTableModel similar to TicketForm but with editable column.
    // - Override isCellEditable to allow edits only on the picked quantity column.
    // - Provide helper methods:
    //       setLines(List<PickTicketLine>),
    //       List<LineEntry> getLines(),
    //       void clear().

    // TODO: Member 4 - ERROR HANDLING
    // - Wrap service calls in SwingWorker to keep UI responsive.
    // - Show ValidationException messages via JOptionPane.WARNING_MESSAGE.
    // - Show SQLException messages via JOptionPane.ERROR_MESSAGE and log to System.err for Phase D.
    // - Ensure buttons are disabled during background work and re-enabled in done().

    // TODO: Member 4 - MAIN METHOD FOR STANDALONE TEST
    // - Provide:
    //       public static void main(String[] args) {
    //           SwingUtilities.invokeLater(() -> {
    //               PickingForm form = new PickingForm();
    //               form.setVisible(true);
    //           });
    //       }
    // - Optional: call DbConnection.ping() before showing to print a friendly message if config is missing.
}
