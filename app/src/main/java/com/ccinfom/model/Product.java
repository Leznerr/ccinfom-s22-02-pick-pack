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

public class Product {
    private Long productId;
    private String sku;
    private String productName;
    private String category;
    private BigDecimal unitPrice;
    private String unitOfMeasure;
    private BigDecimal onHandQty;
    private BigDecimal reservedQty;
    private boolean activeFlag;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Constructors
    public Product() {}
    public Product(Long productId, String sku, String productName, String category,
                   BigDecimal unitPrice, String unitOfMeasure,
                   BigDecimal onHandQty, BigDecimal reservedQty,
                   boolean activeFlag, LocalDateTime createdAt,
                   LocalDateTime updatedAt, String updatedBy) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.category = category;
        this.unitPrice = unitPrice;
        this.unitOfMeasure = unitOfMeasure;
        this.onHandQty = onHandQty;
        this.reservedQty = reservedQty;
        this.activeFlag = activeFlag;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }

    public BigDecimal getOnHandQty() { return onHandQty; }
    public void setOnHandQty(BigDecimal onHandQty) { this.onHandQty = onHandQty; }

    public BigDecimal getReservedQty() { return reservedQty; }
    public void setReservedQty(BigDecimal reservedQty) { this.reservedQty = reservedQty; }

    public boolean isActiveFlag() { return activeFlag; }
    public void setActiveFlag(boolean activeFlag) { this.activeFlag = activeFlag; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
