package com.ccinfom.test;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.CloseDaoImpl;
import com.ccinfom.dao.impl.DispatchDaoImpl;
import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PackDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.CloseDao;
import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.infra.InventoryHelper;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;
import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import com.ccinfom.service.CloseService;
import com.ccinfom.service.DispatchService;
import com.ccinfom.service.PackService;
import com.ccinfom.service.impl.CloseServiceImpl;
import com.ccinfom.service.impl.DispatchServiceImpl;
import com.ccinfom.service.impl.PackServiceImpl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared helpers for Phase E service tests.
 */
final class PhaseETestSupport {

    private static final String TEST_USER = "phaseE-test";

    private static final TicketDao TICKET_DAO = new TicketDaoImpl();
    private static final PickingDao PICKING_DAO = new PickingDaoImpl();
    private static final LookupDao LOOKUP_DAO = new LookupDaoImpl();
    private static final PackDao PACK_DAO = new PackDaoImpl();
    private static final DispatchDao DISPATCH_DAO = new DispatchDaoImpl();
    private static final CloseDao CLOSE_DAO = new CloseDaoImpl();
    private static final InventoryHelper INVENTORY_HELPER = new InventoryHelper();

    private PhaseETestSupport() {}

    static PackService createPackService() {
        return new PackServiceImpl(PACK_DAO, PICKING_DAO, TICKET_DAO, LOOKUP_DAO);
    }

    static DispatchService createDispatchService() {
        return new DispatchServiceImpl(DISPATCH_DAO, PACK_DAO, TICKET_DAO);
    }

    static CloseService createCloseService() {
        return new CloseServiceImpl(CLOSE_DAO, TICKET_DAO, INVENTORY_HELPER);
    }

    static InventoryHelper getInventoryHelper() {
        return INVENTORY_HELPER;
    }

    /* -------------------------------------------------------------
     * Ticket / Picking Test Fixture Handling
     * ------------------------------------------------------------- */

