package com.ccinfom.service;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.model.Product;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TicketService
 *
 * Responsibilities:
 *  - Create a new pick ticket (header + lines) as one atomic business transaction.
 *  - Enforce business rules BEFORE touching the database.
 *  - Guarantee initial lifecycle state (ticket always starts OPEN).
 *  - Guarantee audit fields (updatedBy) are set.
 *
 * High-level flow for createPickTicket():
 *  1. Normalize/sanitize input (status defaults, audit population).
 *  2. Validate business rules (quantities > 0, no duplicate products, product active, etc.).
 *  3. Begin DB transaction.
 *  4. Insert header, then insert lines bound to that header.
 *  5. Commit OR rollback.
 *
 * Notes:
 *  - DAO never decides lifecycle state.
 *  - UI is NOT allowed to set the first status; we enforce OPEN here.
 *  - We do not allow tickets with no lines.
 */
public class TicketService {

    private static final Logger logger = Logger.getLogger(TicketService.class.getName());

    private final TicketDao ticketDao;
    private final LookupDao lookupDao;

    // TODO: Replace this with the actual logged-in username / session user in later phases.
    private static final String SYSTEM_USER = "system";

    public TicketService(TicketDao ticketDao, LookupDao lookupDao) {
        this.ticketDao = ticketDao;
        this.lookupDao = lookupDao;
    }

    /**
     * Creates a new pick ticket (header + lines) in a single DB transaction.
     *
     * Business rules enforced:
     *  - Ticket must have a customer and branch.
     *  - Must have >= 1 line.
     *  - No duplicate product in the same ticket.
     *  - Quantity for each line must be > 0.
     *  - All products must be active.
     *  - Ticket always starts in OPEN state (lifecycle control).
     *
     * @param hdr   the ticket header (customer, branch, remarks, etc.)
     * @param lines the ticket detail lines (product, qty, etc.)
     * @return generated pick_ticket_id
     * @throws ValidationException if business rules fail
     * @throws SQLException        if DB insert fails for technical reasons
     */
    public long createPickTicket(PickTicketHdr hdr, List<PickTicketLine> lines)
            throws ValidationException, SQLException {

        // 1. Normalize/sanitize inputs (status, audit, etc)
        sanitizeTicketForCreate(hdr, lines);

        // 2. Enforce business rules before we even open a transaction
        validatePickTicket(hdr, lines);

        Connection conn = null;
        try {
            // 3. Start transaction
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            logger.info(() -> String.format("[T1_CREATE_TICKET][customer=%d][branch=%d] BEGIN",
                    hdr.getCustomerId(), hdr.getBranchId()));

            // 4. Insert header
            long newTicketId = ticketDao.insertTicketHeader(hdr, conn);

            // 5. Attach generated header ID to each line and insert all lines
            for (PickTicketLine line : lines) {
                line.setPickTicketId(newTicketId);
            }
            ticketDao.insertTicketLines(lines, conn);

            // 6. Commit transaction
            conn.commit();
            logger.info(() -> String.format("[T1_CREATE_TICKET][ticket=%d] SUCCESS", newTicketId));

            return newTicketId;

        } catch (SQLException e) {
            // 7. Rollback on any problem
            safeRollback(conn);
            logger.log(Level.SEVERE,
                    String.format("[T1_CREATE_TICKET][customer=%d][branch=%d] FAILED: %s",
                            hdr.getCustomerId(), hdr.getBranchId(), e.getMessage()),
                    e);
            throw e; // Let caller decide how to present this (UI may show generic failure)

        } finally {
            // 8. Cleanup
            restoreAndClose(conn);
        }
    }

    // ---------------------------------------------------------------------
    // Input preparation (defaults / safety)
    // ---------------------------------------------------------------------

