package com.ccinfom.service;

import com.ccinfom.model.Vehicle;
import java.sql.SQLException;
import java.util.List;

public interface VehicleService {

    List<Vehicle> listVehicles(String filter, boolean includeInactive) throws SQLException;

    Vehicle createVehicle(Vehicle vehicle, String actor) throws SQLException, ValidationException;

    Vehicle updateVehicle(Vehicle vehicle, String actor) throws SQLException, ValidationException;

    void setVehicleActive(long vehicleId, boolean active, String actor) throws SQLException, ValidationException;
}
