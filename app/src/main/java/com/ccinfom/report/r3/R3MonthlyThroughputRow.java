package main.java.com.ccinfom.report.r3;
import java.math.BigDecimal;

public class R3MonthlyThroughputRow {
    /* Time Dimensions */
    private int year;
    private int month;
    private String yearMonth;

    /* Dimensional Attributes */
    private Long customerId;
    private String customerName;
    private Long branchId;
    private String branchName;

    /* Core Metrics */
    private int totalTicketsClosed;
    private int shortClosedTickets;
    private BigDecimal shortCloseRatePct;

    private BigDecimal totalRequestedQty;
    private BigDecimal totalDeliveredQty;
    private BigDecimal totalShortQty;

    private BigDecimal estimatedReturnCost;
    private BigDecimal fulfillmentRatePct;

    /* Throughput Metrics */
    private int boxesPacked;
    private int manifestsCreated;

    /* Inventory Deltas */
    private BigDecimal inventoryDeltaReserved;
    private BigDecimal inventoryDeltaOnHand;

    /* Product Category Summary */
    private String productCategories;

    /* Constructors */
    public R3MonthlyThroughputRow() {
        // Default constructor
    }

    /* Getters and Setters */

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

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
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

    public int getTotalTicketsClosed() {
        return totalTicketsClosed;
    }

    public void setTotalTicketsClosed(int totalTicketsClosed) {
        this.totalTicketsClosed = totalTicketsClosed;
    }

    public int getShortClosedTickets() {
        return shortClosedTickets;
    }

    public void setShortClosedTickets(int shortClosedTickets) {
        this.shortClosedTickets = shortClosedTickets;
    }

    public BigDecimal getShortCloseRatePct() {
        return shortCloseRatePct;
    }

    public void setShortCloseRatePct(BigDecimal shortCloseRatePct) {
        this.shortCloseRatePct = shortCloseRatePct;
    }

    public BigDecimal getTotalRequestedQty() {
        return totalRequestedQty;
    }

    public void setTotalRequestedQty(BigDecimal totalRequestedQty) {
        this.totalRequestedQty = totalRequestedQty;
    }

    public BigDecimal getTotalDeliveredQty() {
        return totalDeliveredQty;
    }

    public void setTotalDeliveredQty(BigDecimal totalDeliveredQty) {
        this.totalDeliveredQty = totalDeliveredQty;
    }

    public BigDecimal getTotalShortQty() {
        return totalShortQty;
    }

    public void setTotalShortQty(BigDecimal totalShortQty) {
        this.totalShortQty = totalShortQty;
    }

    public BigDecimal getEstimatedReturnCost() {
        return estimatedReturnCost;
    }

    public void setEstimatedReturnCost(BigDecimal estimatedReturnCost) {
        this.estimatedReturnCost = estimatedReturnCost;
    }

    public BigDecimal getFulfillmentRatePct() {
        return fulfillmentRatePct;
    }

    public void setFulfillmentRatePct(BigDecimal fulfillmentRatePct) {
        this.fulfillmentRatePct = fulfillmentRatePct;
    }

    public int getBoxesPacked() {
        return boxesPacked;
    }

    public void setBoxesPacked(int boxesPacked) {
        this.boxesPacked = boxesPacked;
    }

    public int getManifestsCreated() {
        return manifestsCreated;
    }

    public void setManifestsCreated(int manifestsCreated) {
        this.manifestsCreated = manifestsCreated;
    }

    public BigDecimal getInventoryDeltaReserved() {
        return inventoryDeltaReserved;
    }

    public void setInventoryDeltaReserved(BigDecimal inventoryDeltaReserved) {
        this.inventoryDeltaReserved = inventoryDeltaReserved;
    }

    public BigDecimal getInventoryDeltaOnHand() {
        return inventoryDeltaOnHand;
    }

    public void setInventoryDeltaOnHand(BigDecimal inventoryDeltaOnHand) {
        this.inventoryDeltaOnHand = inventoryDeltaOnHand;
    }

    public String getProductCategories() {
        return productCategories;
    }

    public void setProductCategories(String productCategories) {
        this.productCategories = productCategories;
    }

    @Override
    public String toString() {
        return String.format(
                "R3MonthlyThroughputRow[%d-%02d, customer=%s, branch=%s, tickets=%d, shortRate=%.2f%%, cost=%.2f]",
                year, month, customerName, branchName, totalTicketsClosed,
                shortCloseRatePct, estimatedReturnCost
        );
    }
}
