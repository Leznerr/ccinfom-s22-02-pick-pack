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

public class PickTicketHdr {

    public enum TicketStatus {
        Open("Open"),
        Picking("Picking"),
        Packed("Packed"),
        Dispatched("Dispatched"),
        Delivered("Delivered"),
        ShortClosed("Short-Closed");

        private final String dbValue;

        TicketStatus(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        @Override
        public String toString() {
            return dbValue;
        }

        public static TicketStatus fromDb(String value) {
            if (value == null) {
                throw new IllegalArgumentException("ticket_status cannot be null");
            }
            for (TicketStatus status : values()) {
                if (status.dbValue.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown ticket_status value: " + value);
        }
    }

    private Long pickTicketId;
    private Long customerId;
    private Long branchId;
    private TicketStatus ticketStatus;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Constructors
    public PickTicketHdr() {}
    public PickTicketHdr(Long pickTicketId, Long customerId, Long branchId,
                         TicketStatus ticketStatus, String remarks,
                         LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.pickTicketId = pickTicketId;
        this.customerId = customerId;
        this.branchId = branchId;
        this.ticketStatus = ticketStatus;
        this.remarks = remarks;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public Long getPickTicketId() { return pickTicketId; }
    public void setPickTicketId(Long pickTicketId) { this.pickTicketId = pickTicketId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public TicketStatus getTicketStatus() { return ticketStatus; }
    public void setTicketStatus(TicketStatus ticketStatus) { this.ticketStatus = ticketStatus; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
 }
