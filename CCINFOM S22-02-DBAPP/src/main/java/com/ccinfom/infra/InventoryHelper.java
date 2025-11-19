package com.ccinfom.infra;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Centralised inventory adjustment helper used by T2 (reserve) and T5 (close).
 * Ensures a single pessimistic-locking path and consistent audit logging.
 */
public class InventoryHelper {

    public enum SourceTxnType {
        RESERVE, CLOSE
    }

    public static final class InventoryAdjustment {
        private final long productId;
        private final BigDecimal previousReserved;
        private final BigDecimal previousOnHand;
        private final BigDecimal newReserved;
        private final BigDecimal newOnHand;

        InventoryAdjustment(long productId,
                            BigDecimal previousReserved,
                            BigDecimal previousOnHand,
                            BigDecimal newReserved,
                            BigDecimal newOnHand) {
            this.productId = productId;
            this.previousReserved = previousReserved;
            this.previousOnHand = previousOnHand;
            this.newReserved = newReserved;
            this.newOnHand = newOnHand;
        }

        public long getProductId() {
            return productId;
        }

        public BigDecimal getPreviousReserved() {
            return previousReserved;
        }

        public BigDecimal getPreviousOnHand() {
            return previousOnHand;
        }

        public BigDecimal getNewReserved() {
            return newReserved;
        }

        public BigDecimal getNewOnHand() {
            return newOnHand;
        }
    }

    private static final String LOCK_PRODUCT_SQL = """
        SELECT on_hand_qty, reserved_qty
          FROM products
         WHERE product_id = ?
         FOR UPDATE
        """;

    private static final String UPDATE_PRODUCT_SQL = """
        UPDATE products
           SET on_hand_qty = ?,
               reserved_qty = ?,
               updated_by = ?,
               updated_at = CURRENT_TIMESTAMP
         WHERE product_id = ?
        """;

    private static final String INSERT_LOG_SQL = """
        INSERT INTO inventory_txn_log
            (product_id, warehouse_id, ticket_id, source_txn_type, source_txn_id,
             delta_reserved, delta_on_hand, note, source_ref, created_by, updated_by)
        VALUES (?, NULL, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    public InventoryAdjustment applyDelta(Connection conn,
                                          long productId,
                                          SourceTxnType sourceType,
                                          long sourceTxnId,
                                          Long ticketId,
                                          BigDecimal deltaReserved,
                                          BigDecimal deltaOnHand,
                                          String note,
                                          String sourceRef,
                                          String user) throws SQLException {
        Objects.requireNonNull(conn, "Connection is required");
        Objects.requireNonNull(sourceType, "sourceType is required");
        Objects.requireNonNull(user, "user is required");
        Objects.requireNonNull(note, "note is required");
        Objects.requireNonNull(sourceRef, "sourceRef is required");

        BigDecimal deltaRes = deltaReserved == null ? BigDecimal.ZERO : deltaReserved;
        BigDecimal deltaOnh = deltaOnHand == null ? BigDecimal.ZERO : deltaOnHand;

        BigDecimal currentReserved;
        BigDecimal currentOnHand;

        try (PreparedStatement lockStmt = conn.prepareStatement(LOCK_PRODUCT_SQL)) {
            lockStmt.setLong(1, productId);
            try (ResultSet rs = lockStmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Product not found for inventory adjustment: " + productId);
                }
                currentOnHand = rs.getBigDecimal("on_hand_qty");
                currentReserved = rs.getBigDecimal("reserved_qty");
            }
        }

        if (currentOnHand == null) {
            currentOnHand = BigDecimal.ZERO;
        }
        if (currentReserved == null) {
            currentReserved = BigDecimal.ZERO;
        }

        BigDecimal newReserved = currentReserved.add(deltaRes);
        BigDecimal newOnHand = currentOnHand.add(deltaOnh);

        if (newReserved.compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("Inventory adjustment would result in negative reserved_qty for product " + productId);
        }
        if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new SQLException("Inventory adjustment would result in negative on_hand_qty for product " + productId);
        }

        try (PreparedStatement updateStmt = conn.prepareStatement(UPDATE_PRODUCT_SQL)) {
            updateStmt.setBigDecimal(1, newOnHand);
            updateStmt.setBigDecimal(2, newReserved);
            updateStmt.setString(3, user);
            updateStmt.setLong(4, productId);
            updateStmt.executeUpdate();
        }

        boolean shouldLog = deltaRes.compareTo(BigDecimal.ZERO) != 0
                || deltaOnh.compareTo(BigDecimal.ZERO) != 0;
        if (shouldLog) {
            try (PreparedStatement logStmt = conn.prepareStatement(INSERT_LOG_SQL)) {
                logStmt.setLong(1, productId);
                if (ticketId != null) {
                    logStmt.setLong(2, ticketId);
                } else {
                    logStmt.setNull(2, java.sql.Types.BIGINT);
                }
                logStmt.setString(3, sourceType.name());
                logStmt.setLong(4, sourceTxnId);
                logStmt.setBigDecimal(5, deltaRes);
                logStmt.setBigDecimal(6, deltaOnh);
                logStmt.setString(7, note);
                logStmt.setString(8, sourceRef);
                logStmt.setString(9, user);
                logStmt.setString(10, user);
                logStmt.executeUpdate();
            }
        }

        return new InventoryAdjustment(productId, currentReserved, currentOnHand, newReserved, newOnHand);
    }

    public InventoryAdjustment reserve(Connection conn,
                                       long productId,
                                       long pickingLineId,
                                       long ticketId,
                                       BigDecimal quantity,
                                       String sourceRef,
                                       String user) throws SQLException {
        return applyDelta(conn, productId, SourceTxnType.RESERVE, pickingLineId, ticketId,
                quantity, BigDecimal.ZERO, "T2 Reserve", sourceRef, user);
    }

    public InventoryAdjustment applyCloseAdjustment(Connection conn,
                                                    long productId,
                                                    long closeId,
                                                    long ticketId,
                                                    BigDecimal deltaReserved,
                                                    BigDecimal deltaOnHand,
                                                    String note,
                                                    String sourceRef,
                                                    String user) throws SQLException {
        return applyDelta(conn, productId, SourceTxnType.CLOSE, closeId, ticketId,
                deltaReserved, deltaOnHand, note, sourceRef, user);
    }
}

