package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PickingDaoImpl implements PickingDao {

    // -------------------- CREATE --------------------
    @Override
    public long insertPickingHeader(PickingHdr hdr, Connection conn) throws SQLException {
        String insertSQL = """
            INSERT INTO picking_hdr (pick_ticket_id, picker_employee_id, picking_status, updated_by)
            VALUES (?, ?, ?, ?)
            """;
            try (PreparedStatement stmt = conn.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, hdr.getPickTicketId());
            stmt.setLong(2, hdr.getPickerEmployeeId());
            stmt.setString(3, hdr.getPickingStatus());
            stmt.setString(4, hdr.getUpdatedBy());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert picking header.");
    }


    @Override
    public void insertPickingLines(long pickingId, List<PickingLine> lines, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO picking_line
              (picking_id, ticket_line_id, product_id, picked_qty, uom, short_reason, scan_ref, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (PickingLine line : lines) {
                stmt.setLong(1, pickingId);
                stmt.setLong(2, line.getTicketLineId());
                stmt.setLong(3, line.getProductId());
                stmt.setBigDecimal(4, line.getPickedQty());
                stmt.setString(5, line.getUom());
                stmt.setString(6, line.getShortReason());
                stmt.setString(7, line.getScanRef());
                stmt.setString(8, line.getUpdatedBy());
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        line.setPickingLineId(rs.getLong(1));
                    }
                }
            }
        }
    }


    // -------------------- READ --------------------
    @Override
    public PickingHdr findByTicketId(long pickTicketId) throws SQLException {
        String sql = "SELECT * FROM picking_hdr WHERE pick_ticket_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
