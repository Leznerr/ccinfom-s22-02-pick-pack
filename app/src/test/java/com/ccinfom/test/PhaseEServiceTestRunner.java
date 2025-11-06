package com.ccinfom.test;

import com.ccinfom.config.DbConnection;
import com.ccinfom.infra.InventoryHelper;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickingLine;
import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import com.ccinfom.service.CloseService;
import com.ccinfom.service.DispatchService;
import com.ccinfom.service.PackService;
import com.ccinfom.service.PickingService;
import com.ccinfom.service.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Executes Phase E service scenarios to validate Picking/Pack/Dispatch/Close behaviour.
 */
public final class PhaseEServiceTestRunner {

    private final PackService packService = PhaseETestSupport.createPackService();
    private final DispatchService dispatchService = PhaseETestSupport.createDispatchService();
    private final CloseService closeService = PhaseETestSupport.createCloseService();
    private final PickingService pickingService = PhaseETestSupport.createPickingService();
    private final InventoryHelper inventoryHelper = PhaseETestSupport.getInventoryHelper();

    private int passed;
    private int failed;

    private PhaseEServiceTestRunner() {
    }

    public static void main(String[] args) {
        new PhaseEServiceTestRunner().runSuite();
    }

    private void runSuite() {
        System.out.println("--- Phase E Service Test Suite ---");

        run("PickingService reserves inventory and logs delta",
                this::testPickingServiceReserveLogging);

        run("PackService happy path seals box and updates status",
                () -> withTicket("pack-happy", new long[]{1L, 2L}, new double[]{2, 3}, ctx ->
                        withBox(ctx, true, info -> info.requestedQty, (context, box) -> {
                            assertTrue(queryBoolean("SELECT sealed_flag FROM pack_box_hdr WHERE box_id = ?", box.boxId),
                                    "Expected sealed flag");
                            assertEquals(PickTicketHdr.TicketStatus.Packed,
                                    PhaseETestSupport.fetchTicketStatus(context.ticketId),
                                    "Ticket should be Packed after sealing box");
                        })));

        run("PackService rejects over-pack attempts", this::testPackOverQty);
        run("PackService rejects resealing sealed box", this::testPackAlreadySealed);
        run("PackService rejects inactive products", this::testPackProductInactive);
        run("PackService rejects double boxing picking lines", this::testPackLineAlreadyBoxed);

        run("InventoryHelper reserve delta writes log", this::testInventoryReserveDelta);
        run("InventoryHelper close delta adjusts quantities", this::testInventoryCloseDelta);
        run("InventoryHelper surfaces lock timeout", this::testInventoryLockTimeout);

        run("DispatchService happy path creates manifest", this::testDispatchHappyPath);
        run("DispatchService rejects unsealed box", this::testDispatchUnsealed);
        run("DispatchService enforces capacity", this::testDispatchCapacityExceeded);
        run("DispatchService enforces vehicle availability", this::testDispatchVehicleUnavailable);
        run("DispatchService prevents duplicate loading", this::testDispatchDuplicateBox);

        run("CloseService delivered flow adjusts inventory", this::testCloseDeliveredFlow);
        run("CloseService short-close flow adjusts reserved only", this::testCloseShortFlow);
        run("CloseService rejects reconciliation mismatch", this::testCloseMismatch);
        run("CloseService rejects negative quantities", this::testCloseInvalidQty);
        run("CloseService surfaces inventory lock timeout", this::testCloseLockTimeout);

        System.out.printf("%nSummary: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            throw new AssertionError("Phase E service suite reported failures.");
        }
    }

    private void run(String name, TestCase test) {
        System.out.print(" - " + name + "... ");
        try {
            test.execute();
            passed++;
            System.out.println("OK");
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL");
            t.printStackTrace(System.out);
        }
    }

    @FunctionalInterface
    private interface TestCase {
        void execute() throws Exception;
    }

