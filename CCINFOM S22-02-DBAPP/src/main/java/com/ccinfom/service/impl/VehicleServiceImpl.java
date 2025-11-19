package com.ccinfom.service.impl;

import com.ccinfom.dao.interfaces.VehicleDao;
import com.ccinfom.model.Vehicle;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.VehicleService;
import com.ccinfom.util.CoreValidationUtil;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class VehicleServiceImpl implements VehicleService {

    private static final String STATUS_AVAILABLE = "available";
    private static final String STATUS_INACTIVE = "inactive";
    private static final Set<String> ALLOWED_STATUSES = new HashSet<>(Arrays.asList(
            "available", "maintenance", "inactive"
    ));
    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList(
            "van", "truck", "motorcycle"
    ));

    private final VehicleDao vehicleDao;
    private static final String SYSTEM_USER = "system";

    public VehicleServiceImpl(VehicleDao vehicleDao) {
        this.vehicleDao = vehicleDao;
    }

    @Override
    public List<Vehicle> listVehicles(String filter, boolean includeInactive) throws SQLException {
        String keyword = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        return vehicleDao.listAll(includeInactive).stream()
                .filter(v -> keyword.isEmpty() || matches(v, keyword))
                .collect(Collectors.toList());
    }

    @Override
    public Vehicle createVehicle(Vehicle vehicle, String actor)
            throws SQLException, ValidationException {
        Vehicle sanitized = sanitize(vehicle);
        validateVehicle(sanitized, null);
        sanitized.setVehicleStatus(STATUS_AVAILABLE);
        sanitized.setUpdatedBy(resolveActor(actor));
        long id = vehicleDao.insert(sanitized);
        return vehicleDao.findById(id).orElseThrow(() ->
                new SQLException("Could not load vehicle after insert."));
    }

    @Override
    public Vehicle updateVehicle(Vehicle vehicle, String actor)
            throws SQLException, ValidationException {
        if (vehicle.getVehicleId() == null) {
            throw new ValidationException("VEHICLE_ID_REQUIRED", "Vehicle ID is required for updates.");
        }
        Vehicle sanitized = sanitize(vehicle);
        validateVehicle(sanitized, sanitized.getVehicleId());
        sanitized.setUpdatedBy(resolveActor(actor));
        vehicleDao.update(sanitized);
        return vehicleDao.findById(sanitized.getVehicleId()).orElseThrow(() ->
                new SQLException("Could not load vehicle after update."));
    }

    @Override
    public void setVehicleActive(long vehicleId, boolean active, String actor)
            throws SQLException, ValidationException {
        Vehicle vehicle = vehicleDao.findById(vehicleId)
                .orElseThrow(() -> new ValidationException("VEHICLE_NOT_FOUND", "Vehicle not found."));
        String newStatus = active ? STATUS_AVAILABLE : STATUS_INACTIVE;
        if (newStatus.equalsIgnoreCase(vehicle.getVehicleStatus())) {
            return;
        }
        if (!active && vehicleDao.hasActiveDispatch(vehicleId)) {
            throw new ValidationException("VEHICLE_HAS_ACTIVE_DISPATCH",
                    "Cannot deactivate vehicle while dispatch manifests are still active.");
        }
        vehicleDao.setStatus(vehicleId, newStatus, resolveActor(actor));
    }

    private boolean matches(Vehicle vehicle, String keyword) {
        return contains(vehicle.getPlateNumber(), keyword)
                || contains(vehicle.getVehicleType(), keyword)
                || contains(vehicle.getVehicleStatus(), keyword);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Vehicle sanitize(Vehicle vehicle) {
        Vehicle copy = new Vehicle();
        copy.setVehicleId(vehicle.getVehicleId());
        copy.setPlateNumber(trim(vehicle.getPlateNumber()));
        copy.setVehicleType(trim(vehicle.getVehicleType()));
        copy.setCapacity(vehicle.getCapacity() != null ? vehicle.getCapacity().stripTrailingZeros() : null);
        copy.setSlaHours(vehicle.getSlaHours());
        copy.setVehicleStatus(vehicle.getVehicleStatus());
        return copy;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateVehicle(Vehicle vehicle, Long excludeId)
            throws ValidationException, SQLException {
        CoreValidationUtil.requireNonBlank(vehicle.getPlateNumber(),
                "VEHICLE_PLATE_REQUIRED", "Plate number is required.");
        CoreValidationUtil.requireNonBlank(vehicle.getVehicleType(),
                "VEHICLE_TYPE_REQUIRED", "Vehicle type is required.");
        if (!ALLOWED_TYPES.contains(vehicle.getVehicleType().toLowerCase(Locale.ROOT))) {
            throw new ValidationException("VEHICLE_TYPE_INVALID", "Vehicle type must be van, truck, or motorcycle.");
        }

        BigDecimal capacity = vehicle.getCapacity();
        if (capacity == null) {
            throw new ValidationException("VEHICLE_CAPACITY_REQUIRED", "Capacity is required.");
        }
        CoreValidationUtil.requireNonNegative(capacity,
                "VEHICLE_CAPACITY_NEGATIVE", "Capacity");

        Integer sla = vehicle.getSlaHours();
        if (sla == null) {
            throw new ValidationException("VEHICLE_SLA_REQUIRED", "SLA hours are required.");
        }
        if (sla < 0) {
            throw new ValidationException("VEHICLE_SLA_NEGATIVE", "SLA hours cannot be negative.");
        }

        if (vehicleDao.isPlateExists(vehicle.getPlateNumber(), excludeId)) {
            throw new ValidationException("VEHICLE_PLATE_DUPLICATE",
                    "Plate number already exists: " + vehicle.getPlateNumber());
        }

        String status = vehicle.getVehicleStatus();
        if (status != null && !ALLOWED_STATUSES.contains(status.toLowerCase(Locale.ROOT))) {
            throw new ValidationException("VEHICLE_STATUS_INVALID", "Vehicle status is invalid.");
        }
    }

    private String resolveActor(String actor) {
        return (actor == null || actor.isBlank()) ? SYSTEM_USER : actor;
    }
}
