package com.ccinfom.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Vehicle {
    private Long vehicleId;
    private String plateNumber;
    private String vehicleType;
    private BigDecimal capacity;
    private String vehicleStatus;
    private LocalDateTime  createdAt;
    private LocalDateTime  updatedAt;
    private String updatedBy;
    
    // Getters and Setters
    public Long getVehicleId() { return vehicleId; }
    public void setVehicleId(Long vehicleId) { this.vehicleId = vehicleId; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public BigDecimal getCapacity() { return capacity; }
    public void setCapacity(BigDecimal capacity) { this.capacity = capacity; }

    public String getVehicleStatus() { return vehicleStatus; }
    public void setVehicleStatus(String vehicleStatus) { this.vehicleStatus = vehicleStatus; }

    public LocalDateTime  getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime  createdAt) { this.createdAt = createdAt; }

    public LocalDateTime  getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime  updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
            return plateNumber + " (" + vehicleType + ")";
    }
}
