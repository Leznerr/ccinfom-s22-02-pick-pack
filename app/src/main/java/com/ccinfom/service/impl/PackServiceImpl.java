package com.ccinfom.service.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickingLine;
import com.ccinfom.model.Product;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import com.ccinfom.service.PackService;
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

public class PackServiceImpl implements PackService {

    private static final Logger LOGGER = Logger.getLogger(PackServiceImpl.class.getName());
    private static final String SYSTEM_USER = "system";

    private final PackDao packDao;
    private final PickingDao pickingDao;
    private final TicketDao ticketDao;
    private final LookupDao lookupDao;

    public PackServiceImpl(PackDao packDao,
                           PickingDao pickingDao,
                           TicketDao ticketDao,
                           LookupDao lookupDao) {
        this.packDao = packDao;
        this.pickingDao = pickingDao;
        this.ticketDao = ticketDao;
        this.lookupDao = lookupDao;
    }

    @Override
    public long createBox(PackBox box) throws SQLException, ValidationException {
        if (box == null) {
            throw new ValidationException("PACK_BOX_NULL", "Box details are required.");
        }
        if (box.getPickTicketId() == null || box.getPickingId() == null) {
            throw new ValidationException("PACK_BOX_INCOMPLETE", "pick_ticket_id and picking_id are required.");
        }

        if (box.getCreatedBy() == null || box.getCreatedBy().isBlank()) {
            box.setCreatedBy(SYSTEM_USER);
        }
        if (box.getUpdatedBy() == null || box.getUpdatedBy().isBlank()) {
            box.setUpdatedBy(box.getCreatedBy());
        }

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            LOGGER.info(() -> String.format("[T3_CREATE_BOX][ticket=%d][picking=%d] BEGIN",
                    box.getPickTicketId(), box.getPickingId()));

            long boxId = packDao.insertBox(box, conn);

            conn.commit();
            LOGGER.info(() -> String.format("[T3_CREATE_BOX][box=%d][ticket=%d] SUCCESS",
                    boxId, box.getPickTicketId()));
            return boxId;
        } catch (SQLException ex) {
            safeRollback(conn);
            LOGGER.log(Level.SEVERE,
                    String.format("[T3_CREATE_BOX][ticket=%d][picking=%d] FAILED: %s",
                            box.getPickTicketId(), box.getPickingId(), ex.getMessage()),
                    ex);
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public void addLines(long boxId, List<PackBoxLine> lines) throws SQLException, ValidationException {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("PACK_LINES_EMPTY", "At least one line must be supplied.");
        }

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            LOGGER.info(() -> String.format("[T3_ADD_LINES][box=%d][lines=%d] BEGIN", boxId, lines.size()));

            PackBox box = packDao.findBoxById(boxId, conn)
                    .orElseThrow(() -> new ValidationException("PACK_BOX_NOT_FOUND", "Box does not exist."));

            if (box.isSealedFlag()) {
                throw new ValidationException("PACK_ALREADY_SEALED", "Cannot add items to an already sealed box.");
            }

            Map<Long, PickingLine> pickingLineMap = loadPickingLines(box.getPickingId());

            for (PackBoxLine line : lines) {
                validatePackLine(box, line, pickingLineMap, conn);
                line.setBoxId(boxId);
                if (line.getCreatedBy() == null || line.getCreatedBy().isBlank()) {
                    line.setCreatedBy(SYSTEM_USER);
                }
                if (line.getUpdatedBy() == null || line.getUpdatedBy().isBlank()) {
                    line.setUpdatedBy(line.getCreatedBy());
                }
            }

            packDao.insertBoxLines(boxId, lines, conn);

            if (packDao.isTicketFullyPacked(box.getPickTicketId(), conn)) {
                ticketDao.updateTicketStatus(
                        box.getPickTicketId(),
                        PickTicketHdr.TicketStatus.Packed,
                        SYSTEM_USER,
                        conn
                );
            }

            conn.commit();
            LOGGER.info(() -> String.format("[T3_ADD_LINES][box=%d] SUCCESS lines=%d",
                    boxId, lines.size()));
        } catch (SQLException ex) {
            safeRollback(conn);
            String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (message.contains("uq_pack_line_picking") || message.contains("duplicate")) {
                throw new ValidationException("PACK_LINE_ALREADY_BOXED",
                        "One of the picking lines is already boxed.");
            }
            LOGGER.log(Level.SEVERE,
                    String.format("[T3_ADD_LINES][box=%d] FAILED: %s", boxId, ex.getMessage()),
                    ex);
            throw ex;
        } catch (ValidationException ex) {
            safeRollback(conn);
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public void sealBox(long boxId, String sealMethod, String user) throws SQLException, ValidationException {
        if (sealMethod == null || sealMethod.isBlank()) {
            throw new ValidationException("PACK_SEAL_METHOD_REQUIRED", "Seal method must be provided.");
        }

        String actor = (user == null || user.isBlank()) ? SYSTEM_USER : user;

        Connection conn = null;
        try {
            conn = DbConnection.getConnection();
            conn.setAutoCommit(false);
            LOGGER.info(() -> String.format("[T3_SEAL_BOX][box=%d] BEGIN", boxId));

            PackBox box = packDao.findBoxById(boxId, conn)
                    .orElseThrow(() -> new ValidationException("PACK_BOX_NOT_FOUND", "Box does not exist."));

            if (box.isSealedFlag()) {
                throw new ValidationException("PACK_ALREADY_SEALED", "Box is already sealed.");
            }

            List<PackBoxLine> existingLines = packDao.listLinesByBoxId(boxId, conn);
            if (existingLines.isEmpty()) {
                throw new ValidationException("PACK_SEAL_EMPTY_BOX", "Cannot seal an empty box.");
            }

            packDao.sealBox(boxId, sealMethod, actor, conn);

            if (packDao.isTicketFullyPacked(box.getPickTicketId(), conn)) {
                ticketDao.updateTicketStatus(
                        box.getPickTicketId(),
                        PickTicketHdr.TicketStatus.Packed,
                        actor,
                        conn
                );
            }

            conn.commit();
            LOGGER.info(() -> String.format("[T3_SEAL_BOX][box=%d][ticket=%d] SUCCESS",
                    boxId, box.getPickTicketId()));
        } catch (SQLException ex) {
            safeRollback(conn);
            LOGGER.log(Level.SEVERE,
                    String.format("[T3_SEAL_BOX][box=%d] FAILED: %s", boxId, ex.getMessage()),
                    ex);
            throw ex;
        } catch (ValidationException ex) {
            safeRollback(conn);
            throw ex;
        } finally {
            restoreAndClose(conn);
        }
    }

    @Override
    public boolean isTicketPacked(long pickTicketId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return packDao.isTicketFullyPacked(pickTicketId, conn);
        }
    }

