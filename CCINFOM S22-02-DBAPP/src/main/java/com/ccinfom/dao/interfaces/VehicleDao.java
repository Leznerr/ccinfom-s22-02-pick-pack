package com.ccinfom.dao.interfaces;

import com.ccinfom.model.Vehicle;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface VehicleDao {

    List<Vehicle> listAll(boolean includeInactive) throws SQLException;

    Optional<Vehicle> findById(long vehicleId) throws SQLException;

    boolean isPlateExists(String plateNumber, Long excludeVehicleId) throws SQLException;

    long insert(Vehicle vehicle) throws SQLException;

    void update(Vehicle vehicle) throws SQLException;

    void setStatus(long vehicleId, String status, String actor) throws SQLException;

    boolean hasActiveDispatch(long vehicleId) throws SQLException;
}
