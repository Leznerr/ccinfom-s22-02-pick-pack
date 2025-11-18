package com.ccinfom.service.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.LookupValue;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.service.DispatchService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.util.CoreValidationUtil;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DispatchServiceImpl implements DispatchService {

    private static final Logger LOGGER = Logger.getLogger(DispatchServiceImpl.class.getName());
    private static final String SYSTEM_USER = "system";

    private final DispatchDao dispatchDao;
    private final PackDao packDao;
    private final TicketDao ticketDao;

    public DispatchServiceImpl(DispatchDao dispatchDao,
                               PackDao packDao,
                               TicketDao ticketDao) {
        this.dispatchDao = dispatchDao;
        this.packDao = packDao;
        this.ticketDao = ticketDao;
    }

    @Override
    public DispatchHeader createDispatch(DispatchHeader header, List<DispatchLine> lines)
            throws SQLException, ValidationException {

        validateDispatchRequest(header, lines);

            String actor = header.getCreatedBy();
            if (actor == null || actor.isBlank()) {
                actor = SYSTEM_USER;
                header.setCreatedBy(actor);
            }
            if (header.getDispatchStatus() == null) {
                header.setDispatchStatus(DispatchHeader.DispatchStatus.Built);
            }
            if (header.getUpdatedBy() == null || header.getUpdatedBy().isBlank()) {
                header.setUpdatedBy(actor);
            }

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            long ticketId = header.getPickTicketId();
            LOGGER.info(() -> String.format("[T4_CREATE_MANIFEST][ticket=%d][vehicle=%d][driver=%d][boxes=%d] BEGIN",
                    ticketId, header.getVehicleId(), header.getDriverId(), lines.size()));

            CoreValidationUtil.ensureNotDuplicate(
                    dispatchDao.isManifestNoExists(header.getManifestNo(), conn),
                    "DISPATCH_MANIFEST_DUP",
                    "Manifest number already exists: " + header.getManifestNo()
            );

            if (!dispatchDao.isVehicleAvailable(header.getVehicleId(), conn)) {
                throw new ValidationException("DISPATCH_VEHICLE_UNAVAILABLE",
                        "Vehicle is not available for dispatch.");
            }

            int capacity = dispatchDao.fetchVehicleCapacity(header.getVehicleId(), conn);
            int currentLoad = dispatchDao.countBoxesAssignedToVehicle(header.getVehicleId(), conn);
            CoreValidationUtil.ensureCapacityAvailable(
                    capacity,
                    currentLoad,
                    lines.size(),
                    "DISPATCH_CAPACITY_EXCEEDED"
            );

            for (DispatchLine line : lines) {
                line.setCreatedBy(line.getCreatedBy() == null ? actor : line.getCreatedBy());
                line.setUpdatedBy(line.getUpdatedBy() == null ? line.getCreatedBy() : line.getUpdatedBy());
                if (line.getQtyDispatched() == null) {
                    line.setQtyDispatched(java.math.BigDecimal.ONE);
                }

                PackBox box = packDao.findBoxById(line.getBoxId(), conn)
                        .orElseThrow(() -> new ValidationException("DISPATCH_BOX_NOT_FOUND",
                                "Box not found: " + line.getBoxId()));

                if (!box.isSealedFlag()) {
                    throw new ValidationException("DISPATCH_UNSEALED_BOX",
                            "Box " + line.getBoxId() + " must be sealed before dispatch.");
                }

                if (!Objects.equals(box.getPickTicketId(), header.getPickTicketId())) {
                    throw new ValidationException("DISPATCH_BOX_TICKET_MISMATCH",
                            "Box " + line.getBoxId() + " does not belong to ticket " + header.getPickTicketId());
                }

                if (dispatchDao.isBoxLoaded(line.getBoxId(), conn)) {
                    throw new ValidationException("DISPATCH_BOX_ALREADY_LOADED",
                            "Box " + line.getBoxId() + " is already loaded on a manifest.");
                }
            }

            long dispatchId = dispatchDao.insertDispatchHeader(header, conn);
            dispatchDao.insertDispatchLines(dispatchId, lines, conn);

            ticketDao.updateTicketStatus(
                    header.getPickTicketId(),
                    PickTicketHdr.TicketStatus.Dispatched,
                    actor,
                    conn
            );

            conn.commit();

            header.setDispatchId(dispatchId);
            LOGGER.info(() -> String.format("[T4_CREATE_MANIFEST][ticket=%d][dispatch=%d] SUCCESS",
                    ticketId, dispatchId));
            return header;
        } catch (SQLException | ValidationException ex) {
            safeRollback(conn);
            if (ex instanceof SQLException sqlEx) {
                LOGGER.log(Level.SEVERE,
                        String.format("[T4_CREATE_MANIFEST][ticket=%d] FAILED: %s",
                                header.getPickTicketId(), sqlEx.getMessage()),
                        sqlEx);
                throw sqlEx;
            }
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public void registerDeparture(long dispatchId, DispatchHeader updates)
            throws SQLException, ValidationException {
        if (dispatchId <= 0) {
            throw new ValidationException("DISPATCH_INVALID_ID", "Dispatch ID must be positive.");
        }

        LocalDateTime departTs = updates != null ? updates.getDepartTs() : null;
        LocalDateTime arriveTs = updates != null ? updates.getArriveTs() : null;
        LocalDateTime podTs = updates != null ? updates.getPodTs() : null;
        String podRef = updates != null ? updates.getPodRef() : null;
        String actor = (updates != null && updates.getUpdatedBy() != null && !updates.getUpdatedBy().isBlank())
                ? updates.getUpdatedBy()
                : SYSTEM_USER;

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            LOGGER.info(() -> String.format("[T4_RECORD_DEPARTURE][dispatch=%d] BEGIN", dispatchId));

            if (dispatchDao.findById(dispatchId, conn).isEmpty()) {
                throw new ValidationException("DISPATCH_NOT_FOUND", "Dispatch not found: " + dispatchId);
            }

            dispatchDao.updateDispatchTimings(dispatchId, departTs, arriveTs, podTs, podRef, actor, conn);

            conn.commit();
            LOGGER.info(() -> String.format("[T4_RECORD_DEPARTURE][dispatch=%d] SUCCESS", dispatchId));
        } catch (SQLException | ValidationException ex) {
            safeRollback(conn);
            if (ex instanceof SQLException sqlEx) {
                LOGGER.log(Level.SEVERE,
                        String.format("[T4_RECORD_DEPARTURE][dispatch=%d] FAILED: %s",
                                dispatchId, sqlEx.getMessage()),
                        sqlEx);
                throw sqlEx;
            }
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public boolean canLoadBox(long boxId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return packDao.findBoxById(boxId, conn)
                    .filter(PackBox::isSealedFlag)
                    .filter(box -> {
                        try {
                            return !dispatchDao.isBoxLoaded(boxId, conn);
                        } catch (SQLException e) {
                            LOGGER.log(Level.WARNING, "Failed to verify box load state", e);
                            return false;
                        }
                    })
                    .isPresent();
        }
    }

    private void validateDispatchRequest(DispatchHeader header, List<DispatchLine> lines)
            throws ValidationException {
        if (header == null) {
            throw new ValidationException("DISPATCH_HDR_REQUIRED", "Dispatch header is required.");
        }
        if (header.getPickTicketId() == null) {
            throw new ValidationException("DISPATCH_TICKET_REQUIRED", "pick_ticket_id is required.");
        }
        if (header.getVehicleId() == null) {
            throw new ValidationException("DISPATCH_VEHICLE_REQUIRED", "vehicle_id is required.");
        }
        if (header.getDriverId() == null) {
            throw new ValidationException("DISPATCH_DRIVER_REQUIRED", "driver_id is required.");
        }
        if (header.getManifestNo() == null || header.getManifestNo().isBlank()) {
            throw new ValidationException("DISPATCH_MANIFEST_REQUIRED", "Manifest number is required.");
        }
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("DISPATCH_LINES_REQUIRED", "At least one box must be loaded.");
        }
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




    // NEW METHODS FOR DispatchFrom.java
    @Override
    public void registerArrival(long dispatchId, DispatchHeader updates)
        throws SQLException, ValidationException {
        if (dispatchId <= 0) {
            throw new ValidationException("DISPATCH_INVALID_ID", "Dispatch ID must be positive.");
        }

        LocalDateTime arriveTs = updates != null && updates.getArriveTs() != null ? updates.getArriveTs() : LocalDateTime.now();
        String receivedBy = updates != null ? updates.getPodRef() : null;
        String actor = (updates != null && updates.getUpdatedBy() != null && !updates.getUpdatedBy().isBlank())
                ? updates.getUpdatedBy()
                : SYSTEM_USER;

        Connection conn = null;
    try {
        conn = DbConnection.getConnection();
        conn.setAutoCommit(false);
        LOGGER.info(() -> String.format("[T4_RECORD_ARRIVAL][dispatch=%d] BEGIN", dispatchId));

        if (dispatchDao.findById(dispatchId, conn).isEmpty()) {
            throw new ValidationException("DISPATCH_NOT_FOUND", "Dispatch not found: " + dispatchId);
        }

        // Reuse DAO method for updating arrival timestamp
        dispatchDao.updateDispatchArrival(dispatchId, arriveTs, actor, conn);
        dispatchDao.markLinesDelivered(dispatchId, arriveTs, receivedBy, actor, conn);
        // Mark header as Delivered (align with proposal: Built/Loaded/Delivered/Partial)
        dispatchDao.updateDispatchStatus(dispatchId, DispatchHeader.DispatchStatus.Delivered.name(), actor, conn);

        conn.commit();
        LOGGER.info(() -> String.format("[T4_RECORD_ARRIVAL][dispatch=%d] SUCCESS", dispatchId));
    } catch (SQLException | ValidationException ex) {
        safeRollback(conn);
        if (ex instanceof SQLException sqlEx) {
            LOGGER.log(Level.SEVERE,
                    String.format("[T4_RECORD_ARRIVAL][dispatch=%d] FAILED: %s",
                            dispatchId, sqlEx.getMessage()),
                    sqlEx);
            throw sqlEx;
        }
        throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public List<LookupValue> findReadyTickets() throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return ticketDao.findReadyTicketOptions(conn);
        }
    }

    @Override
    public List<LookupValue> findAvailableVehicles() throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return dispatchDao.findAvailableVehicles(conn);
        }
    }

    @Override
    public List<LookupValue> findAvailableDrivers() throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return dispatchDao.findAvailableDrivers(conn);
        }
    }

    @Override
    public List<DispatchLine> findBoxesForTicket(long ticketId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return dispatchDao.findBoxesForTicket(ticketId, conn); // implement in DAO
        }
    }





} // end

