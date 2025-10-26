package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TicketDaoImpl implements TicketDao {

    // -------------------- CREATE --------------------

    @Override
    public long insertTicketHeader(PickTicketHdr hdr, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO pick_ticket_hdr (customer_id, branch_id, ticket_status, remarks, updated_by)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, hdr.getCustomerId());
            stmt.setLong(2, hdr.getBranchId());
            stmt.setString(3, hdr.getTicketStatus().name());
            stmt.setString(4, hdr.getRemarks());
            stmt.setString(5, hdr.getUpdatedBy());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating ticket header failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating ticket header failed, no ID obtained.");
                }
            }
        }
    }

    @Override
    public void insertTicketLines(List<PickTicketLine> lines, Connection conn) throws SQLException {
        String sql = """
            INSERT INTO pick_ticket_line
              (pick_ticket_id, product_id, requested_qty, uom, line_status, updated_by)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (PickTicketLine line : lines) {
                stmt.setLong(1, line.getPickTicketId());
                stmt.setLong(2, line.getProductId());
                stmt.setBigDecimal(3, line.getRequestedQty());
                stmt.setString(4, line.getUom());
                stmt.setString(5, line.getLineStatus().name());
                stmt.setString(6, line.getUpdatedBy());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // -------------------- READ --------------------

    @Override
    public List<PickTicketHdr> listAllTickets() throws SQLException {
        String sql = "SELECT * FROM pick_ticket_hdr ORDER BY created_at DESC";
        List<PickTicketHdr> tickets = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                tickets.add(mapHeader(rs));
            }
        }
        return tickets;
    }

    @Override
    public PickTicketHdr findTicketById(long pickTicketId) throws SQLException {
        String sql = "SELECT * FROM pick_ticket_hdr WHERE pick_ticket_id = ?";
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

    @Override
    public List<PickTicketLine> listTicketLines(long pickTicketId) throws SQLException {
        String sql = "SELECT * FROM pick_ticket_line WHERE pick_ticket_id = ?";
        List<PickTicketLine> lines = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickTicketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(mapLine(rs));
                }
            }
        }
        return lines;
    }

    // -------------------- UPDATE --------------------

    @Override
    public void updateTicketStatus(long pickTicketId, PickTicketHdr.TicketStatus status, String updatedBy) throws SQLException {
        try (Connection conn = DbConnection.getConnection()) {
            updateTicketStatus(pickTicketId, status, updatedBy, conn);
        }
    }

    @Override
    public void updateTicketStatus(long pickTicketId, PickTicketHdr.TicketStatus status, String updatedBy, Connection conn) throws SQLException {
        String sql = """
            UPDATE pick_ticket_hdr
            SET ticket_status = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE pick_ticket_id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, updatedBy);
            stmt.setLong(3, pickTicketId);
            stmt.executeUpdate();
        }
    }

    // -------------------- DELETE / CLOSE --------------------

    @Override
    public void closeOrCancelTicket(long pickTicketId, PickTicketHdr.TicketStatus status, String updatedBy) throws SQLException {
        // Simply delegates to updateTicketStatus for now
        updateTicketStatus(pickTicketId, status, updatedBy);
    }

    // -------------------- MAPPING HELPERS --------------------

    private PickTicketHdr mapHeader(ResultSet rs) throws SQLException {
        PickTicketHdr hdr = new PickTicketHdr();
        hdr.setPickTicketId(rs.getLong("pick_ticket_id"));
        hdr.setCustomerId(rs.getLong("customer_id"));
        hdr.setBranchId(rs.getLong("branch_id"));
        hdr.setTicketStatus(PickTicketHdr.TicketStatus.valueOf(rs.getString("ticket_status")));
        hdr.setRemarks(rs.getString("remarks"));
        hdr.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        hdr.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        hdr.setUpdatedBy(rs.getString("updated_by"));
        return hdr;
    }

    private PickTicketLine mapLine(ResultSet rs) throws SQLException {
        PickTicketLine line = new PickTicketLine();
        line.setTicketLineId(rs.getLong("ticket_line_id"));
        line.setPickTicketId(rs.getLong("pick_ticket_id"));
        line.setProductId(rs.getLong("product_id"));
        line.setRequestedQty(rs.getBigDecimal("requested_qty"));
        line.setUom(rs.getString("uom"));
        line.setLineStatus(PickTicketLine.LineStatus.valueOf(rs.getString("line_status")));
        line.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        line.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        line.setUpdatedBy(rs.getString("updated_by"));
        return line;
    }
}