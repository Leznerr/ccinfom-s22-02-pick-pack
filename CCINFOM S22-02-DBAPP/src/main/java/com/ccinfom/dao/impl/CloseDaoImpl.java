package com.ccinfom.dao.impl;

import com.ccinfom.dao.interfaces.CloseDao;
import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CloseDaoImpl implements CloseDao {

    private static final String INSERT_HEADER_SQL = """
        INSERT INTO close_hdr
            (pick_ticket_id, dispatch_id, final_status, pod_ref, pod_ts, notes, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String INSERT_VARIANCE_SQL = """
        INSERT INTO close_variance
            (close_id, ticket_line_id, requested_qty, delivered_qty, short_qty, reason, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    @Override
    public long insertCloseHeader(CloseHeader header, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_HEADER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, header.getPickTicketId());
            stmt.setLong(2, header.getDispatchId());
            stmt.setString(3, header.getFinalStatus().getDbValue());
            stmt.setString(4, header.getPodRef());
            if (header.getPodTs() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(header.getPodTs()));
            } else {
                stmt.setNull(5, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(6, header.getNotes());
            stmt.setString(7, header.getSourceRef());
            stmt.setString(8, header.getCreatedBy());
            stmt.setString(9, header.getUpdatedBy());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert close header.");
    }

    @Override
    public void insertCloseVariances(long closeId, List<CloseVariance> variances, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_VARIANCE_SQL)) {
            for (CloseVariance variance : variances) {
                stmt.setLong(1, closeId);
                stmt.setLong(2, variance.getTicketLineId());
                stmt.setBigDecimal(3, variance.getRequestedQty());
                stmt.setBigDecimal(4, variance.getDeliveredQty());
                stmt.setBigDecimal(5, variance.getShortQty());
                stmt.setString(6, variance.getReason());
                stmt.setString(7, variance.getSourceRef());
                stmt.setString(8, variance.getCreatedBy());
                stmt.setString(9, variance.getUpdatedBy());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public Optional<CloseHeader> findByTicketId(long pickTicketId, Connection conn) throws SQLException {
        String sql = """
            SELECT *
              FROM close_hdr
             WHERE pick_ticket_id = ?
          ORDER BY created_at DESC
             LIMIT 1
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickTicketId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHeader(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<CloseVariance> listVariancesByCloseId(long closeId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM close_variance WHERE close_id = ?";
        List<CloseVariance> variances = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, closeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    variances.add(mapVariance(rs));
                }
            }
        }
        return variances;
    }

    private CloseHeader mapHeader(ResultSet rs) throws SQLException {
        CloseHeader header = new CloseHeader();
        header.setCloseId(rs.getLong("close_id"));
        header.setPickTicketId(rs.getLong("pick_ticket_id"));
        header.setDispatchId(rs.getLong("dispatch_id"));
        header.setFinalStatus(CloseHeader.FinalStatus.fromDb(rs.getString("final_status")));
        header.setPodRef(rs.getString("pod_ref"));
        Timestamp podTs = rs.getTimestamp("pod_ts");
        if (podTs != null) {
            header.setPodTs(podTs.toLocalDateTime());
        }
        header.setNotes(rs.getString("notes"));
        header.setSourceRef(rs.getString("source_ref"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            header.setCreatedAt(created.toLocalDateTime());
        }
        header.setCreatedBy(rs.getString("created_by"));
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            header.setUpdatedAt(updated.toLocalDateTime());
        }
        header.setUpdatedBy(rs.getString("updated_by"));
        return header;
    }

    private CloseVariance mapVariance(ResultSet rs) throws SQLException {
        CloseVariance variance = new CloseVariance();
        variance.setVarianceId(rs.getLong("variance_id"));
        variance.setCloseId(rs.getLong("close_id"));
        variance.setTicketLineId(rs.getLong("ticket_line_id"));
        variance.setRequestedQty(rs.getBigDecimal("requested_qty"));
        variance.setDeliveredQty(rs.getBigDecimal("delivered_qty"));
        variance.setShortQty(rs.getBigDecimal("short_qty"));
        variance.setReason(rs.getString("reason"));
        variance.setSourceRef(rs.getString("source_ref"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            variance.setCreatedAt(created.toLocalDateTime());
        }
        variance.setCreatedBy(rs.getString("created_by"));
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            variance.setUpdatedAt(updated.toLocalDateTime());
        }
        variance.setUpdatedBy(rs.getString("updated_by"));
        return variance;
    }
}

