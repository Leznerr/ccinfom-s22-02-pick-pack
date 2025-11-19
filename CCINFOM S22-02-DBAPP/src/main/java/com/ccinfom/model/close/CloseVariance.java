package com.ccinfom.model.close;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents {@code close_variance}.
 */
public class CloseVariance {

    private Long varianceId;
    private Long closeId;
    private Long ticketLineId;
    private BigDecimal requestedQty;
    private BigDecimal deliveredQty;
    private BigDecimal shortQty;
    private String reason;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public CloseVariance() {
        // default
    }

    public CloseVariance(Long varianceId,
                         Long closeId,
                         Long ticketLineId,
                         BigDecimal requestedQty,
                         BigDecimal deliveredQty,
                         BigDecimal shortQty,
                         String reason,
                         String sourceRef,
                         LocalDateTime createdAt,
                         String createdBy,
                         LocalDateTime updatedAt,
                         String updatedBy) {
        this.varianceId = varianceId;
        this.closeId = closeId;
        this.ticketLineId = ticketLineId;
        this.requestedQty = requestedQty;
        this.deliveredQty = deliveredQty;
        this.shortQty = shortQty;
        this.reason = reason;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getVarianceId() {
        return varianceId;
    }

    public void setVarianceId(Long varianceId) {
        this.varianceId = varianceId;
    }

    public Long getCloseId() {
        return closeId;
    }

    public void setCloseId(Long closeId) {
        this.closeId = closeId;
    }

    public Long getTicketLineId() {
        return ticketLineId;
    }

    public void setTicketLineId(Long ticketLineId) {
        this.ticketLineId = ticketLineId;
    }

    public BigDecimal getRequestedQty() {
        return requestedQty;
    }

    public void setRequestedQty(BigDecimal requestedQty) {
        this.requestedQty = requestedQty;
    }

    public BigDecimal getDeliveredQty() {
        return deliveredQty;
    }

    public void setDeliveredQty(BigDecimal deliveredQty) {
        this.deliveredQty = deliveredQty;
    }

    public BigDecimal getShortQty() {
        return shortQty;
    }

    public void setShortQty(BigDecimal shortQty) {
        this.shortQty = shortQty;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

