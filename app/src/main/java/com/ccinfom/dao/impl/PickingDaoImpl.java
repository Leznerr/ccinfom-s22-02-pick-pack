/*
 * CCINFOM — Phase D
 * File: PickingDaoImpl.java
 * Purpose: JDBC implementation of PickingDao.
 *
 * TODOs:
 *  [ ] insertPickingHeader(): try insert; if duplicate (unique by ticket), fetch existing id.
 *  [ ] insertPickingLines(): batch insert with anti-dup filter.
 *
 * Definition of Done:
 *  - Idempotent behavior (re-running inserts does not duplicate rows).
 *  - Works with triggers: ticket status flips to 'Picking'.
 */

package com.ccinfom.dao.impl;

import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

 public class PickingDaoImpl implements PickingDao {

    private final Connection conn;

    public PickingDaoImpl(Connection conn) {
        this.conn = conn;
    }

    // -------------------- CREATE --------------------
    @Override
    public long insertPickingHeader(PickingHdr hdr) throws SQLException {
        String insertSQL = """
            INSERT INTO picking_hdr (pick_ticket_id, picker_employee_id, picking_status, updated_by)
            VALUES (?, ?, ?, ?)
            """;

        String selectSQL = "SELECT picking_id FROM picking_hdr WHERE pick_ticket_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, hdr.getPickTicketId());
            stmt.setLong(2, hdr.getPickerEmployeeId());
            stmt.setString(3, hdr.getPickingStatus());
            stmt.setString(4, hdr.getUpdatedBy());
            int affected = stmt.executeUpdate();

            System.out.println("Insert affected rows: " + affected);

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    long id = rs.getLong(1);
                    System.out.println("Inserted new picking_id: " + id);
                    return id;
                }
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            System.out.println("Duplicate entry detected for pick_ticket_id=" + hdr.getPickTicketId());
            try (PreparedStatement stmt = conn.prepareStatement(selectSQL)) {
                stmt.setLong(1, hdr.getPickTicketId());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long existing = rs.getLong("picking_id");
                        System.out.println("Existing picking_id found: " + existing);
                        return existing;
                    } else {
                        System.out.println("No picking_hdr row found for that ticket_id despite duplicate error!");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL ErrorState: " + e.getSQLState());
            System.err.println("SQL ErrorCode: " + e.getErrorCode());
            e.printStackTrace();
            throw e;
        }

        // If both insert and lookup failed:
        System.err.println("Insert failed for pick_ticket_id=" + hdr.getPickTicketId() +
                        ", picker_employee_id=" + hdr.getPickerEmployeeId());
        throw new SQLException("Failed to insert or retrieve picking header.");
    }


    @Override
    public void insertPickingLines(long pickingId, List<PickingLine> lines) throws SQLException {
        String sql = """
            INSERT IGNORE INTO picking_line
              (picking_id, ticket_line_id, product_id, picked_qty, uom, short_reason, scan_ref, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PickingLine line : lines) {
                stmt.setLong(1, pickingId);
                stmt.setLong(2, line.getTicketLineId());
                stmt.setLong(3, line.getProductId());
                stmt.setBigDecimal(4, line.getPickedQty());
                stmt.setString(5, line.getUom());
                stmt.setString(6, line.getShortReason());
                stmt.setString(7, line.getScanRef());
                stmt.setString(8, line.getUpdatedBy());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }


    // -------------------- READ --------------------
    @Override
    public PickingHdr findByTicketId(long pickTicketId) throws SQLException {
        String sql = "SELECT * FROM picking_hdr WHERE pick_ticket_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickTicketId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapHeader(rs);
                }
            }
        }
        return null;
    }

    public List<PickingLine> listLinesByPickingId(long pickingId) throws SQLException {
        String sql = "SELECT * FROM picking_line WHERE picking_id = ?";
        List<PickingLine> lines = new ArrayList<>();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(mapLine(rs));
                }
            }
        }
        return lines;
    }

    // -------------------- MAPPING HELPERS --------------------

    private PickingHdr mapHeader(ResultSet rs) throws SQLException {
        PickingHdr hdr = new PickingHdr();
        hdr.setPickingId(rs.getLong("picking_id"));
        hdr.setPickTicketId(rs.getLong("pick_ticket_id"));
        hdr.setPickerEmployeeId(rs.getLong("picker_employee_id"));
        hdr.setPickingStatus(rs.getString("picking_status"));
        Timestamp started = rs.getTimestamp("started_at");
        if (started != null) hdr.setStartedAt(started.toLocalDateTime());
        Timestamp completed = rs.getTimestamp("completed_at");
        if (completed != null) hdr.setCompletedAt(completed.toLocalDateTime());
        hdr.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        hdr.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        hdr.setUpdatedBy(rs.getString("updated_by"));
        return hdr;
    }

    private PickingLine mapLine(ResultSet rs) throws SQLException {
        PickingLine line = new PickingLine();
        line.setPickingLineId(rs.getLong("picking_line_id"));
        line.setPickingId(rs.getLong("picking_id"));
        line.setTicketLineId(rs.getLong("ticket_line_id"));
        line.setProductId(rs.getLong("product_id"));
        line.setPickedQty(rs.getBigDecimal("picked_qty"));
        line.setUom(rs.getString("uom"));
        line.setShortReason(rs.getString("short_reason"));
        line.setScanRef(rs.getString("scan_ref"));
        line.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        line.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        line.setUpdatedBy(rs.getString("updated_by"));
        return line;
    }

 }