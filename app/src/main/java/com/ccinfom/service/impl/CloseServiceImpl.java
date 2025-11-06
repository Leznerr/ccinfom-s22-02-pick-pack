package com.ccinfom.service.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.CloseDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.infra.InventoryHelper;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import com.ccinfom.service.CloseService;
import com.ccinfom.service.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloseServiceImpl implements CloseService {

    private static final Logger LOGGER = Logger.getLogger(CloseServiceImpl.class.getName());
    private static final String SYSTEM_USER = "system";

    private final CloseDao closeDao;
    private final TicketDao ticketDao;
    private final InventoryHelper inventoryHelper;

    public CloseServiceImpl(CloseDao closeDao,
                            TicketDao ticketDao,
                            InventoryHelper inventoryHelper) {
        this.closeDao = closeDao;
        this.ticketDao = ticketDao;
        this.inventoryHelper = inventoryHelper;
    }

    @Override
    public void closeTicket(CloseHeader header, List<CloseVariance> variances)
            throws SQLException, ValidationException {

        validateCloseRequest(header, variances);

        String actor = header.getCreatedBy();
        if (actor == null || actor.isBlank()) {
            actor = SYSTEM_USER;
            header.setCreatedBy(actor);
        }
        if (header.getUpdatedBy() == null || header.getUpdatedBy().isBlank()) {
            header.setUpdatedBy(actor);
        }

        Map<Long, PickTicketLine> ticketLineMap = loadTicketLines(header.getPickTicketId());

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);

            if (closeDao.findByTicketId(header.getPickTicketId(), conn).isPresent()) {
                throw new ValidationException("CLOSE_ALREADY_EXISTS",
                        "Ticket " + header.getPickTicketId() + " already has a close record.");
            }

            long closeId = closeDao.insertCloseHeader(header, conn);

            for (CloseVariance variance : variances) {
                variance.setCloseId(closeId);
                if (variance.getCreatedBy() == null || variance.getCreatedBy().isBlank()) {
                    variance.setCreatedBy(actor);
                }
                if (variance.getUpdatedBy() == null || variance.getUpdatedBy().isBlank()) {
                    variance.setUpdatedBy(variance.getCreatedBy());
                }
            }

            closeDao.insertCloseVariances(closeId, variances, conn);

            for (CloseVariance variance : variances) {
                PickTicketLine ticketLine = ticketLineMap.get(variance.getTicketLineId());
                if (ticketLine == null) {
                    throw new ValidationException("CLOSE_LINE_UNKNOWN",
                            "Ticket line not found: " + variance.getTicketLineId());
                }

                BigDecimal deltaReserved = variance.getDeliveredQty()
                        .add(variance.getShortQty())
                        .negate();
                BigDecimal deltaOnHand = variance.getDeliveredQty().negate();
                String varianceRef = variance.getSourceRef();
                if (varianceRef == null || varianceRef.isBlank()) {
                    varianceRef = "T5-" + closeId + "-" + variance.getTicketLineId();
                }

                inventoryHelper.applyCloseAdjustment(
                        conn,
                        ticketLine.getProductId(),
                        closeId,
                        header.getPickTicketId(),
                        deltaReserved,
                        deltaOnHand,
                        "T5 Close",
                        varianceRef,
                        actor
                );
            }

            PickTicketHdr.TicketStatus status = header.getFinalStatus() == CloseHeader.FinalStatus.Delivered
                    ? PickTicketHdr.TicketStatus.Delivered
                    : PickTicketHdr.TicketStatus.ShortClosed;

            ticketDao.updateTicketStatus(header.getPickTicketId(), status, actor, conn);

            conn.commit();
        } catch (SQLException ex) {
            safeRollback(conn);
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("lock wait timeout") || message.contains("deadlock")) {
                throw new ValidationException("CLOSE_INVENTORY_LOCK_TIMEOUT",
                        "Inventory is locked by another transaction. Please retry.");
            }
            LOGGER.log(Level.SEVERE, "Failed to close ticket", ex);
            throw ex;
        } catch (ValidationException ex) {
            safeRollback(conn);
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public CloseHeader getCloseSummary(long pickTicketId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            Optional<CloseHeader> summary = closeDao.findByTicketId(pickTicketId, conn);
            return summary.orElse(null);
        }
    }

    @Override
    public List<CloseVariance> getVariances(long closeId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return closeDao.listVariancesByCloseId(closeId, conn);
        }
    }

    private void validateCloseRequest(CloseHeader header, List<CloseVariance> variances)
            throws ValidationException {
        if (header == null) {
            throw new ValidationException("CLOSE_HDR_REQUIRED", "Close header is required.");
        }
        if (header.getPickTicketId() == null) {
            throw new ValidationException("CLOSE_TICKET_REQUIRED", "pick_ticket_id is required.");
        }
        if (header.getDispatchId() == null) {
            throw new ValidationException("CLOSE_DISPATCH_REQUIRED", "dispatch_id is required.");
        }
        if (header.getFinalStatus() == null) {
            throw new ValidationException("CLOSE_STATUS_REQUIRED", "Final status is required.");
        }
        if (variances == null || variances.isEmpty()) {
            throw new ValidationException("CLOSE_VARIANCE_REQUIRED", "At least one variance record is required.");
        }

        for (CloseVariance variance : variances) {
            if (variance.getTicketLineId() == null) {
                throw new ValidationException("CLOSE_VARIANCE_LINE_REQUIRED", "ticket_line_id is required.");
            }
            if (variance.getRequestedQty() == null
                    || variance.getDeliveredQty() == null
                    || variance.getShortQty() == null) {
                throw new ValidationException("CLOSE_VARIANCE_QTY_REQUIRED",
                        "requested, delivered, and short quantities are required.");
            }
            BigDecimal deliveredPlusShort = variance.getDeliveredQty().add(variance.getShortQty());
            if (variance.getRequestedQty().compareTo(deliveredPlusShort) != 0) {
                throw new ValidationException("CLOSE_RECONCILE_MISMATCH",
                        "Delivered + short must equal requested for ticket_line_id " + variance.getTicketLineId());
            }
            if (variance.getRequestedQty().compareTo(BigDecimal.ZERO) < 0
                    || variance.getDeliveredQty().compareTo(BigDecimal.ZERO) < 0
                    || variance.getShortQty().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("CLOSE_INVALID_QTY",
                        "Quantities cannot be negative for ticket_line_id " + variance.getTicketLineId());
            }
        }
    }

    private Map<Long, PickTicketLine> loadTicketLines(long pickTicketId) throws SQLException {
        List<PickTicketLine> ticketLines = ticketDao.listTicketLines(pickTicketId);
        Map<Long, PickTicketLine> map = new HashMap<>();
        for (PickTicketLine line : ticketLines) {
            map.put(line.getTicketLineId(), line);
        }
        return map;
    }

    private void safeRollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed", ex);
            }
        }
    }

    private void restoreAndClose(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to reset auto-commit", ex);
            }
            try {
                conn.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to close connection", ex);
            }
        }
    }
}

