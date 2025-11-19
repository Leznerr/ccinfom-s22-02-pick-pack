package com.ccinfom.model.close;

import java.time.LocalDateTime;

/**
 * Represents {@code close_hdr}.
 */
public class CloseHeader {

    public enum FinalStatus {
        Delivered("Delivered"),
        ShortClosed("Short-Closed");

        private final String dbValue;

        FinalStatus(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static FinalStatus fromDb(String value) {
            if (value == null) {
                return null;
            }
            for (FinalStatus status : values()) {
                if (status.dbValue.equalsIgnoreCase(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown close final status: " + value);
        }
    }

    private Long closeId;
    private Long pickTicketId;
    private Long dispatchId;
    private FinalStatus finalStatus;
    private String podRef;
    private LocalDateTime podTs;
    private String notes;
    private String sourceRef;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public CloseHeader() {
        // default
    }

    public CloseHeader(Long closeId,
                       Long pickTicketId,
                       Long dispatchId,
                       FinalStatus finalStatus,
                       String podRef,
                       LocalDateTime podTs,
                       String notes,
                       String sourceRef,
                       LocalDateTime createdAt,
                       String createdBy,
                       LocalDateTime updatedAt,
                       String updatedBy) {
        this.closeId = closeId;
        this.pickTicketId = pickTicketId;
        this.dispatchId = dispatchId;
        this.finalStatus = finalStatus;
        this.podRef = podRef;
        this.podTs = podTs;
        this.notes = notes;
        this.sourceRef = sourceRef;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public Long getCloseId() {
        return closeId;
    }

    public void setCloseId(Long closeId) {
        this.closeId = closeId;
    }

    public Long getPickTicketId() {
        return pickTicketId;
    }

    public void setPickTicketId(Long pickTicketId) {
        this.pickTicketId = pickTicketId;
    }

    public Long getDispatchId() {
        return dispatchId;
    }

    public void setDispatchId(Long dispatchId) {
        this.dispatchId = dispatchId;
    }

    public FinalStatus getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(FinalStatus finalStatus) {
        this.finalStatus = finalStatus;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

