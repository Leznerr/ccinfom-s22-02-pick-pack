package com.ccinfom.report.r4;

public class ReportFilters {
    private Integer year;
    private Integer month;
    private Long customerId;
    private Long productId;
    
    // Constructors
    public ReportFilters() {}
    
    public ReportFilters(Integer year, Integer month) {
        this.year = year;
        this.month = month;
    }
    
    // Getters and setters
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
