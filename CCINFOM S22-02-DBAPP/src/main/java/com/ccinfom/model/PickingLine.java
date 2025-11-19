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

 public class PickingLine {
    private Long pickingLineId;
    private Long pickingId;
    private Long ticketLineId;
    private Long productId;
    private BigDecimal pickedQty;
    private String uom;
    private String shortReason;
    private String scanRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Constructors
    public PickingLine() {}
    public PickingLine(Long pickingLineId, Long pickingId, Long ticketLineId, Long productId,
                       BigDecimal pickedQty, String uom, String shortReason, String scanRef,
                       LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.pickingLineId = pickingLineId;
        this.pickingId = pickingId;
        this.ticketLineId = ticketLineId;
        this.productId = productId;
        this.pickedQty = pickedQty;
        this.uom = uom;
        this.shortReason = shortReason;
        this.scanRef = scanRef;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public Long getPickingLineId() { return pickingLineId; }
    public void setPickingLineId(Long pickingLineId) { this.pickingLineId = pickingLineId; }

    public Long getPickingId() { return pickingId; }
    public void setPickingId(Long pickingId) { this.pickingId = pickingId; }

    public Long getTicketLineId() { return ticketLineId; }
    public void setTicketLineId(Long ticketLineId) { this.ticketLineId = ticketLineId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public BigDecimal getPickedQty() { return pickedQty; }
    public void setPickedQty(BigDecimal pickedQty) { this.pickedQty = pickedQty; }

    public String getUom() { return uom; }
    public void setUom(String uom) { this.uom = uom; }

    public String getShortReason() { return shortReason; }
    public void setShortReason(String shortReason) { this.shortReason = shortReason; }

    public String getScanRef() { return scanRef; }
    public void setScanRef(String scanRef) { this.scanRef = scanRef; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
 }