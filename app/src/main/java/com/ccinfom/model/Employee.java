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

public class Employee {
    public enum Role {
        PICKER, PACKER, DISPATCHER
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    private Long employeeId;
    private String lastName;
    private String firstName;
    private Role employeeRole;
    private String phone;
    private String email;
    private Status employeeStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // Constructors
    public Employee() {}
    public Employee(Long employeeId, String lastName, String firstName, Role employeeRole,
                    String phone, String email, Status employeeStatus,
                    LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.employeeId = employeeId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.employeeRole = employeeRole;
        this.phone = phone;
        this.email = email;
        this.employeeStatus = employeeStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    // Getters and Setters
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public Role getEmployeeRole() { return employeeRole; }
    public void setEmployeeRole(Role employeeRole) { this.employeeRole = employeeRole; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Status getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(Status employeeStatus) { this.employeeStatus = employeeStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
