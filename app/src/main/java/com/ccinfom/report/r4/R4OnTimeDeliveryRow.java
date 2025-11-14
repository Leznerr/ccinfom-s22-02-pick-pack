package com.ccinfom.report.r4;

import java.math.BigDecimal;

public class R4OnTimeDeliveryRow {    
    // Time Dimensions
    private int deliveryYear;
    private int deliveryMonth;
    private String yearMonthLabel;

    // Customer Dimensions
    private long customerId;
    private String customerName;

    // Product Dimensions
    private long productId;
    private String sku;
    private String productName;
    private String category;

    // Core Metrics
    private int onTimeDeliveries;
    private int lateDeliveries;
    private int shortClosedShipments;
    private int shortageReasonsCount;
    private BigDecimal onTimePercentage;

    // Getters & Setters
    public int getDeliveryYear() { return deliveryYear; }
    public void setDeliveryYear(int deliveryYear) { this.deliveryYear = deliveryYear; }

    public int getDeliveryMonth() { return deliveryMonth; }
    public void setDeliveryMonth(int deliveryMonth) { this.deliveryMonth = deliveryMonth; }

    public String getYearMonthLabel() { return yearMonthLabel; }
    public void setYearMonthLabel(String yearMonthLabel) { this.yearMonthLabel = yearMonthLabel; }

    public long getCustomerId() { return customerId; }
    public void setCustomerId(long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public long getProductId() { return productId;}
    public void setProductId(long productId) { this.productId = productId; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // Core Metrics Getters and Setters
    public int getOnTimeDeliveries() { return onTimeDeliveries; }
    public void setOnTimeDeliveries(int onTimeDeliveries) { this.onTimeDeliveries = onTimeDeliveries; }

    public int getLateDeliveries() { return lateDeliveries; }
    public void setLateDeliveries(int lateDeliveries) { this.lateDeliveries = lateDeliveries; }

    public int getShortClosedShipments() { return shortClosedShipments; }
    public void setShortClosedShipments(int shortClosedShipments) { this.shortClosedShipments = shortClosedShipments; }

    public int getShortageReasonsCount() { return shortageReasonsCount; }
    public void setShortageReasonsCount(int shortageReasonsCount) { this.shortageReasonsCount = shortageReasonsCount; }

    public BigDecimal getOnTimePercentage() { return onTimePercentage; }
    public void setOnTimePercentage(BigDecimal onTimePercentage) { this.onTimePercentage = onTimePercentage; }
    
} // end
