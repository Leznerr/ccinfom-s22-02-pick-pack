package com.ccinfom.service;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.infra.InventoryHelper;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PickingService
 *
 * Responsibilities:
 *  - Start a picking session for an existing pick ticket (assign a picker).
 *  - Save picked item lines for an active picking session.
 *  - Enforce business rules around picking lifecycle.
 *
 * Key rules:
 *  - You can only start picking if the ticket status is OPEN.
 *  - Starting picking will:
 *      (a) create a picking_hdr row
 *      (b) update the ticket_hdr status to PICKING
 *      as one transaction.
 *  - You cannot start picking twice for the same ticket.
 *  - Picked quantities must be valid (handled by DB + translated here).
 *
 * All SQL/state changes are wrapped in transactions with explicit commit/rollback.
 * The DAO layer never decides status or transaction boundaries.
 */
public class PickingService {

    private static final Logger logger = Logger.getLogger(PickingService.class.getName());

    private final PickingDao pickingDao;
    private final TicketDao ticketDao;
    private final LookupDao lookupDao;
    private final InventoryHelper inventoryHelper;

    // TODO in future: inject current user / session user instead of hardcoding
    private static final String SYSTEM_USER = "system";

    public PickingService(PickingDao pickingDao, TicketDao ticketDao, LookupDao lookupDao) {
        this(pickingDao, ticketDao, lookupDao, new InventoryHelper());
    }

    public PickingService(PickingDao pickingDao,
                          TicketDao ticketDao,
                          LookupDao lookupDao,
                          InventoryHelper inventoryHelper) {
        if (pickingDao == null || ticketDao == null || lookupDao == null || inventoryHelper == null) {
            throw new IllegalArgumentException("DAO and helper dependencies must not be null.");
        }
        this.pickingDao = pickingDao;
        this.ticketDao = ticketDao;
        this.lookupDao = lookupDao;
        this.inventoryHelper = inventoryHelper;
    }

    /**
     * Assigns a picker to a ticket and transitions the ticket into PICKING state.
     * Creates a picking_hdr record.
     *
     * This is Phase D's "start picking" action.
     *
     * @param pickTicketId      the ticket we're picking
     * @param pickerEmployeeId  the employee assigned to pick
     * @throws ValidationException if business rules fail
     * @throws SQLException        if the DB rejects the operation
     */
    public void assignPickerAndStartPicking(long pickTicketId, long pickerEmployeeId)
            throws ValidationException, SQLException {

        // ---- 1. VALIDATE INPUT BUSINESS RULES ----

        // Check that ticket exists
        PickTicketHdr ticket = ticketDao.findTicketById(pickTicketId);
        if (ticket == null) {
            throw new ValidationException("Ticket not found.");
        }

        // Only OPEN tickets can be moved to PICKING
        if (ticket.getTicketStatus() != PickTicketHdr.TicketStatus.Open) {
            throw new ValidationException("Ticket is not OPEN and cannot be picked.");
        }

        // Optional: confirm picker is valid and active
        // e.g. lookupDao.listActivePickers() and ensure pickerEmployeeId is in there
        // Skipped for now, but leave the hook:
        // validatePickerExists(pickerEmployeeId);

        // Prevent duplicate picking sessions for the same ticket
        // (If a picking_hdr for this ticket already exists, don't create another)
        PickingHdr existingPicking = pickingDao.findByTicketId(pickTicketId);
        if (existingPicking != null) {
            throw new ValidationException("This ticket is already assigned to a picker.");
        }

        // ---- 2. EXECUTE STATE TRANSITION IN ONE TRANSACTION ----

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);

            // Build picking header record
            PickingHdr newPickingHdr = new PickingHdr();
            newPickingHdr.setPickTicketId(pickTicketId);
            newPickingHdr.setPickerEmployeeId(pickerEmployeeId);

            // NOTE: ideally this is an enum like PickingHdr.PickingStatus.PICKING
            // If your model still uses String, this will remain a String.
            newPickingHdr.setPickingStatus("Picking");

            newPickingHdr.setUpdatedBy(SYSTEM_USER);

            // 2a. Insert picking header
            pickingDao.insertPickingHeader(newPickingHdr, conn);

            // 2b. Update ticket status from OPEN -> PICKING
            ticketDao.updateTicketStatus(
                    pickTicketId,
                    PickTicketHdr.TicketStatus.Picking,
                    SYSTEM_USER,
                    conn // <-- Ensure we use the SAME connection for the transaction
            );

