package com.ccinfom.daoTest;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.VehicleDaoImpl;
import com.ccinfom.model.Vehicle;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.VehicleService;
import com.ccinfom.service.impl.VehicleServiceImpl;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

/**
 * Smoke test for Vehicle CRUD with status toggle and dispatch guard.
 */
public final class VehicleCrudRunner {

    private VehicleCrudRunner() {}

    public static void main(String[] args) throws Exception {
        VehicleService service = new VehicleServiceImpl(new VehicleDaoImpl());
        String plate = "QA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);

        Vehicle created = null;
        try {
            created = createVehicle(service, plate);
            created = updateVehicle(service, created);
            toggleVehicle(service, created.getVehicleId(), false);
            toggleVehicle(service, created.getVehicleId(), true);
            System.out.println("Vehicle CRUD smoke test PASSED.");
        } catch (SQLException | ValidationException ex) {
            System.err.println("Vehicle CRUD test FAILED: " + ex.getMessage());
            throw ex;
        } finally {
            if (created != null) {
                cleanup(created.getVehicleId());
            }
        }
    }

    private static Vehicle createVehicle(VehicleService service, String plate)
            throws SQLException, ValidationException {
        Vehicle v = new Vehicle();
        v.setPlateNumber(plate);
        v.setVehicleType("van");
        v.setCapacity(new BigDecimal("500"));
        v.setSlaHours(24);
        Vehicle created = service.createVehicle(v, "qa");
        System.out.println("Created vehicle ID=" + created.getVehicleId());
        return created;
    }

    private static Vehicle updateVehicle(VehicleService service, Vehicle vehicle)
            throws SQLException, ValidationException {
        vehicle.setCapacity(new BigDecimal("750"));
        vehicle.setSlaHours(36);
        vehicle.setVehicleType("truck");
        Vehicle updated = service.updateVehicle(vehicle, "qa");
        System.out.println("Updated vehicle type=" + updated.getVehicleType() + ", SLA=" + updated.getSlaHours());
        return updated;
    }

    private static void toggleVehicle(VehicleService service, long vehicleId, boolean active)
            throws SQLException, ValidationException {
        service.setVehicleActive(vehicleId, active, "qa");
        System.out.println("Set vehicle " + vehicleId + " active=" + active);
    }

    private static void cleanup(long vehicleId) {
        String sql = "DELETE FROM vehicles WHERE vehicle_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, vehicleId);
            ps.executeUpdate();
            System.out.println("Cleaned up temp vehicle row.");
        } catch (SQLException ex) {
            System.err.println("Failed to cleanup temp vehicle: " + ex.getMessage());
        }
    }
}
