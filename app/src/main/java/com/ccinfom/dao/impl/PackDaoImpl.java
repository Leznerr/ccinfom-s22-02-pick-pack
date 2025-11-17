package com.ccinfom.dao.impl;

import com.ccinfom.dao.interfaces.PackDao;
import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PackDaoImpl implements PackDao {

    private static final String INSERT_BOX_SQL = """
        INSERT INTO pack_box_hdr
            (pick_ticket_id, picking_id, sealed_flag, seal_method, sealed_at, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String INSERT_LINE_SQL = """
        INSERT INTO pack_box_line
            (box_id, picking_line_id, packed_qty, uom, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_SEAL_SQL = """
        UPDATE pack_box_hdr
           SET sealed_flag = ?, seal_method = ?, sealed_at = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP
         WHERE box_id = ?
        """;

    @Override
    public long insertBox(PackBox box, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_BOX_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, box.getPickTicketId());
            stmt.setLong(2, box.getPickingId());
            stmt.setBoolean(3, box.isSealedFlag());
            stmt.setString(4, box.getSealMethod());
            if (box.getSealedAt() != null) {
                stmt.setTimestamp(5, java.sql.Timestamp.valueOf(box.getSealedAt()));
            } else {
                stmt.setNull(5, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(6, box.getSourceRef());
            stmt.setString(7, box.getCreatedBy());
            stmt.setString(8, box.getUpdatedBy());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert pack_box_hdr record.");
    }

    @Override
    public void insertBoxLines(long boxId, List<PackBoxLine> lines, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_LINE_SQL)) {
            for (PackBoxLine line : lines) {
                stmt.setLong(1, boxId);
                stmt.setLong(2, line.getPickingLineId());
                stmt.setBigDecimal(3, line.getPackedQty());
                stmt.setString(4, line.getUom());
                stmt.setString(5, line.getSourceRef());
                stmt.setString(6, line.getCreatedBy());
                stmt.setString(7, line.getUpdatedBy());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public void sealBox(long boxId, String sealMethod, String updatedBy, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(UPDATE_SEAL_SQL)) {
            stmt.setBoolean(1, true);
            stmt.setString(2, sealMethod);
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, updatedBy);
            stmt.setLong(5, boxId);
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<PackBox> findBoxById(long boxId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM pack_box_hdr WHERE box_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, boxId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBox(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<PackBox> findOpenBoxByPickingId(long pickingId, Connection conn) throws SQLException {
        String sql = """
            SELECT *
              FROM pack_box_hdr
             WHERE picking_id = ?
               AND sealed_flag = 0
             ORDER BY updated_at DESC
             LIMIT 1
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickingId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapBox(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<PackBoxLine> listLinesByBoxId(long boxId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM pack_box_line WHERE box_id = ?";
        List<PackBoxLine> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, boxId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(mapLine(rs));
                }
            }
        }
        return lines;
    }

    @Override
    public List<Long> listPackedLineIdsByPicking(long pickingId, Connection conn) throws SQLException {
        String sql = """
            SELECT pbl.picking_line_id
              FROM pack_box_line pbl
              JOIN pack_box_hdr pb ON pb.box_id = pbl.box_id
             WHERE pb.picking_id = ?
            """;
        List<Long> ids = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickingId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getLong(1));
                }
            }
        }
        return ids;
    }

    @Override
    public boolean existsPackedLineForPickingLine(long pickingLineId, Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM pack_box_line WHERE picking_line_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickingLineId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Long findBoxIdByPickingLine(long pickingLineId, Connection conn) throws SQLException {
        String sql = "SELECT box_id FROM pack_box_line WHERE picking_line_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickingLineId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return null;
    }

    @Override
    public boolean isTicketFullyPacked(long pickTicketId, Connection conn) throws SQLException {
        String sql = """
            SELECT
                COUNT(pl.picking_line_id) AS total_lines,
                COUNT(pbl.picking_line_id) AS packed_lines,
                COALESCE(SUM(pl.picked_qty), 0) AS total_picked_qty,
                COALESCE(SUM(pbl.packed_qty), 0) AS total_packed_qty
            FROM picking_line pl
            JOIN picking_hdr ph ON ph.picking_id = pl.picking_id
            LEFT JOIN pack_box_line pbl ON pbl.picking_line_id = pl.picking_line_id
            WHERE ph.pick_ticket_id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, pickTicketId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int totalLines = rs.getInt("total_lines");
                    int packedLines = rs.getInt("packed_lines");
                    BigDecimal totalPicked = rs.getBigDecimal("total_picked_qty");
                    BigDecimal totalPacked = rs.getBigDecimal("total_packed_qty");

                    if (totalLines == 0) {
                        return false;
                    }
                    if (packedLines < totalLines) {
                        return false;
                    }
                    return totalPicked != null && totalPacked != null
                            && totalPicked.compareTo(totalPacked) == 0;
                }
            }
        }
        return false;
    }

    private PackBox mapBox(ResultSet rs) throws SQLException {
        PackBox box = new PackBox();
        box.setBoxId(rs.getLong("box_id"));
        box.setPickTicketId(rs.getLong("pick_ticket_id"));
        box.setPickingId(rs.getLong("picking_id"));
        box.setSealedFlag(rs.getBoolean("sealed_flag"));
        box.setSealMethod(rs.getString("seal_method"));
        java.sql.Timestamp sealedTs = rs.getTimestamp("sealed_at");
        if (sealedTs != null) {
            box.setSealedAt(sealedTs.toLocalDateTime());
        }
        box.setSourceRef(rs.getString("source_ref"));
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            box.setCreatedAt(createdTs.toLocalDateTime());
        }
        box.setCreatedBy(rs.getString("created_by"));
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            box.setUpdatedAt(updatedTs.toLocalDateTime());
        }
        box.setUpdatedBy(rs.getString("updated_by"));
        return box;
    }

    private PackBoxLine mapLine(ResultSet rs) throws SQLException {
        PackBoxLine line = new PackBoxLine();
        line.setBoxLineId(rs.getLong("box_line_id"));
        line.setBoxId(rs.getLong("box_id"));
        line.setPickingLineId(rs.getLong("picking_line_id"));
        line.setPackedQty(rs.getBigDecimal("packed_qty"));
        line.setUom(rs.getString("uom"));
        line.setSourceRef(rs.getString("source_ref"));
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) {
            line.setCreatedAt(createdTs.toLocalDateTime());
        }
        line.setCreatedBy(rs.getString("created_by"));
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) {
            line.setUpdatedAt(updatedTs.toLocalDateTime());
        }
        line.setUpdatedBy(rs.getString("updated_by"));
        return line;
    }
}

