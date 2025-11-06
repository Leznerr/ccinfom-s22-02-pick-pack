package com.ccinfom.model.pack;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * POJO that mirrors {@code pack_box_line}.
 */
public class PackBoxLine {

    private Long boxLineId;
    private Long boxId;
    private Long pickingLineId;
    private BigDecimal packedQty;
    private String uom;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public PackBoxLine() {
        // default
    }

    public PackBoxLine(Long boxLineId,
                       Long boxId,
                       Long pickingLineId,
                       BigDecimal packedQty,
                       String uom,
                       String sourceRef,
                       LocalDateTime createdAt,
                       String createdBy,
                       LocalDateTime updatedAt,
                       String updatedBy) {
        this.boxLineId = boxLineId;
        this.boxId = boxId;
        this.pickingLineId = pickingLineId;
        this.packedQty = packedQty;
        this.uom = uom;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getBoxLineId() {
        return boxLineId;
    }

    public void setBoxLineId(Long boxLineId) {
        this.boxLineId = boxLineId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
    }

    public Long getPickingLineId() {
        return pickingLineId;
    }

    public void setPickingLineId(Long pickingLineId) {
        this.pickingLineId = pickingLineId;
    }

    public BigDecimal getPackedQty() {
        return packedQty;
    }

    public void setPackedQty(BigDecimal packedQty) {
        this.packedQty = packedQty;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
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

