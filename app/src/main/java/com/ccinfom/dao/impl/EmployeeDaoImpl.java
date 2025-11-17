package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.EmployeeDao;
import com.ccinfom.model.Employee;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmployeeDaoImpl implements EmployeeDao {

    private static final String BASE_SELECT = """
            SELECT employee_id, last_name, first_name, employee_role,
                   phone, email, employee_status,
                   created_at, updated_at, updated_by
              FROM employees
            """;

    @Override
    public List<Employee> listAll(boolean includeInactive) throws SQLException {
        String sql = BASE_SELECT + (includeInactive ? "" : " WHERE employee_status = 'active'")
                + " ORDER BY last_name, first_name";
        List<Employee> employees = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                employees.add(mapRow(rs));
            }
        }
        return employees;
    }

    @Override
    public Optional<Employee> findById(long employeeId) throws SQLException {
        String sql = BASE_SELECT + " WHERE employee_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isEmailExists(String email, Long excludeEmployeeId) throws SQLException {
        if (email == null || email.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM employees WHERE email = ?" +
                (excludeEmployeeId != null ? " AND employee_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            if (excludeEmployeeId != null) {
                ps.setLong(2, excludeEmployeeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPhoneExists(String phone, Long excludeEmployeeId) throws SQLException {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM employees WHERE phone = ?" +
                (excludeEmployeeId != null ? " AND employee_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            if (excludeEmployeeId != null) {
                ps.setLong(2, excludeEmployeeId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public long insert(Employee employee) throws SQLException {
        String sql = """
                INSERT INTO employees
                    (last_name, first_name, employee_role,
                     phone, email, employee_status, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(employee, ps);
            ps.setString(6, employee.getEmployeeStatus().name().toLowerCase());
            ps.setString(7, employee.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert employee record.");
    }

    @Override
    public void update(Employee employee) throws SQLException {
        String sql = """
                UPDATE employees
                   SET last_name=?, first_name=?, employee_role=?, phone=?, email=?,
                       updated_by=?, updated_at=CURRENT_TIMESTAMP
                 WHERE employee_id=?
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, employee.getLastName());
            ps.setString(2, employee.getFirstName());
            ps.setString(3, employee.getEmployeeRole().name().toLowerCase());
            ps.setString(4, employee.getPhone());
            ps.setString(5, employee.getEmail());
            ps.setString(6, employee.getUpdatedBy());
            ps.setLong(7, employee.getEmployeeId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setStatus(long employeeId, Employee.Status status, String actor) throws SQLException {
        String sql = "UPDATE employees SET employee_status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE employee_id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name().toLowerCase());
            ps.setString(2, actor);
            ps.setLong(3, employeeId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean hasOpenPickingAssignments(long employeeId) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                  FROM picking_hdr
                 WHERE picker_employee_id = ?
                   AND picking_status NOT IN ('Done','Cancelled')
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasOpenDispatchAssignments(long employeeId) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                  FROM dispatch_hdr
                 WHERE driver_id = ?
                   AND arrive_ts IS NULL
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    private void bindFields(Employee employee, PreparedStatement ps) throws SQLException {
        ps.setString(1, employee.getLastName());
        ps.setString(2, employee.getFirstName());
        ps.setString(3, employee.getEmployeeRole().name().toLowerCase());
        ps.setString(4, employee.getPhone());
        ps.setString(5, employee.getEmail());
    }

    private Employee mapRow(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setEmployeeId(rs.getLong("employee_id"));
        employee.setLastName(rs.getString("last_name"));
        employee.setFirstName(rs.getString("first_name"));
        employee.setEmployeeRole(Employee.Role.valueOf(rs.getString("employee_role").toUpperCase()));
        employee.setPhone(rs.getString("phone"));
        employee.setEmail(rs.getString("email"));
        employee.setEmployeeStatus(Employee.Status.valueOf(rs.getString("employee_status").toUpperCase()));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            employee.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            employee.setUpdatedAt(updated.toLocalDateTime());
        }
        employee.setUpdatedBy(rs.getString("updated_by"));
        return employee;
    }
}