    /**
     * Ensures required defaults are set BEFORE validation and insert.
     * - Force initial status to OPEN (UI cannot override this).
     * - Ensure updatedBy is present on header and lines.
     *
     * This protects the data model and audit requirements.
     */
    private void sanitizeTicketForCreate(PickTicketHdr hdr, List<PickTicketLine> lines) {
        if (hdr == null) {
            return; // validatePickTicket will still catch null later
        }

        // Lifecycle rule: ALL new tickets begin as OPEN
        hdr.setTicketStatus(PickTicketHdr.TicketStatus.Open);

        // Audit rule: updatedBy must never be null/blank
        if (hdr.getUpdatedBy() == null || hdr.getUpdatedBy().isBlank()) {
            hdr.setUpdatedBy(SYSTEM_USER);
        }
        // Normalize promised date (no-op if null)
        if (hdr.getPromisedDeliveryDate() != null) {
            hdr.setPromisedDeliveryDate(hdr.getPromisedDeliveryDate());
        }

        if (lines != null) {
            for (PickTicketLine line : lines) {
                // Lifecycle rule: ALL new lines begin as VALID
                line.setLineStatus(PickTicketLine.LineStatus.Valid);
                if (line.getUpdatedBy() == null || line.getUpdatedBy().isBlank()) {
                    line.setUpdatedBy(hdr.getUpdatedBy()); // inherit same user
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // Business validation (friendly errors → ValidationException)
    // ---------------------------------------------------------------------

    /**
     * Validates that the ticket header and its lines follow all required
     * business rules, before we touch the database.
     *
     * We throw ValidationException for anything that the user can fix.
     */
    private void validatePickTicket(PickTicketHdr hdr, List<PickTicketLine> lines)
            throws ValidationException, SQLException {

        // Basic presence checks
        if (hdr == null || lines == null) {
            throw new ValidationException("Header and lines cannot be null.");
        }

        if (hdr.getCustomerId() == null || hdr.getBranchId() == null) {
            throw new ValidationException("Customer and Branch are required.");
        }

        // Promised delivery date must be present and not in the past
        if (hdr.getPromisedDeliveryDate() == null) {
            throw new ValidationException("Promised delivery date is required.");
        }
        if (hdr.getPromisedDeliveryDate().isBefore(java.time.LocalDate.now())) {
            throw new ValidationException("Promised delivery date cannot be in the past.");
        }

        if (lines.isEmpty()) {
            throw new ValidationException("At least one item line is required.");
        }

        // Validate each line
        Set<Long> productIdsInTicket = new HashSet<>();

        for (PickTicketLine line : lines) {

            // Quantity > 0
            BigDecimal qty = line.getRequestedQty();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Item quantity must be greater than zero.");
            }

            // No duplicate products
            Long productId = line.getProductId();
            if (productId == null) {
                throw new ValidationException("Product is required on each line.");
            }
            if (!productIdsInTicket.add(productId)) {
                throw new ValidationException("Duplicate product found in the ticket (product_id=" + productId + ").");
            }

            // Product must exist and be active
            Product product = lookupDao.findProductById(productId);
            if (product == null || !product.isActiveFlag()) {
                throw new ValidationException(
                    "Product with ID " + productId + " is invalid or inactive."
                );
            }
        }

        // OPTIONAL FUTURE IMPROVEMENT: Validate that hdr.getCustomerId() and hdr.getBranchId()
        // actually correspond to valid rows (customers / branches).
        // In Phase D we rely on DB foreign keys for that.
    }

    // ---------------------------------------------------------------------
    // Read helpers exposed to UI layer
    // ---------------------------------------------------------------------

    /**
     * Loads ticket lines after ensuring the ticket exists. Keeps the UI away from
     * direct DAO calls so business rules can evolve here.
     *
     * @param pickTicketId target ticket identifier
     * @return list of lines bound to the ticket
     * @throws ValidationException if the ticket id is invalid or missing
     * @throws SQLException        if a database error occurs during retrieval
     */
    public List<PickTicketLine> listTicketLines(long pickTicketId)
            throws ValidationException, SQLException {

        if (pickTicketId <= 0) {
            throw new ValidationException("Ticket id must be positive.");
        }

        PickTicketHdr ticket = ticketDao.findTicketById(pickTicketId);
        if (ticket == null) {
            throw new ValidationException("Ticket not found.");
        }

        return ticketDao.listTicketLines(pickTicketId);
    }

    // ---------------------------------------------------------------------
    // Connection helpers
    // ---------------------------------------------------------------------

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
     * Reset auto-commit to true (defensive) and then close.
     * We always try both steps, but we never throw here.
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
}
