package com.ccinfom.report.r3;

import java.math.BigDecimal;

/**
 * DTO for R3 – Monthly Pack-to-Dispatch performance by branch.
 */
public class R3MonthlyThroughputRow {
    /* Time Dimensions */
    private int year;
    private int month;
    private String yearMonth;

    /* Branch */
    private Long branchId;
    private String branchName;

    /* Metrics */
    private int shipmentsPacked;
    private BigDecimal avgMinutesToDispatch;
    private BigDecimal medianMinutesToDispatch;
    private BigDecimal slaWithinPct;
    private int exceptionsCount;

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public int getShipmentsPacked() {
        return shipmentsPacked;
    }

    public void setShipmentsPacked(int shipmentsPacked) {
        this.shipmentsPacked = shipmentsPacked;
    }

    public BigDecimal getAvgMinutesToDispatch() {
        return avgMinutesToDispatch;
    }

    public void setAvgMinutesToDispatch(BigDecimal avgMinutesToDispatch) {
        this.avgMinutesToDispatch = avgMinutesToDispatch;
    }

    public BigDecimal getMedianMinutesToDispatch() {
        return medianMinutesToDispatch;
    }

    public void setMedianMinutesToDispatch(BigDecimal medianMinutesToDispatch) {
        this.medianMinutesToDispatch = medianMinutesToDispatch;
    }

    public BigDecimal getSlaWithinPct() {
        return slaWithinPct;
    }

    public void setSlaWithinPct(BigDecimal slaWithinPct) {
        this.slaWithinPct = slaWithinPct;
    }

    public int getExceptionsCount() {
        return exceptionsCount;
    }

    public void setExceptionsCount(int exceptionsCount) {
        this.exceptionsCount = exceptionsCount;
    }
}
