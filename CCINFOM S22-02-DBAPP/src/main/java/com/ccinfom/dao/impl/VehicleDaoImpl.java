package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.VehicleDao;
import com.ccinfom.model.Vehicle;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleDaoImpl implements VehicleDao {

    private static final String BASE_SELECT = """
            SELECT vehicle_id, plate_number, vehicle_type, capacity, sla_hours,
                   vehicle_status, created_at, updated_at, updated_by
              FROM vehicles
            """;

    @Override
    public List<Vehicle> listAll(boolean includeInactive) throws SQLException {
        String sql = BASE_SELECT + (includeInactive ? "" : " WHERE vehicle_status <> 'inactive'")
                + " ORDER BY plate_number";
        List<Vehicle> vehicles = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                vehicles.add(mapRow(rs));
            }
        }
        return vehicles;
    }

    @Override
    public Optional<Vehicle> findById(long vehicleId) throws SQLException {
        String sql = BASE_SELECT + " WHERE vehicle_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isPlateExists(String plateNumber, Long excludeVehicleId) throws SQLException {
        if (plateNumber == null || plateNumber.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM vehicles WHERE plate_number = ?" +
                (excludeVehicleId != null ? " AND vehicle_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plateNumber);
            if (excludeVehicleId != null) {
                ps.setLong(2, excludeVehicleId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public long insert(Vehicle vehicle) throws SQLException {
        String sql = """
                INSERT INTO vehicles
                    (plate_number, vehicle_type, capacity, sla_hours, vehicle_status, updated_by)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(vehicle, ps);
            ps.setString(5, vehicle.getVehicleStatus());
            ps.setString(6, vehicle.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert vehicle record.");
    }

    @Override
    public void update(Vehicle vehicle) throws SQLException {
        String sql = """
                UPDATE vehicles
                   SET plate_number=?, vehicle_type=?, capacity=?, sla_hours=?,
                       updated_by=?, updated_at=CURRENT_TIMESTAMP
                 WHERE vehicle_id=?
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, vehicle.getPlateNumber());
            ps.setString(2, vehicle.getVehicleType());
            ps.setBigDecimal(3, vehicle.getCapacity());
            ps.setInt(4, vehicle.getSlaHours());
            ps.setString(5, vehicle.getUpdatedBy());
            ps.setLong(6, vehicle.getVehicleId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setStatus(long vehicleId, String status, String actor) throws SQLException {
        String sql = "UPDATE vehicles SET vehicle_status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE vehicle_id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, actor);
            ps.setLong(3, vehicleId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean hasActiveDispatch(long vehicleId) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                  FROM dispatch_hdr
                 WHERE vehicle_id = ?
                   AND arrive_ts IS NULL
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    private void bindFields(Vehicle vehicle, PreparedStatement ps) throws SQLException {
        ps.setString(1, vehicle.getPlateNumber());
        ps.setString(2, vehicle.getVehicleType());
        ps.setBigDecimal(3, vehicle.getCapacity());
        ps.setInt(4, vehicle.getSlaHours());
    }

    private Vehicle mapRow(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getLong("vehicle_id"));
        v.setPlateNumber(rs.getString("plate_number"));
        v.setVehicleType(rs.getString("vehicle_type"));
        BigDecimal cap = rs.getBigDecimal("capacity");
        v.setCapacity(cap);
        v.setSlaHours(rs.getInt("sla_hours"));
        v.setVehicleStatus(rs.getString("vehicle_status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            v.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            v.setUpdatedAt(updated.toLocalDateTime());
        }
        v.setUpdatedBy(rs.getString("updated_by"));
        return v;
    }
}
