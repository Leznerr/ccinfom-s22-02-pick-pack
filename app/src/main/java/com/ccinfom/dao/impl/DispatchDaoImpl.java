package com.ccinfom.dao.impl;

import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.model.LookupValue;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
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

public class DispatchDaoImpl implements DispatchDao {

    private static final String INSERT_HEADER_SQL = """
        INSERT INTO dispatch_hdr
            (pick_ticket_id, vehicle_id, driver_id, manifest_no, depart_ts, arrive_ts,
             pod_ref, pod_ts, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final String INSERT_LINE_SQL = """
        INSERT INTO dispatch_line
            (dispatch_id, box_id, source_ref, created_by, updated_by)
        VALUES (?, ?, ?, ?, ?)
        """;

    @Override
    public long insertDispatchHeader(DispatchHeader header, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_HEADER_SQL, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, header.getPickTicketId());
            stmt.setLong(2, header.getVehicleId());
            stmt.setLong(3, header.getDriverId());
            stmt.setString(4, header.getManifestNo());
            if (header.getDepartTs() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(header.getDepartTs()));
            } else {
                stmt.setNull(5, java.sql.Types.TIMESTAMP);
            }
            if (header.getArriveTs() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(header.getArriveTs()));
            } else {
                stmt.setNull(6, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(7, header.getPodRef());
            if (header.getPodTs() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(header.getPodTs()));
            } else {
                stmt.setNull(8, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(9, header.getSourceRef());
            stmt.setString(10, header.getCreatedBy());
            stmt.setString(11, header.getUpdatedBy());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert dispatch header.");
    }

    @Override
    public void insertDispatchLines(long dispatchId, List<DispatchLine> lines, Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(INSERT_LINE_SQL)) {
            for (DispatchLine line : lines) {
                stmt.setLong(1, dispatchId);
                stmt.setLong(2, line.getBoxId());
                stmt.setString(3, line.getSourceRef());
                stmt.setString(4, line.getCreatedBy());
                stmt.setString(5, line.getUpdatedBy());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public boolean isBoxLoaded(long boxId, Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM dispatch_line WHERE box_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, boxId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean isManifestNoExists(String manifestNo, Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM dispatch_hdr WHERE manifest_no = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, manifestNo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public Optional<DispatchHeader> findById(long dispatchId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM dispatch_hdr WHERE dispatch_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, dispatchId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapHeader(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public int countBoxesAssignedToVehicle(long vehicleId, Connection conn) throws SQLException {
        String sql = """
            SELECT COUNT(*)
              FROM dispatch_line dl
              JOIN dispatch_hdr dh ON dh.dispatch_id = dl.dispatch_id
             WHERE dh.vehicle_id = ?
               AND dh.arrive_ts IS NULL
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    @Override
    public boolean isVehicleAvailable(long vehicleId, Connection conn) throws SQLException {
        String sql = "SELECT vehicle_status FROM vehicles WHERE vehicle_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("vehicle_status");
                    return status != null && status.equalsIgnoreCase("available");
                }
            }
        }
        return false;
    }

    @Override
    public int fetchVehicleCapacity(long vehicleId, Connection conn) throws SQLException {
        String sql = "SELECT capacity FROM vehicles WHERE vehicle_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, vehicleId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("capacity");
                }
            }
        }
        throw new SQLException("Vehicle not found for capacity lookup: " + vehicleId);
    }

    @Override
    public void updateDispatchTimings(long dispatchId,
                                      LocalDateTime departTs,
                                      LocalDateTime arriveTs,
                                      LocalDateTime podTs,
                                      String podRef,
                                      String updatedBy,
                                      Connection conn) throws SQLException {
        String sql = """
            UPDATE dispatch_hdr
               SET depart_ts = ?,
                   arrive_ts = ?,
                   pod_ts = ?,
                   pod_ref = ?,
                   updated_by = ?,
                   updated_at = CURRENT_TIMESTAMP
             WHERE dispatch_id = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (departTs != null) {
                stmt.setTimestamp(1, Timestamp.valueOf(departTs));
            } else {
                stmt.setNull(1, java.sql.Types.TIMESTAMP);
            }
            if (arriveTs != null) {
                stmt.setTimestamp(2, Timestamp.valueOf(arriveTs));
            } else {
                stmt.setNull(2, java.sql.Types.TIMESTAMP);
            }
            if (podTs != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(podTs));
            } else {
                stmt.setNull(3, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(4, podRef);
            stmt.setString(5, updatedBy);
            stmt.setLong(6, dispatchId);
            stmt.executeUpdate();
        }
    }

    private DispatchHeader mapHeader(ResultSet rs) throws SQLException {
        DispatchHeader header = new DispatchHeader();
        header.setDispatchId(rs.getLong("dispatch_id"));
        header.setPickTicketId(rs.getLong("pick_ticket_id"));
        header.setVehicleId(rs.getLong("vehicle_id"));
        header.setDriverId(rs.getLong("driver_id"));
        header.setManifestNo(rs.getString("manifest_no"));
        Timestamp depart = rs.getTimestamp("depart_ts");
        if (depart != null) {
            header.setDepartTs(depart.toLocalDateTime());
        }
        Timestamp arrive = rs.getTimestamp("arrive_ts");
        if (arrive != null) {
            header.setArriveTs(arrive.toLocalDateTime());
        }
        header.setPodRef(rs.getString("pod_ref"));
        Timestamp podTs = rs.getTimestamp("pod_ts");
        if (podTs != null) {
            header.setPodTs(podTs.toLocalDateTime());
        }
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

    private DispatchLine mapLine(ResultSet rs) throws SQLException {
        DispatchLine line = new DispatchLine();
        line.setDispatchLineId(rs.getLong("dispatch_line_id"));
        line.setDispatchId(rs.getLong("dispatch_id"));
        line.setBoxId(rs.getLong("box_id"));
        line.setSourceRef(rs.getString("source_ref"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            line.setCreatedAt(created.toLocalDateTime());
        }
        line.setCreatedBy(rs.getString("created_by"));
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            line.setUpdatedAt(updated.toLocalDateTime());
        }
        line.setUpdatedBy(rs.getString("updated_by"));
        return line;
    }

    // Convenience method should tests need line details
    public List<DispatchLine> listLinesByDispatchId(long dispatchId, Connection conn) throws SQLException {
        String sql = "SELECT * FROM dispatch_line WHERE dispatch_id = ?";
        List<DispatchLine> lines = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, dispatchId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lines.add(mapLine(rs));
                }
            }
        }
        return lines;
    }


    // NEW METHODS REQUIRED BY DispatchServiceImpl ===

    // --- Update arrival timestamp only ---
    @Override
    public void updateDispatchArrival(long dispatchId, LocalDateTime arriveTs, String updatedBy, Connection conn) throws SQLException {
        String sql = """
            UPDATE dispatch_hdr
            SET arrive_ts = ?,
                updated_by = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE dispatch_id = ?
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (arriveTs != null) {
                stmt.setTimestamp(1, Timestamp.valueOf(arriveTs));
            } else {
                stmt.setNull(1, java.sql.Types.TIMESTAMP);
            }
            stmt.setString(2, updatedBy);
            stmt.setLong(3, dispatchId);
            stmt.executeUpdate();
        }
    }

    // --- Fetch available vehicle names for dropdown ---
    @Override
    public List<LookupValue> findAvailableVehicles(Connection conn) throws SQLException {
        String sql = """
            SELECT vehicle_id, plate_number
              FROM vehicles
             WHERE vehicle_status = 'available'
             ORDER BY plate_number
            """;
        List<LookupValue> vehicles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                vehicles.add(new LookupValue(rs.getLong("vehicle_id"), rs.getString("plate_number")));
            }
        }
        return vehicles;
    }

    // --- Fetch available driver names for dropdown ---
    @Override
    public List<LookupValue> findAvailableDrivers(Connection conn) throws SQLException {
        List<LookupValue> drivers = new ArrayList<>();
        String sql = """
            SELECT employee_id, CONCAT(first_name, ' ', last_name) AS full_name
              FROM employees
             WHERE employee_role = 'driver'
               AND employee_status = 'active'
             ORDER BY full_name
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                drivers.add(new LookupValue(rs.getLong("employee_id"), rs.getString("full_name")));
            }
        }
        return drivers;
    }


    // --- Fetch all boxes for a given ticket ---
    @Override
    public List<DispatchLine> findBoxesForTicket(long ticketId, Connection conn) throws SQLException {
        List<DispatchLine> boxes = new ArrayList<>();
        String sql = """
            SELECT box_id, source_ref, created_at, created_by
              FROM pack_box_hdr
             WHERE pick_ticket_id = ?
               AND sealed_flag = 1
               AND NOT EXISTS (
                    SELECT 1 FROM dispatch_line dl WHERE dl.box_id = pack_box_hdr.box_id
               )
             ORDER BY box_id
            """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, ticketId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    DispatchLine line = new DispatchLine();
                    line.setBoxId(rs.getLong("box_id"));
                    line.setSourceRef(rs.getString("source_ref"));
                    Timestamp createdTs = rs.getTimestamp("created_at");
                    if (createdTs != null) {
                        line.setCreatedAt(createdTs.toLocalDateTime());
                    }
                    line.setCreatedBy(rs.getString("created_by"));
                    boxes.add(line);
                }
            }
        }
        return boxes;
    }

} // end

