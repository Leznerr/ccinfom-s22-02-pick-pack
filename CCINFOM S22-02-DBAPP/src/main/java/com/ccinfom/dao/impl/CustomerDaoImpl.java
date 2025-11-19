package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.CustomerDao;
import com.ccinfom.model.Customer;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CustomerDaoImpl implements CustomerDao {

    private static final String BASE_SELECT = """
            SELECT customer_id, customer_name, contact_person, phone, email,
                   default_delivery_address, customer_status,
                   created_at, updated_at, updated_by
              FROM customers
            """;

    @Override
    public List<Customer> listAll(boolean includeInactive) throws SQLException {
        String sql = BASE_SELECT + (includeInactive ? "" : " WHERE customer_status = 'active'")
                + " ORDER BY customer_name";
        List<Customer> customers = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add(mapRow(rs));
            }
        }
        return customers;
    }

    @Override
    public Optional<Customer> findById(long customerId) throws SQLException {
        String sql = BASE_SELECT + " WHERE customer_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isEmailExists(String email, Long excludeCustomerId) throws SQLException {
        if (email == null || email.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM customers WHERE email = ?" +
                (excludeCustomerId != null ? " AND customer_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            if (excludeCustomerId != null) {
                ps.setLong(2, excludeCustomerId);
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
    public boolean isPhoneExists(String phone, Long excludeCustomerId) throws SQLException {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM customers WHERE phone = ?" +
                (excludeCustomerId != null ? " AND customer_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            if (excludeCustomerId != null) {
                ps.setLong(2, excludeCustomerId);
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
    public long insert(Customer customer) throws SQLException {
        String sql = """
                INSERT INTO customers
                    (customer_name, contact_person, phone, email,
                     default_delivery_address, customer_status, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(customer, ps);
            ps.setString(6, customer.getCustomerStatus());
            ps.setString(7, customer.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert customer record.");
    }

    @Override
    public void update(Customer customer) throws SQLException {
        String sql = """
                UPDATE customers
                   SET customer_name=?, contact_person=?, phone=?, email=?,
                       default_delivery_address=?, updated_by=?, updated_at=CURRENT_TIMESTAMP
                 WHERE customer_id=?
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, customer.getCustomerName());
            ps.setString(2, customer.getContactPerson());
            ps.setString(3, customer.getPhone());
            ps.setString(4, customer.getEmail());
            ps.setString(5, customer.getDefaultDeliveryAddress());
            ps.setString(6, customer.getUpdatedBy());
            ps.setLong(7, customer.getCustomerId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setStatus(long customerId, String status, String actor) throws SQLException {
        String sql = "UPDATE customers SET customer_status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE customer_id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, actor);
            ps.setLong(3, customerId);
            ps.executeUpdate();
        }
    }

    private void bindFields(Customer customer, PreparedStatement ps) throws SQLException {
        ps.setString(1, customer.getCustomerName());
        ps.setString(2, customer.getContactPerson());
        ps.setString(3, customer.getPhone());
        ps.setString(4, customer.getEmail());
        ps.setString(5, customer.getDefaultDeliveryAddress());
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getLong("customer_id"));
        c.setCustomerName(rs.getString("customer_name"));
        c.setContactPerson(rs.getString("contact_person"));
        c.setPhone(rs.getString("phone"));
        c.setEmail(rs.getString("email"));
        c.setDefaultDeliveryAddress(rs.getString("default_delivery_address"));
        c.setCustomerStatus(rs.getString("customer_status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            c.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            c.setUpdatedAt(updated.toLocalDateTime());
        }
        c.setUpdatedBy(rs.getString("updated_by"));
        return c;
    }
}