    @Override
    public Optional<PackBox> findOpenBox(long pickingId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return packDao.findOpenBoxByPickingId(pickingId, conn);
        }
    }

    @Override
    public List<PackBoxLine> listLinesByBoxId(long boxId) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            return packDao.listLinesByBoxId(boxId, conn);
        }
    }

    private Map<Long, PickingLine> loadPickingLines(long pickingId) throws SQLException {
        List<PickingLine> pickingLines = pickingDao.listLinesByPickingId(pickingId);
        Map<Long, PickingLine> map = new HashMap<>();
        for (PickingLine line : pickingLines) {
            map.put(line.getPickingLineId(), line);
        }
        return map;
    }

    private void validatePackLine(PackBox box,
                                  PackBoxLine line,
                                  Map<Long, PickingLine> pickingLineMap,
                                  Connection conn) throws SQLException, ValidationException {
        if (line.getPickingLineId() == null) {
            throw new ValidationException("PACK_LINE_INVALID", "Picking line id is required.");
        }

        PickingLine pickingLine = pickingLineMap.get(line.getPickingLineId());
        if (pickingLine == null) {
            throw new ValidationException("PACK_LINE_UNKNOWN", "Picking line does not belong to this session.");
        }

        if (line.getPackedQty() == null || line.getPackedQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("PACK_INVALID_QTY", "Packed quantity must be greater than zero.");
        }

        if (line.getPackedQty().compareTo(pickingLine.getPickedQty()) > 0) {
            throw new ValidationException("PACK_OVER_QTY",
                    "Packed quantity cannot exceed picked quantity for line " + pickingLine.getPickingLineId());
        }

        if (packDao.existsPackedLineForPickingLine(line.getPickingLineId(), conn)) {
            throw new ValidationException("PACK_LINE_ALREADY_BOXED",
                    "Picking line " + pickingLine.getPickingLineId() + " is already boxed.");
        }

        try {
            Product product = lookupDao.findProductById(pickingLine.getProductId());
            if (product == null || !product.isActiveFlag()) {
                throw new ValidationException("PACK_PRODUCT_INACTIVE",
                        "Product for picking line " + pickingLine.getPickingLineId() + " is inactive.");
            }
        } catch (SQLException lookupError) {
            LOGGER.log(Level.WARNING, "Failed to lookup product while validating pack line", lookupError);
            throw lookupError;
        }

        if (line.getUom() == null || line.getUom().isBlank()) {
            line.setUom(pickingLine.getUom());
        }
        line.setCreatedBy(line.getCreatedBy() == null ? SYSTEM_USER : line.getCreatedBy());
        line.setUpdatedBy(line.getUpdatedBy() == null ? line.getCreatedBy() : line.getUpdatedBy());
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

