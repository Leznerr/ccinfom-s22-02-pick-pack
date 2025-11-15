package com.ccinfom.report.r4;

public class ReportFilters {
    private Integer year;
    private Integer month;       // 1–12 for MONTH mode
    private Integer isoWeek;     // 1–53 for ISO_WEEK mode
    private String vehicleName;  // optional vehicle filter
    private String driverName;   // optional driver filter
    private Long customerId;
    private Long productId;

    public ReportFilters() {}

    // === Getters and Setters ===

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getIsoWeek() { return isoWeek; }
    public void setIsoWeek(Integer isoWeek) { this.isoWeek = isoWeek; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
