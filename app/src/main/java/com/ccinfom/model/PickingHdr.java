/*
 * CCINFOM — Phase D
 * File: <ModelName>.java
 * Purpose: Simple data holder that mirrors table <table_name>.
 *
 * Fields (keep in sync with schema):
 *  - List properties by name and type (e.g., Long pickTicketId, String ticketStatus, ...).
 *
 * TODOs:
 *  [ ] Define fields with correct Java types (use BigDecimal for DECIMAL).
 *  [ ] Add getters/setters, toString(), equals()/hashCode() if needed for UI lists.
 *
 * Definition of Done:
 *  - Can be populated from a ResultSet row without type loss.
 *  - No DB logic here (pure POJO).
 */

package com.ccinfom.model;

import java.time.LocalDateTime;

 public class PickingHdr {

    public enum PickingStatus {
        Picking, Done, Cancelled;
    }
    
    private Long pickingId;
    private Long pickTicketId;
    private Long pickerEmployeeId;
    private String pickingStatus;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Constructors
    public PickingHdr() {}
    public PickingHdr(Long pickingId, Long pickTicketId, Long pickerEmployeeId, String pickingStatus,
                      LocalDateTime startedAt, LocalDateTime completedAt,
                      LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.pickingId = pickingId;
        this.pickTicketId = pickTicketId;
        this.pickerEmployeeId = pickerEmployeeId;
        this.pickingStatus = pickingStatus;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public Long getPickingId() { return pickingId; }
    public void setPickingId(Long pickingId) { this.pickingId = pickingId; }

    public Long getPickTicketId() { return pickTicketId; }
    public void setPickTicketId(Long pickTicketId) { this.pickTicketId = pickTicketId; }

    public Long getPickerEmployeeId() { return pickerEmployeeId;}
    public void setPickerEmployeeId(Long pickerEmployeeId) { this.pickerEmployeeId = pickerEmployeeId; }

    public String getPickingStatus() { return pickingStatus; }
    public void setPickingStatus(String pickingStatus) { this.pickingStatus = pickingStatus; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
 }