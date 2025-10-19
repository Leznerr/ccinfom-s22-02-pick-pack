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

import java.time.LocalDateTime;

public class Customer {
    private Long customerId;
    private String customerName;
    private String contactPerson;
    private String phone;
    private String email;
    private String defaultDeliveryAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Getters and Setters
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDefaultDeliveryAddress() { return defaultDeliveryAddress; }
    public void setDefaultDeliveryAddress(String defaultDeliveryAddress) { this.defaultDeliveryAddress = defaultDeliveryAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    @Override
    public String toString() {
        return customerName + " (" + contactPerson + ")";
    }
}