    static TestTicketContext createTestTicket(String label,
                                              long[] productIds,
                                              BigDecimal[] quantities,
                                              String[] uoms) throws SQLException {
        Objects.requireNonNull(productIds, "productIds");
        Objects.requireNonNull(quantities, "quantities");
        if (productIds.length != quantities.length) {
            throw new IllegalArgumentException("productIds and quantities length mismatch");
        }
        if (uoms != null && uoms.length != productIds.length) {
            throw new IllegalArgumentException("uoms length mismatch");
        }

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);

            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L);
            hdr.setBranchId(1L);
            hdr.setTicketStatus(PickTicketHdr.TicketStatus.Picking);
            hdr.setRemarks(label);
            hdr.setUpdatedBy(TEST_USER);
            long ticketId = TICKET_DAO.insertTicketHeader(hdr, conn);

            List<PickTicketLine> ticketLines = new ArrayList<>();
            for (int i = 0; i < productIds.length; i++) {
                PickTicketLine line = new PickTicketLine();
                line.setPickTicketId(ticketId);
                line.setProductId(productIds[i]);
                line.setRequestedQty(quantities[i]);
                line.setUom(uoms == null ? "pcs" : uoms[i]);
                line.setLineStatus(PickTicketLine.LineStatus.Valid);
                line.setUpdatedBy(TEST_USER);
                ticketLines.add(line);
            }
            TICKET_DAO.insertTicketLines(ticketLines, conn);

            Map<Long, LineInfo> lineInfoByTicketLineId = new HashMap<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT ticket_line_id, product_id, requested_qty, uom "
                            + "FROM pick_ticket_line WHERE pick_ticket_id = ? ORDER BY ticket_line_id")) {
                ps.setLong(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        LineInfo info = new LineInfo();
                        info.ticketLineId = rs.getLong("ticket_line_id");
                        info.productId = rs.getLong("product_id");
                        info.requestedQty = rs.getBigDecimal("requested_qty");
                        info.uom = rs.getString("uom");
                        loadProductSnapshot(conn, info);
                        lineInfoByTicketLineId.put(info.ticketLineId, info);
                    }
                }
            }

            PickingHdr pickingHdr = new PickingHdr();
            pickingHdr.setPickTicketId(ticketId);
            pickingHdr.setPickerEmployeeId(findAnyPicker(conn));
            pickingHdr.setPickingStatus("Picking");
            pickingHdr.setUpdatedBy(TEST_USER);
            long pickingId = PICKING_DAO.insertPickingHeader(pickingHdr, conn);

            List<PickingLine> pickingLines = new ArrayList<>();
            for (LineInfo info : lineInfoByTicketLineId.values()) {
                PickingLine line = new PickingLine();
                line.setPickingId(pickingId);
                line.setTicketLineId(info.ticketLineId);
                line.setProductId(info.productId);
                line.setPickedQty(info.requestedQty);
                line.setUom(info.uom);
                line.setUpdatedBy(TEST_USER);
                pickingLines.add(line);
            }
            PICKING_DAO.insertPickingLines(pickingId, pickingLines, conn);

            conn.commit();

            List<LineInfo> finalLines = loadPickingLines(pickingId);
            TestTicketContext ctx = new TestTicketContext();
            ctx.ticketId = ticketId;
            ctx.pickingId = pickingId;
            ctx.lines = finalLines;
            ctx.label = label;
            ctx.initialStatus = PickTicketHdr.TicketStatus.Picking;
            return ctx;
        }
    }

    private static void loadProductSnapshot(Connection conn, LineInfo info) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT reserved_qty, on_hand_qty FROM products WHERE product_id = ?")) {
            ps.setLong(1, info.productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    info.initialReserved = rs.getBigDecimal("reserved_qty");
                    info.initialOnHand = rs.getBigDecimal("on_hand_qty");
                } else {
                    throw new SQLException("Product not found for snapshot: " + info.productId);
                }
            }
        }
    }

    private static List<LineInfo> loadPickingLines(long pickingId) throws SQLException {
        List<LineInfo> result = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT pl.picking_line_id, pl.ticket_line_id, pl.product_id, "
                             + "pl.picked_qty, pl.uom, tl.requested_qty "
                             + "FROM picking_line pl "
                             + "JOIN pick_ticket_line tl ON tl.ticket_line_id = pl.ticket_line_id "
                             + "WHERE pl.picking_id = ? ORDER BY pl.picking_line_id")) {
            ps.setLong(1, pickingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LineInfo info = new LineInfo();
                    info.pickingLineId = rs.getLong("picking_line_id");
                    info.ticketLineId = rs.getLong("ticket_line_id");
                    info.productId = rs.getLong("product_id");
                    info.requestedQty = rs.getBigDecimal("requested_qty");
                    info.uom = rs.getString("uom");
                    info.initialReserved = fetchCurrentReserved(info.productId);
                    info.initialOnHand = fetchCurrentOnHand(info.productId);
                    result.add(info);
                }
            }
        }
        return result;
    }

    private static BigDecimal fetchCurrentReserved(long productId) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT reserved_qty FROM products WHERE product_id = ?")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal fetchCurrentOnHand(long productId) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT on_hand_qty FROM products WHERE product_id = ?")) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private static long findAnyPicker(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT employee_id FROM employees "
                        + "WHERE employee_role = 'picker' AND employee_status = 'active' "
                        + "ORDER BY employee_id LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT employee_id FROM employees ORDER BY employee_id LIMIT 1")) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("No employees available for picker assignment");
    }

    static void cleanupTicket(TestTicketContext ctx) throws SQLException {
        if (ctx == null) {
            return;
        }
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Remove inventory logs referencing this ticket/close.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM inventory_txn_log "
                            + "WHERE ticket_id = ? OR source_txn_id IN "
                            + "(SELECT close_id FROM close_hdr WHERE pick_ticket_id = ?)")) {
                ps.setLong(1, ctx.ticketId);
                ps.setLong(2, ctx.ticketId);
                ps.executeUpdate();
            }

            // Delete close records.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM close_variance WHERE close_id IN "
                            + "(SELECT close_id FROM close_hdr WHERE pick_ticket_id = ?)")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM close_hdr WHERE pick_ticket_id = ?")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }

            // Delete dispatch records.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dispatch_line WHERE dispatch_id IN "
                            + "(SELECT dispatch_id FROM dispatch_hdr WHERE pick_ticket_id = ?)")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dispatch_hdr WHERE pick_ticket_id = ?")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }

            // Delete pack boxes.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pack_box_line WHERE box_id IN "
                            + "(SELECT box_id FROM pack_box_hdr WHERE pick_ticket_id = ?)")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pack_box_hdr WHERE pick_ticket_id = ?")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }

            // Reset product quantities.
            Map<Long, BigDecimal[]> snapshots = new HashMap<>();
            for (LineInfo info : ctx.lines) {
                snapshots.putIfAbsent(info.productId, new BigDecimal[]{info.initialReserved, info.initialOnHand});
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET reserved_qty = ?, on_hand_qty = ?, updated_by = ?, updated_at = ? "
                            + "WHERE product_id = ?")) {
                for (Map.Entry<Long, BigDecimal[]> entry : snapshots.entrySet()) {
                    ps.setBigDecimal(1, entry.getValue()[0]);
                    ps.setBigDecimal(2, entry.getValue()[1]);
                    ps.setString(3, TEST_USER);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(5, entry.getKey());
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Remove picking & ticket details.
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM picking_line WHERE picking_id = ?")) {
                ps.setLong(1, ctx.pickingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM picking_hdr WHERE picking_id = ?")) {
                ps.setLong(1, ctx.pickingId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pick_ticket_line WHERE pick_ticket_id = ?")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pick_ticket_hdr WHERE pick_ticket_id = ?")) {
                ps.setLong(1, ctx.ticketId);
                ps.executeUpdate();
            }

            conn.commit();
        }
    }

    /* -------------------------------------------------------------
     * Box helpers
     * ------------------------------------------------------------- */

    static TestBox createBoxWithAllLines(TestTicketContext ctx,
                                         PackService packService,
                                         String sourceRef,
                                         boolean sealBox,
                                         java.util.function.Function<LineInfo, BigDecimal> qtySupplier)
            throws Exception {
        PackBox box = new PackBox();
        box.setPickTicketId(ctx.ticketId);
        box.setPickingId(ctx.pickingId);
        box.setSealedFlag(false);
        box.setSealMethod(null);
        box.setCreatedBy(TEST_USER);
        box.setUpdatedBy(TEST_USER);
        box.setSourceRef(sourceRef);
        long boxId = packService.createBox(box);

        List<PackBoxLine> lines = buildPackLines(ctx, boxId, sourceRef, qtySupplier);
        packService.addLines(boxId, lines);

        PickTicketHdr.TicketStatus previousStatus = fetchTicketStatus(ctx.ticketId);
        if (sealBox) {
            packService.sealBox(boxId, "strap", TEST_USER);
        }

        TestBox testBox = new TestBox();
        testBox.boxId = boxId;
        testBox.ticketId = ctx.ticketId;
        testBox.pickingId = ctx.pickingId;
        testBox.sourceRef = sourceRef;
        testBox.previousStatus = previousStatus;
        return testBox;
    }

    static List<PackBoxLine> buildPackLines(TestTicketContext ctx,
                                            long boxId,
                                            String sourceRef,
                                            java.util.function.Function<LineInfo, BigDecimal> qtySupplier) {
        List<PackBoxLine> result = new ArrayList<>();
        int counter = 1;
        for (LineInfo info : ctx.lines) {
            PackBoxLine line = new PackBoxLine();
            line.setBoxId(boxId);
            line.setPickingLineId(info.pickingLineId);
            line.setPackedQty(qtySupplier.apply(info));
            line.setUom(info.uom);
            line.setSourceRef(sourceRef + "-line-" + counter++);
            line.setCreatedBy(TEST_USER);
            line.setUpdatedBy(TEST_USER);
            result.add(line);
        }
        return result;
    }

    static void deleteBox(TestBox testBox) throws SQLException {
        if (testBox == null) {
            return;
        }
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pack_box_line WHERE box_id = ?")) {
                ps.setLong(1, testBox.boxId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pack_box_hdr WHERE box_id = ?")) {
                ps.setLong(1, testBox.boxId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE pick_ticket_hdr SET ticket_status = ?, updated_by = ?, updated_at = ? "
                            + "WHERE pick_ticket_id = ?")) {
                ps.setString(1, testBox.previousStatus.getDbValue());
                ps.setString(2, TEST_USER);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(4, testBox.ticketId);
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    /* -------------------------------------------------------------
     * Dispatch helpers
     * ------------------------------------------------------------- */

    static TestDispatch createDispatch(TestTicketContext ctx,
                                       TestBox box,
                                       DispatchService dispatchService,
                                       String manifestNo,
                                       long vehicleId,
                                       long driverId,
                                       String sourceRef) throws Exception {
        DispatchHeader header = new DispatchHeader();
        header.setPickTicketId(ctx.ticketId);
        header.setVehicleId(vehicleId);
        header.setDriverId(driverId);
        header.setManifestNo(manifestNo);
        header.setDepartTs(null);
        header.setArriveTs(null);
        header.setPodRef(null);
        header.setPodTs(null);
        header.setSourceRef(sourceRef);
        header.setCreatedBy(TEST_USER);
        header.setUpdatedBy(TEST_USER);

        DispatchLine line = new DispatchLine();
        line.setBoxId(box.boxId);
        line.setSourceRef(sourceRef + "-line");
        line.setCreatedBy(TEST_USER);
        line.setUpdatedBy(TEST_USER);

        PickTicketHdr.TicketStatus previousStatus = fetchTicketStatus(ctx.ticketId);
        DispatchHeader created = dispatchService.createDispatch(header, List.of(line));

        TestDispatch dispatch = new TestDispatch();
        dispatch.dispatchId = created.getDispatchId();
        dispatch.ticketId = ctx.ticketId;
        dispatch.sourceRef = sourceRef;
        dispatch.manifestNo = manifestNo;
        dispatch.previousStatus = previousStatus;
        return dispatch;
    }

    static void deleteDispatch(TestDispatch dispatch) throws SQLException {
        if (dispatch == null) {
            return;
        }
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dispatch_line WHERE dispatch_id = ?")) {
                ps.setLong(1, dispatch.dispatchId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM dispatch_hdr WHERE dispatch_id = ?")) {
                ps.setLong(1, dispatch.dispatchId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE pick_ticket_hdr SET ticket_status = ?, updated_by = ?, updated_at = ? "
                            + "WHERE pick_ticket_id = ?")) {
                ps.setString(1, dispatch.previousStatus.getDbValue());
                ps.setString(2, TEST_USER);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(4, dispatch.ticketId);
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    static long findAvailableVehicleId() throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vehicle_id FROM vehicles WHERE vehicle_status = 'available' ORDER BY vehicle_id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("No available vehicle found");
    }

    static VehicleSnapshot captureVehicle(long vehicleId) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT vehicle_status, capacity FROM vehicles WHERE vehicle_id = ?")) {
            ps.setLong(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    VehicleSnapshot snapshot = new VehicleSnapshot();
                    snapshot.vehicleId = vehicleId;
                    snapshot.status = rs.getString("vehicle_status");
                    snapshot.capacity = rs.getInt("capacity");
                    return snapshot;
                }
            }
        }
        throw new SQLException("Vehicle not found for snapshot: " + vehicleId);
    }

    static void restoreVehicleSnapshot(VehicleSnapshot snapshot) throws SQLException {
        if (snapshot == null) {
            return;
        }
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE vehicles SET vehicle_status = ?, capacity = ?, updated_at = ?, updated_by = ? "
                             + "WHERE vehicle_id = ?")) {
            ps.setString(1, snapshot.status);
            ps.setInt(2, snapshot.capacity);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, TEST_USER);
            ps.setLong(5, snapshot.vehicleId);
            ps.executeUpdate();
        }
    }

    static long findDispatcherId() throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT employee_id FROM employees "
                             + "WHERE employee_role = 'dispatcher' AND employee_status = 'active' "
                             + "ORDER BY employee_id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        throw new SQLException("No dispatcher found");
    }

    /* -------------------------------------------------------------
     * Close helpers
     * ------------------------------------------------------------- */

    static CloseSnapshot captureCloseSnapshot(long ticketId) throws SQLException {
        CloseSnapshot snapshot = new CloseSnapshot();
        snapshot.ticketId = ticketId;
        snapshot.previousStatus = fetchTicketStatus(ticketId);
        snapshot.productSnapshots = new HashMap<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT DISTINCT product_id FROM picking_line pl "
                             + "JOIN picking_hdr ph ON pl.picking_id = ph.picking_id "
                             + "WHERE ph.pick_ticket_id = ?")) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long productId = rs.getLong(1);
                    ProductSnapshot productSnapshot = new ProductSnapshot();
                    productSnapshot.productId = productId;
                    productSnapshot.reservedQty = fetchCurrentReserved(productId);
                    productSnapshot.onHandQty = fetchCurrentOnHand(productId);
                    snapshot.productSnapshots.put(productId, productSnapshot);
                }
            }
        }
        return snapshot;
    }

    static void restoreCloseSnapshot(CloseSnapshot snapshot) throws SQLException {
        if (snapshot == null) {
            return;
        }
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE pick_ticket_hdr SET ticket_status = ?, updated_by = ?, updated_at = ? "
                            + "WHERE pick_ticket_id = ?")) {
                ps.setString(1, snapshot.previousStatus.getDbValue());
                ps.setString(2, TEST_USER);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setLong(4, snapshot.ticketId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE products SET reserved_qty = ?, on_hand_qty = ?, updated_by = ?, updated_at = ? "
                            + "WHERE product_id = ?")) {
                for (ProductSnapshot productSnapshot : snapshot.productSnapshots.values()) {
                    ps.setBigDecimal(1, productSnapshot.reservedQty);
                    ps.setBigDecimal(2, productSnapshot.onHandQty);
                    ps.setString(3, TEST_USER);
                    ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                    ps.setLong(5, productSnapshot.productId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM close_variance WHERE close_id IN "
                            + "(SELECT close_id FROM close_hdr WHERE pick_ticket_id = ?)")) {
                ps.setLong(1, snapshot.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM close_hdr WHERE pick_ticket_id = ?")) {
                ps.setLong(1, snapshot.ticketId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM inventory_txn_log WHERE ticket_id = ?")) {
                ps.setLong(1, snapshot.ticketId);
                ps.executeUpdate();
            }
            conn.commit();
        }
    }

    /* -------------------------------------------------------------
     * Utility lookups
     * ------------------------------------------------------------- */

    static PickTicketHdr.TicketStatus fetchTicketStatus(long ticketId) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT ticket_status FROM pick_ticket_hdr WHERE pick_ticket_id = ?")) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PickTicketHdr.TicketStatus.fromDb(rs.getString(1));
                }
            }
        }
        throw new SQLException("Ticket not found: " + ticketId);
    }

    /* -------------------------------------------------------------
     * Data holder classes
     * ------------------------------------------------------------- */

    static final class TestTicketContext {
        long ticketId;
        long pickingId;
        List<LineInfo> lines;
        String label;
        PickTicketHdr.TicketStatus initialStatus;
    }

    static final class LineInfo {
        long ticketLineId;
        long pickingLineId;
        long productId;
        BigDecimal requestedQty;
        String uom;
        BigDecimal initialReserved;
        BigDecimal initialOnHand;
    }

    static final class TestBox {
        long boxId;
        long ticketId;
        long pickingId;
        String sourceRef;
        PickTicketHdr.TicketStatus previousStatus;
    }

    static final class TestDispatch {
        long dispatchId;
        long ticketId;
        String manifestNo;
        String sourceRef;
        PickTicketHdr.TicketStatus previousStatus;
    }

    static final class VehicleSnapshot {
        long vehicleId;
        String status;
        int capacity;
    }

    static final class CloseSnapshot {
        long ticketId;
        PickTicketHdr.TicketStatus previousStatus;
        Map<Long, ProductSnapshot> productSnapshots;
    }

    static final class ProductSnapshot {
        long productId;
        BigDecimal reservedQty;
        BigDecimal onHandQty;
    }
}

