package com.ccinfom.model.dispatch;

import java.time.LocalDateTime;

/**
 * Represents {@code dispatch_line}.
 */
public class DispatchLine {

    private Long dispatchLineId;
    private Long dispatchId;
    private Long boxId;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public DispatchLine() {
        // default
    }

    public DispatchLine(Long dispatchLineId,
                        Long dispatchId,
                        Long boxId,
                        String sourceRef,
                        LocalDateTime createdAt,
                        String createdBy,
                        LocalDateTime updatedAt,
                        String updatedBy) {
        this.dispatchLineId = dispatchLineId;
        this.dispatchId = dispatchId;
        this.boxId = boxId;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getDispatchLineId() {
        return dispatchLineId;
    }

    public void setDispatchLineId(Long dispatchLineId) {
        this.dispatchLineId = dispatchLineId;
    }

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public void setBoxId(Long boxId) {
        this.boxId = boxId;
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