    /* -------------------------------------------------------------
     * Picking service tests
     * ------------------------------------------------------------- */

    private void testPickingServiceReserveLogging() throws Exception {
        BigDecimal qty = new BigDecimal("2.00");
        PhaseETestSupport.TestTicketContext ctx = PhaseETestSupport.createEmptyPickingSession(
                "pick-service", new long[]{7L}, new BigDecimal[]{qty}, null);
        try {
            PhaseETestSupport.LineInfo info = ctx.lines.get(0);
            BigDecimal before;
            try (Connection conn = DbConnection.getConnection()) {
                before = queryBigDecimal(conn,
                        "SELECT reserved_qty FROM products WHERE product_id = ?",
                        info.productId);
            }
            PickingLine line = new PickingLine();
            line.setPickingId(ctx.pickingId);
            line.setTicketLineId(info.ticketLineId);
            line.setProductId(info.productId);
            line.setPickedQty(qty);
            line.setUom(info.uom);
            line.setUpdatedBy("pick-test");
            String scanRef = unique("scan");
            line.setScanRef(scanRef);

            pickingService.savePickedItems(ctx.pickingId, List.of(line));

            try (Connection conn = DbConnection.getConnection()) {
                BigDecimal reserved = queryBigDecimal(conn,
                        "SELECT reserved_qty FROM products WHERE product_id = ?",
                        info.productId);
                assertEquals(before.add(qty), reserved,
                        "Reserved qty should include new picks");

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT delta_reserved, delta_on_hand, note, source_ref FROM inventory_txn_log "
                                + "WHERE ticket_id = ? AND source_txn_type = 'RESERVE' "
                                + "ORDER BY log_id DESC LIMIT 1")) {
                    ps.setLong(1, ctx.ticketId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new AssertionError("Expected inventory_txn_log entry for picking reserve.");
                        }
                        BigDecimal deltaReserved = rs.getBigDecimal("delta_reserved");
                        BigDecimal deltaOnHand = rs.getBigDecimal("delta_on_hand");
                        assertEquals(qty, deltaReserved, "Reserve log should match picked quantity");
                        assertTrue(deltaOnHand.compareTo(BigDecimal.ZERO) == 0,
                                "Reserve log must not change on-hand quantity");
                        assertEquals("T2 Reserve", rs.getString("note"),
                                "Log note should identify reserve action");
                        assertEquals(scanRef, rs.getString("source_ref"),
                                "Log should carry scan/source reference");
                    }
                }
            }
        } finally {
            PhaseETestSupport.cleanupTicket(ctx);
        }
    }

    /* -------------------------------------------------------------
     * Pack service tests
     * ------------------------------------------------------------- */

    private void testPackOverQty() throws Exception {
        withTicket("pack-over", new long[]{3L}, new double[]{1}, ctx -> {
            PackBox box = new PackBox();
            box.setPickTicketId(ctx.ticketId);
            box.setPickingId(ctx.pickingId);
            box.setSourceRef(unique("pack-over"));
            box.setCreatedBy("pack-over");
            box.setUpdatedBy("pack-over");
            long boxId = packService.createBox(box);

            PhaseETestSupport.TestBox wrapper = new PhaseETestSupport.TestBox();
            wrapper.boxId = boxId;
            wrapper.ticketId = ctx.ticketId;
            wrapper.previousStatus = PhaseETestSupport.fetchTicketStatus(ctx.ticketId);
            try {
                List<PackBoxLine> lines = PhaseETestSupport.buildPackLines(
                        ctx, boxId, box.getSourceRef(), info -> info.requestedQty.add(BigDecimal.ONE));
                ValidationException ex = expectValidation(() -> packService.addLines(boxId, lines));
                assertEquals("PACK_OVER_QTY", ex.getCode(), "Expected PACK_OVER_QTY code");
                assertEquals(0,
                        queryInt("SELECT COUNT(*) FROM pack_box_line WHERE box_id = ?", boxId),
                        "Over-pack should not insert rows");
            } finally {
                PhaseETestSupport.deleteBox(wrapper);
            }
        });
    }

    private void testPackAlreadySealed() throws Exception {
        withTicket("pack-reseal", new long[]{4L}, new double[]{2}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) -> {
                    ValidationException ex = expectValidation(() -> packService.sealBox(box.boxId, "strap", "tester"));
                    assertEquals("PACK_ALREADY_SEALED", ex.getCode(), "Expected PACK_ALREADY_SEALED code");
                }));
    }

    private void testPackProductInactive() throws Exception {
        withTicket("pack-inactive", new long[]{5L}, new double[]{1}, ctx -> {
            long productId = ctx.lines.get(0).productId;
            String original = queryString("SELECT active_flag FROM products WHERE product_id = ?", productId);
            executeUpdate("UPDATE products SET active_flag = 0, updated_by = ?, updated_at = ? WHERE product_id = ?",
                    "pack-inactive", Timestamp.valueOf(LocalDateTime.now()), productId);

            PackBox box = new PackBox();
            box.setPickTicketId(ctx.ticketId);
            box.setPickingId(ctx.pickingId);
            box.setSourceRef(unique("pack-inactive"));
            box.setCreatedBy("pack-inactive");
            box.setUpdatedBy("pack-inactive");
            long boxId = packService.createBox(box);

            PhaseETestSupport.TestBox wrapper = new PhaseETestSupport.TestBox();
            wrapper.boxId = boxId;
            wrapper.ticketId = ctx.ticketId;
            wrapper.previousStatus = PhaseETestSupport.fetchTicketStatus(ctx.ticketId);
            try {
                List<PackBoxLine> lines = PhaseETestSupport.buildPackLines(
                        ctx, boxId, box.getSourceRef(), info -> info.requestedQty);
                ValidationException ex = expectValidation(() -> packService.addLines(boxId, lines));
                assertEquals("PACK_PRODUCT_INACTIVE", ex.getCode(), "Expected PACK_PRODUCT_INACTIVE code");
            } finally {
                PhaseETestSupport.deleteBox(wrapper);
                executeUpdate("UPDATE products SET active_flag = ?, updated_by = ?, updated_at = ? WHERE product_id = ?",
                        original, "pack-inactive-reset", Timestamp.valueOf(LocalDateTime.now()), productId);
            }
        });
    }

    private void testPackLineAlreadyBoxed() throws Exception {
        withTicket("pack-duplicate", new long[]{6L}, new double[]{2}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, firstBox) -> {
                    PackBox second = new PackBox();
                    second.setPickTicketId(context.ticketId);
                    second.setPickingId(context.pickingId);
                    second.setSourceRef(unique("pack-dup"));
                    second.setCreatedBy("pack-dup");
                    second.setUpdatedBy("pack-dup");
                    long boxId = packService.createBox(second);

                    PhaseETestSupport.TestBox wrapper = new PhaseETestSupport.TestBox();
                    wrapper.boxId = boxId;
                    wrapper.ticketId = context.ticketId;
                    wrapper.previousStatus = PhaseETestSupport.fetchTicketStatus(context.ticketId);
                    try {
                        List<PackBoxLine> duplicateLines = PhaseETestSupport.buildPackLines(
                                context, boxId, second.getSourceRef(), info -> info.requestedQty);
                        ValidationException ex = expectValidation(() -> packService.addLines(boxId, duplicateLines));
                        assertEquals("PACK_LINE_ALREADY_BOXED", ex.getCode(), "Expected PACK_LINE_ALREADY_BOXED code");
                    } finally {
                        PhaseETestSupport.deleteBox(wrapper);
                    }
                }));
    }

    @FunctionalInterface
    private interface TicketConsumer {
        void accept(PhaseETestSupport.TestTicketContext ctx) throws Exception;
    }

    @FunctionalInterface
    private interface BoxConsumer {
        void accept(PhaseETestSupport.TestTicketContext ctx, PhaseETestSupport.TestBox box) throws Exception;
    }

    @FunctionalInterface
    private interface DispatchConsumer {
        void accept(PhaseETestSupport.TestDispatch dispatch) throws Exception;
    }

    private void withTicket(String label,
                            long[] productIds,
                            double[] requested,
                            TicketConsumer consumer) throws Exception {
        BigDecimal[] quantities = new BigDecimal[requested.length];
        for (int i = 0; i < requested.length; i++) {
            quantities[i] = BigDecimal.valueOf(requested[i]);
        }
        PhaseETestSupport.TestTicketContext ctx = PhaseETestSupport.createTestTicket(label, productIds, quantities, null);
        try {
            consumer.accept(ctx);
        } finally {
            PhaseETestSupport.cleanupTicket(ctx);
        }
    }

    private void withBox(PhaseETestSupport.TestTicketContext ctx,
                         boolean seal,
                         java.util.function.Function<PhaseETestSupport.LineInfo, BigDecimal> qty,
                         BoxConsumer consumer) throws Exception {
        PhaseETestSupport.TestBox box = PhaseETestSupport.createBoxWithAllLines(
                ctx, packService, unique("box"), seal, qty);
        try {
            consumer.accept(ctx, box);
        } finally {
            PhaseETestSupport.deleteBox(box);
        }
    }

    private void withDispatch(PhaseETestSupport.TestTicketContext ctx,
                              PhaseETestSupport.TestBox box,
                              DispatchConsumer consumer) throws Exception {
        long vehicleId = PhaseETestSupport.findAvailableVehicleId();
        long driverId = PhaseETestSupport.findDispatcherId();
        PhaseETestSupport.TestDispatch dispatch = PhaseETestSupport.createDispatch(
                ctx, box, dispatchService,
                "MANIFEST-" + System.nanoTime(), vehicleId, driverId, unique("dispatch"));
        try {
            consumer.accept(dispatch);
        } finally {
            PhaseETestSupport.deleteDispatch(dispatch);
        }
    }

    /* -------------------------------------------------------------
     * Inventory helper tests
     * ------------------------------------------------------------- */

    private void testInventoryReserveDelta() throws Exception {
        withTicket("inv-reserve", new long[]{7L}, new double[]{3}, ctx -> {
            PhaseETestSupport.LineInfo line = ctx.lines.get(0);
            try (Connection conn = DbConnection.getConnection()) {
                conn.setAutoCommit(false);
                setLockTimeout(conn, 5);
                inventoryHelper.reserve(conn, line.productId, line.pickingLineId,
                        ctx.ticketId, new BigDecimal("1.00"),
                        unique("inv-reserve"), "inv-reserve");
                BigDecimal reserved = queryBigDecimal(conn,
                        "SELECT reserved_qty FROM products WHERE product_id = ?", line.productId);
                assertEquals(line.initialReserved.add(new BigDecimal("1.00")), reserved,
                        "Reserved qty should increase by reserve delta");
                conn.rollback();
            }
        });
    }

    private void testInventoryCloseDelta() throws Exception {
        withTicket("inv-close", new long[]{8L}, new double[]{4}, ctx -> {
            PhaseETestSupport.LineInfo line = ctx.lines.get(0);
            try (Connection conn = DbConnection.getConnection()) {
                conn.setAutoCommit(false);
                setLockTimeout(conn, 5);
                inventoryHelper.applyCloseAdjustment(conn, line.productId,
                        888L, ctx.ticketId,
                        line.requestedQty.negate(), line.requestedQty.negate(),
                        "inv-close", unique("inv-close"), "inv-close");
                BigDecimal reserved = queryBigDecimal(conn,
                        "SELECT reserved_qty FROM products WHERE product_id = ?", line.productId);
                BigDecimal onHand = queryBigDecimal(conn,
                        "SELECT on_hand_qty FROM products WHERE product_id = ?", line.productId);
                assertEquals(line.initialReserved.subtract(line.requestedQty), reserved,
                        "Reserved qty should decrease by delivered amount");
                assertEquals(line.initialOnHand.subtract(line.requestedQty), onHand,
                        "On-hand qty should decrease by delivered amount");
                conn.rollback();
            }
        });
    }

    private void testInventoryLockTimeout() throws Exception {
        withTicket("inv-lock", new long[]{9L}, new double[]{2}, ctx -> {
            PhaseETestSupport.LineInfo line = ctx.lines.get(0);
            Connection locker = null;
            try {
                locker = DbConnection.getConnection();
                locker.setAutoCommit(false);
                setLockTimeout(locker, 5);
                try (PreparedStatement ps = locker.prepareStatement(
                        "SELECT product_id FROM products WHERE product_id = ? FOR UPDATE")) {
                    ps.setLong(1, line.productId);
                    ps.executeQuery();
                }
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<SQLException> ref = new AtomicReference<>();
                Thread worker = new Thread(() -> {
                    try (Connection conn = DbConnection.getConnection()) {
                        conn.setAutoCommit(false);
                        setLockTimeout(conn, 1);
                        inventoryHelper.applyCloseAdjustment(conn, line.productId,
                                777L, ctx.ticketId,
                                BigDecimal.ONE.negate(), BigDecimal.ONE.negate(),
                                "inv-lock", unique("inv-lock"), "inv-lock");
                    } catch (SQLException e) {
                        ref.set(e);
                    } finally {
                        latch.countDown();
                    }
                });
                worker.start();
                latch.await();
                SQLException ex = ref.get();
                assertTrue(ex != null && ex.getMessage().toLowerCase().contains("lock wait timeout"),
                        "Expected lock wait timeout");
            } finally {
                if (locker != null) {
                    locker.rollback();
                    locker.close();
                }
            }
        });
    }
    /* -------------------------------------------------------------
     * Dispatch service tests
     * ------------------------------------------------------------- */

    private void testDispatchHappyPath() throws Exception {
        withTicket("dispatch-happy", new long[]{10L}, new double[]{3}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            assertEquals(PickTicketHdr.TicketStatus.Dispatched,
                                    PhaseETestSupport.fetchTicketStatus(context.ticketId),
                                    "Ticket should be Dispatched after manifest creation");
                            assertEquals(1,
                                    queryInt("SELECT COUNT(*) FROM dispatch_line WHERE dispatch_id = ?",
                                            dispatch.dispatchId),
                                    "Expected one dispatch line");
                        })));
    }

    private void testDispatchUnsealed() throws Exception {
        withTicket("dispatch-unsealed", new long[]{11L}, new double[]{2}, ctx ->
                withBox(ctx, false, info -> info.requestedQty, (context, box) -> {
                    DispatchHeader header = baseDispatchHeader(context.ticketId);
                    header.setSourceRef("dispatch-unsealed");
                    header.setManifestNo("MANIFEST-UNSEALED-" + System.nanoTime());
                    DispatchLine line = baseDispatchLine(box.boxId, "dispatch-unsealed");
                    ValidationException ex = expectValidation(() -> dispatchService.createDispatch(header, List.of(line)));
                    assertEquals("DISPATCH_UNSEALED_BOX", ex.getCode(), "Expected DISPATCH_UNSEALED_BOX code");
                }));
    }

    private void testDispatchCapacityExceeded() throws Exception {
        withTicket("dispatch-cap", new long[]{12L}, new double[]{2}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) -> {
                    long vehicleId = PhaseETestSupport.findAvailableVehicleId();
                    int originalCapacity = queryInt("SELECT capacity FROM vehicles WHERE vehicle_id = ?", vehicleId);
                    try {
                        executeUpdate("UPDATE vehicles SET capacity = 0, updated_by = ?, updated_at = ? WHERE vehicle_id = ?",
                                "dispatch-cap", Timestamp.valueOf(LocalDateTime.now()), vehicleId);
                        DispatchHeader header = baseDispatchHeader(context.ticketId);
                        header.setVehicleId(vehicleId);
                        header.setSourceRef("dispatch-cap");
                        header.setManifestNo("MANIFEST-CAP-" + System.nanoTime());
                        DispatchLine line = baseDispatchLine(box.boxId, "dispatch-cap");
                        ValidationException ex = expectValidation(() -> dispatchService.createDispatch(header, List.of(line)));
                        assertEquals("DISPATCH_CAPACITY_EXCEEDED", ex.getCode(), "Expected capacity exceeded code");
                    } finally {
                        executeUpdate("UPDATE vehicles SET capacity = ?, updated_by = ?, updated_at = ? WHERE vehicle_id = ?",
                                originalCapacity, "dispatch-cap-reset", Timestamp.valueOf(LocalDateTime.now()), vehicleId);
                    }
                }));
    }

    private void testDispatchVehicleUnavailable() throws Exception {
        withTicket("dispatch-unavailable", new long[]{18L}, new double[]{2}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) -> {
                    long vehicleId = PhaseETestSupport.findAvailableVehicleId();
                    String originalStatus = queryString("SELECT vehicle_status FROM vehicles WHERE vehicle_id = ?", vehicleId);
                    try {
                        executeUpdate("UPDATE vehicles SET vehicle_status = 'maintenance', updated_by = ?, updated_at = ? WHERE vehicle_id = ?",
                                "dispatch-unavailable", Timestamp.valueOf(LocalDateTime.now()), vehicleId);
                        DispatchHeader header = baseDispatchHeader(context.ticketId);
                        header.setVehicleId(vehicleId);
                        header.setSourceRef("dispatch-unavailable");
                        header.setManifestNo("MANIFEST-UNAV-" + System.nanoTime());
                        DispatchLine line = baseDispatchLine(box.boxId, "dispatch-unavailable");
                        ValidationException ex = expectValidation(() -> dispatchService.createDispatch(header, List.of(line)));
                        assertEquals("DISPATCH_VEHICLE_UNAVAILABLE", ex.getCode(), "Expected vehicle unavailable code");
                    } finally {
                        executeUpdate("UPDATE vehicles SET vehicle_status = ?, updated_by = ?, updated_at = ? WHERE vehicle_id = ?",
                                originalStatus, "dispatch-unavailable-reset", Timestamp.valueOf(LocalDateTime.now()), vehicleId);
                    }
                }));
    }

    private void testDispatchDuplicateBox() throws Exception {
        withTicket("dispatch-dup", new long[]{20L}, new double[]{3}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            DispatchHeader header = baseDispatchHeader(context.ticketId);
                            header.setSourceRef("dispatch-dup2");
                            header.setManifestNo("MANIFEST-DUP2-" + System.nanoTime());
                            DispatchLine line = baseDispatchLine(box.boxId, "dispatch-dup2");
                            ValidationException ex = expectValidation(() -> dispatchService.createDispatch(header, List.of(line)));
                            assertEquals("DISPATCH_BOX_ALREADY_LOADED", ex.getCode(), "Expected duplicate box code");
                        })));
    }
    /* -------------------------------------------------------------
     * Close service tests
     * ------------------------------------------------------------- */

    private void testCloseDeliveredFlow() throws Exception {
        withTicket("close-delivered", new long[]{18L, 19L}, new double[]{2, 3}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            CloseHeader header = baseCloseHeader(context.ticketId, dispatch.dispatchId);
                            header.setFinalStatus(CloseHeader.FinalStatus.Delivered);
                            header.setSourceRef("close-delivered");
                            List<CloseVariance> variances = new ArrayList<>();
                            int counter = 1;
                            for (PhaseETestSupport.LineInfo line : context.lines) {
                                variances.add(baseVariance(line, line.requestedQty, BigDecimal.ZERO,
                                        "close-delivered-var-" + counter++));
                            }
                            closeService.closeTicket(header, variances);
                            assertEquals(PickTicketHdr.TicketStatus.Delivered,
                                    PhaseETestSupport.fetchTicketStatus(context.ticketId),
                                    "Ticket should be Delivered");
                        })));
    }

    private void testCloseShortFlow() throws Exception {
        withTicket("close-short", new long[]{17L}, new double[]{5}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            CloseHeader header = baseCloseHeader(context.ticketId, dispatch.dispatchId);
                            header.setFinalStatus(CloseHeader.FinalStatus.ShortClosed);
                            header.setSourceRef("close-short");
                            PhaseETestSupport.LineInfo line = context.lines.get(0);
                            CloseVariance variance = baseVariance(line, new BigDecimal("3.00"), new BigDecimal("2.00"),
                                    "close-short-var");
                            closeService.closeTicket(header, List.of(variance));
                            assertEquals(PickTicketHdr.TicketStatus.ShortClosed,
                                    PhaseETestSupport.fetchTicketStatus(context.ticketId),
                                    "Ticket should be Short-Closed");
                        })));
    }

    private void testCloseMismatch() throws Exception {
        withTicket("close-mismatch", new long[]{18L}, new double[]{4}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            CloseHeader header = baseCloseHeader(context.ticketId, dispatch.dispatchId);
                            header.setFinalStatus(CloseHeader.FinalStatus.Delivered);
                            header.setSourceRef("close-mismatch");
                            PhaseETestSupport.LineInfo line = context.lines.get(0);
                            CloseVariance variance = baseVariance(line, line.requestedQty, BigDecimal.ONE,
                                    "close-mismatch-var");
                            ValidationException ex = expectValidation(() -> closeService.closeTicket(header, List.of(variance)));
                            assertEquals("CLOSE_RECONCILE_MISMATCH", ex.getCode(), "Expected mismatch code");
                        })));
    }

    private void testCloseInvalidQty() throws Exception {
        withTicket("close-invalid", new long[]{19L}, new double[]{3}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            CloseHeader header = baseCloseHeader(context.ticketId, dispatch.dispatchId);
                            header.setFinalStatus(CloseHeader.FinalStatus.Delivered);
                            header.setSourceRef("close-invalid");
                            PhaseETestSupport.LineInfo line = context.lines.get(0);
                            CloseVariance variance = baseVariance(line, new BigDecimal("-1.00"), new BigDecimal("4.00"),
                                    "close-invalid-var");
                            ValidationException ex = expectValidation(() -> closeService.closeTicket(header, List.of(variance)));
                            assertEquals("CLOSE_INVALID_QTY", ex.getCode(), "Expected invalid qty code");
                        })));
    }

    private void testCloseLockTimeout() throws Exception {
        withTicket("close-lock", new long[]{20L}, new double[]{2}, ctx ->
                withBox(ctx, true, info -> info.requestedQty, (context, box) ->
                        withDispatch(context, box, dispatch -> {
                            PhaseETestSupport.LineInfo line = context.lines.get(0);
                            Connection locker = null;
                            try {
                                locker = DbConnection.getConnection();
                                locker.setAutoCommit(false);
                                setLockTimeout(locker, 5);
                                try (PreparedStatement ps = locker.prepareStatement(
                                        "SELECT product_id FROM products WHERE product_id = ? FOR UPDATE")) {
                                    ps.setLong(1, line.productId);
                                    ps.executeQuery();
                                }
                                CloseHeader header = baseCloseHeader(context.ticketId, dispatch.dispatchId);
                                header.setFinalStatus(CloseHeader.FinalStatus.Delivered);
                                header.setSourceRef("close-lock");
                                CloseVariance variance = baseVariance(line, line.requestedQty, BigDecimal.ZERO,
                                        "close-lock-var");
                                ValidationException ex = expectValidation(() -> closeService.closeTicket(header, List.of(variance)));
                                assertEquals("CLOSE_INVENTORY_LOCK_TIMEOUT", ex.getCode(), "Expected lock timeout code");
                            } finally {
                                if (locker != null) {
                                    locker.rollback();
                                    locker.close();
                                }
                            }
                        })));
    }
    private DispatchHeader baseDispatchHeader(long ticketId) throws SQLException {
        DispatchHeader header = new DispatchHeader();
        header.setPickTicketId(ticketId);
        header.setVehicleId(PhaseETestSupport.findAvailableVehicleId());
        header.setDriverId(PhaseETestSupport.findDispatcherId());
        header.setManifestNo("MANIFEST-" + System.nanoTime());
        header.setCreatedBy("dispatch-test");
        header.setUpdatedBy("dispatch-test");
        return header;
    }

    private DispatchLine baseDispatchLine(long boxId, String ref) {
        DispatchLine line = new DispatchLine();
        line.setBoxId(boxId);
        line.setSourceRef(ref + "-line");
        line.setCreatedBy(ref);
        line.setUpdatedBy(ref);
        return line;
    }

    private CloseHeader baseCloseHeader(long ticketId, long dispatchId) {
        CloseHeader header = new CloseHeader();
        header.setPickTicketId(ticketId);
        header.setDispatchId(dispatchId);
        header.setPodRef("POD-" + System.nanoTime());
        header.setPodTs(LocalDateTime.now());
        header.setNotes("auto");
        header.setCreatedBy("close-test");
        header.setUpdatedBy("close-test");
        return header;
    }

    private CloseVariance baseVariance(PhaseETestSupport.LineInfo line,
                                       BigDecimal delivered,
                                       BigDecimal shortQty,
                                       String ref) {
        CloseVariance variance = new CloseVariance();
        variance.setTicketLineId(line.ticketLineId);
        variance.setRequestedQty(line.requestedQty);
        variance.setDeliveredQty(delivered);
        variance.setShortQty(shortQty);
        variance.setReason(shortQty.signum() == 0 ? null : "short");
        variance.setSourceRef(ref);
        variance.setCreatedBy("close-test");
        variance.setUpdatedBy("close-test");
        return variance;
    }
    private interface TestCaseWithException {
        void execute() throws Exception;
    }

    private ValidationException expectValidation(TestCaseWithException executable) throws Exception {
        try {
            executable.execute();
        } catch (ValidationException ex) {
            return ex;
        }
        throw new AssertionError("Expected ValidationException but none thrown");
    }

    private <T> void assertEquals(T expected, T actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private boolean queryBoolean(String sql, Object... params) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean(1);
                }
            }
        }
        throw new SQLException("No result for query: " + sql);
    }

    private int queryInt(String sql, Object... params) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No result for query: " + sql);
    }

    private String queryString(String sql, Object... params) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private BigDecimal queryBigDecimal(Connection conn, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        }
        throw new SQLException("No result for query: " + sql);
    }

    private static void executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            ps.executeUpdate();
        }
    }

    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof String s) {
                ps.setString(i + 1, s);
            } else if (value instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else if (value instanceof Long n) {
                ps.setLong(i + 1, n);
            } else if (value instanceof BigDecimal bd) {
                ps.setBigDecimal(i + 1, bd);
            } else if (value instanceof Timestamp ts) {
                ps.setTimestamp(i + 1, ts);
            } else if (value == null) {
                ps.setObject(i + 1, null);
            } else {
                ps.setObject(i + 1, value);
            }
        }
    }

    private static void setLockTimeout(Connection conn, int seconds) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SET innodb_lock_wait_timeout = ?")) {
            ps.setInt(1, seconds);
            ps.execute();
        }
    }

    private static String unique(String prefix) {
        return prefix + "-" + System.nanoTime();
    }
}

