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

import java.math.BigDecimal;
import java.time.LocalDateTime;

 public class PickTicketLine {

    public enum LineStatus {
        Valid, Invalid, Duplicate, Cancelled
    }

    private Long ticketLineId;
    private Long pickTicketId;
    private Long productId;
    private BigDecimal requestedQty;
    private String uom;
    private LineStatus lineStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Constructors
    public PickTicketLine() {}
    public PickTicketLine(Long ticketLineId, Long pickTicketId, Long productId,
                          BigDecimal requestedQty, String uom, LineStatus lineStatus,
                          LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.ticketLineId = ticketLineId;
        this.pickTicketId = pickTicketId;
        this.productId = productId;
        this.requestedQty = requestedQty;
        this.uom = uom;
        this.lineStatus = lineStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }
    
    // Getters and Setters
    public Long getTicketLineId() { return ticketLineId; }
    public void setTicketLineId(Long ticketLineId) { this.ticketLineId = ticketLineId; }

    public Long getPickTicketId() { return pickTicketId; }
    public void setPickTicketId(Long pickTicketId) { this.pickTicketId = pickTicketId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getRequestedQty() { return requestedQty; }
    public void setRequestedQty(BigDecimal requestedQty) { this.requestedQty = requestedQty; }

    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }

    public LineStatus getLineStatus() { return lineStatus; }
    public void setLineStatus(LineStatus lineStatus) { this.lineStatus = lineStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

 } // end