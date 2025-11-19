package com.ccinfom.model.dispatch;

import java.time.LocalDateTime;

/**
 * Represents {@code dispatch_hdr}.
 */
public class DispatchHeader {

    public enum DispatchStatus {
        Built,
        Departed,
        Arrived,
        Delivered,
        Partial
    }

    private Long dispatchId;
    private Long pickTicketId;
    private Long vehicleId;
    private Long driverId;
    private String manifestNo;
    private DispatchStatus dispatchStatus;
    private LocalDateTime departTs;
    private LocalDateTime arriveTs;
    private String podRef;
    private LocalDateTime podTs;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public DispatchHeader() {
        // default
    }

    public DispatchHeader(Long dispatchId,
                          Long pickTicketId,
                          Long vehicleId,
                          Long driverId,
                          String manifestNo,
                          DispatchStatus dispatchStatus,
                          LocalDateTime departTs,
                          LocalDateTime arriveTs,
                          String podRef,
                          LocalDateTime podTs,
                          String sourceRef,
                          LocalDateTime createdAt,
                          String createdBy,
                          LocalDateTime updatedAt,
                          String updatedBy) {
        this.dispatchId = dispatchId;
        this.pickTicketId = pickTicketId;
        this.vehicleId = vehicleId;
        this.driverId = driverId;
        this.manifestNo = manifestNo;
        this.dispatchStatus = dispatchStatus;
        this.departTs = departTs;
        this.arriveTs = arriveTs;
        this.podRef = podRef;
        this.podTs = podTs;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public Long getPickTicketId() {
        return pickTicketId;
    }

    public void setPickTicketId(Long pickTicketId) {
        this.pickTicketId = pickTicketId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getManifestNo() {
        return manifestNo;
    }

    public void setManifestNo(String manifestNo) {
        this.manifestNo = manifestNo;
    }

    public DispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(DispatchStatus dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public LocalDateTime getDepartTs() {
        return departTs;
    }

    public void setDepartTs(LocalDateTime departTs) {
        this.departTs = departTs;
    }

    public LocalDateTime getArriveTs() {
        return arriveTs;
    }

    public void setArriveTs(LocalDateTime arriveTs) {
        this.arriveTs = arriveTs;
    }

    public String getPodRef() {
        return podRef;
    }

    public void setPodRef(String podRef) {
        this.podRef = podRef;
    }

    public LocalDateTime getPodTs() {
        return podTs;
    }

    public void setPodTs(LocalDateTime podTs) {
        this.podTs = podTs;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public void setSourceRef(String sourceRef) {
        this.sourceRef = sourceRef;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}

