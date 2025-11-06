package com.ccinfom.model.pack;

import java.time.LocalDateTime;

/**
 * POJO that mirrors {@code pack_box_hdr}.
 */
public class PackBox {

    private Long boxId;
    private Long pickTicketId;
    private Long pickingId;
    private boolean sealedFlag;
    private String sealMethod;
    private LocalDateTime sealedAt;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public PackBox() {
        // default constructor
    }

    public PackBox(Long boxId,
                   Long pickTicketId,
                   Long pickingId,
                   boolean sealedFlag,
                   String sealMethod,
                   LocalDateTime sealedAt,
                   String sourceRef,
                   LocalDateTime createdAt,
                   String createdBy,
                   LocalDateTime updatedAt,
                   String updatedBy) {
        this.boxId = boxId;
        this.pickTicketId = pickTicketId;
        this.pickingId = pickingId;
        this.sealedFlag = sealedFlag;
        this.sealMethod = sealMethod;
        this.sealedAt = sealedAt;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public Long getPickTicketId() {
        return pickTicketId;
    }

    public void setPickTicketId(Long pickTicketId) {
        this.pickTicketId = pickTicketId;
    }

    public Long getPickingId() {
        return pickingId;
    }

    public void setPickingId(Long pickingId) {
        this.pickingId = pickingId;
    }

    public boolean isSealedFlag() {
        return sealedFlag;
    }

    public void setSealedFlag(boolean sealedFlag) {
        this.sealedFlag = sealedFlag;
    }

    public String getSealMethod() {
        return sealMethod;
    }

    public void setSealMethod(String sealMethod) {
        this.sealMethod = sealMethod;
    }

    public LocalDateTime getSealedAt() {
        return sealedAt;
    }

    public void setSealedAt(LocalDateTime sealedAt) {
        this.sealedAt = sealedAt;
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