            conn.commit();
            logger.info("Picking started for ticket " + pickTicketId +
                        " with picker employee " + pickerEmployeeId);

        } catch (SQLException e) {
            // Roll back everything if any step failed
            safeRollback(conn);
            logger.log(Level.SEVERE,
                    "Failed to start picking for ticket " + pickTicketId + ": " + e.getMessage(), e);
            throw e;
        } finally {
            restoreAndClose(conn);
        }
    }

    /**
     * Saves picked item lines (picking_line rows) for a given picking session.
     *
     * @param pickingId     active picking_hdr.picking_id
     * @param pickedItems   list of PickingLine rows to insert
     * @throws ValidationException if business rules fail or DB triggers reject data
     * @throws SQLException        if a non-business DB error occurs
     */
    public void savePickedItems(long pickingId, List<PickingLine> pickedItems)
            throws ValidationException, SQLException {

        // ---- 1. BASIC VALIDATION ----
        if (pickedItems == null || pickedItems.isEmpty()) {
            throw new ValidationException("Picked items list cannot be empty.");
        }

        // Ensure updatedBy is filled (audit requirement)
        for (PickingLine line : pickedItems) {
            if (line.getUpdatedBy() == null || line.getUpdatedBy().isBlank()) {
                line.setUpdatedBy(SYSTEM_USER);
            }
        }

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);

            long ticketId = loadTicketId(conn, pickingId);

            // ---- 2. EXECUTE INSERT ----
            pickingDao.insertPickingLines(pickingId, pickedItems, conn);

            for (PickingLine line : pickedItems) {
                if (line.getPickingLineId() == null) {
                    throw new SQLException("Picking line ID not generated for ticket_line_id=" + line.getTicketLineId());
                }
                String sourceRef = determineReserveSourceRef(pickingId, line);
                inventoryHelper.reserve(
                        conn,
                        line.getProductId(),
                        line.getPickingLineId(),
                        ticketId,
                        line.getPickedQty(),
                        sourceRef,
                        line.getUpdatedBy()
                );
            }

            conn.commit();
            logger.info("Saved " + pickedItems.size() +
                        " picked line(s) for picking_id=" + pickingId);

        } catch (ValidationException e) {
            safeRollback(conn);
            throw e;
        } catch (SQLException e) {
            safeRollback(conn);

            logger.log(Level.WARNING,
                    "Error while saving picked items for picking_id=" + pickingId
                            + ": " + e.getMessage(), e);

            // ---- 3. TRANSLATE TECHNICAL DB ERRORS INTO USER MESSAGES ----
            String dbErrorMessage = e.getMessage() != null
                    ? e.getMessage().toLowerCase()
                    : "";

            if (dbErrorMessage.contains("picked qty exceeds requested")) {
                throw new ValidationException(
                    "Save failed: Picked quantity exceeds requested quantity."
                );
            } else if (dbErrorMessage.contains("insufficient available stock")
                    || dbErrorMessage.contains("negative reserved_qty")
                    || dbErrorMessage.contains("negative on_hand_qty")) {
                throw new ValidationException(
                    "Save failed: Not enough available stock for at least one item."
                );
            } else if (dbErrorMessage.contains("product is inactive")) {
                throw new ValidationException(
                    "Save failed: One of the items is inactive and cannot be picked."
                );
            }

            // Anything else is a real SQL exception
            throw e;
        } finally {
            restoreAndClose(conn);
        }
    }

    // ------------------------------------------------------------
    // Internal helpers for connection safety and clarity
    // ------------------------------------------------------------

    /**
     * Attempt to roll back if we're mid-transaction.
     */
    private void safeRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException rollbackErr) {
                logger.log(Level.SEVERE, "Rollback failed: " + rollbackErr.getMessage(), rollbackErr);
            }
        }
    }

    /**
     * Reset auto-commit to true (defensive) and close the connection.
     */
    private void restoreAndClose(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to reset auto-commit: " + e.getMessage(), e);
            }
            try {
                conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Failed to close connection: " + e.getMessage(), e);
            }
        }
    }

    private long loadTicketId(Connection conn, long pickingId) throws SQLException, ValidationException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT pick_ticket_id FROM picking_hdr WHERE picking_id = ?")) {
            ps.setLong(1, pickingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new ValidationException("Picking session not found.");
    }

    private String determineReserveSourceRef(long pickingId, PickingLine line) {
        if (line.getScanRef() != null && !line.getScanRef().isBlank()) {
            return line.getScanRef();
        }
        if (line.getPickingLineId() != null) {
            return "T2-" + pickingId + "-" + line.getPickingLineId();
        }
        return "T2-" + pickingId + "-" + line.getTicketLineId();
    }
}
