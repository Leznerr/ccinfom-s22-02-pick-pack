package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.BranchDao;
import com.ccinfom.model.Branch;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BranchDaoImpl implements BranchDao {

    private static final String BASE_SELECT = """
            SELECT branch_id, branch_name, address, city,
                   contact_person, phone, branch_status,
                   created_at, updated_at, updated_by
              FROM branches
            """;

    @Override
    public List<Branch> listAll(boolean includeInactive) throws SQLException {
        String sql = BASE_SELECT + (includeInactive ? "" : " WHERE branch_status = 'active'")
                + " ORDER BY branch_name";
        List<Branch> branches = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                branches.add(mapRow(rs));
            }
        }
        return branches;
    }

    @Override
    public Optional<Branch> findById(long branchId) throws SQLException {
        String sql = BASE_SELECT + " WHERE branch_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isPhoneExists(String phone, Long excludeBranchId) throws SQLException {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(1) FROM branches WHERE phone = ?" +
                (excludeBranchId != null ? " AND branch_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            if (excludeBranchId != null) {
                ps.setLong(2, excludeBranchId);
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
    public long insert(Branch branch) throws SQLException {
        String sql = """
                INSERT INTO branches
                    (branch_name, address, city, contact_person, phone, branch_status, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindFields(branch, ps);
            ps.setString(6, branch.getBranchStatus());
            ps.setString(7, branch.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert branch record.");
    }

    @Override
    public void update(Branch branch) throws SQLException {
        String sql = """
                UPDATE branches
                   SET branch_name=?, address=?, city=?, contact_person=?, phone=?,
                       updated_by=?, updated_at=CURRENT_TIMESTAMP
                 WHERE branch_id=?
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, branch.getBranchName());
            ps.setString(2, branch.getAddress());
            ps.setString(3, branch.getCity());
            ps.setString(4, branch.getContactPerson());
            ps.setString(5, branch.getPhone());
            ps.setString(6, branch.getUpdatedBy());
            ps.setLong(7, branch.getBranchId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setStatus(long branchId, String status, String actor) throws SQLException {
        String sql = "UPDATE branches SET branch_status=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE branch_id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, actor);
            ps.setLong(3, branchId);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean hasActiveTickets(long branchId) throws SQLException {
        String sql = """
                SELECT COUNT(1)
                  FROM pick_ticket_hdr
                 WHERE branch_id = ?
                   AND ticket_status NOT IN ('Closed','Delivered')
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, branchId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
        }
        return false;
    }

    private void bindFields(Branch branch, PreparedStatement ps) throws SQLException {
        ps.setString(1, branch.getBranchName());
        ps.setString(2, branch.getAddress());
        ps.setString(3, branch.getCity());
        ps.setString(4, branch.getContactPerson());
        ps.setString(5, branch.getPhone());
    }

    private Branch mapRow(ResultSet rs) throws SQLException {
        Branch b = new Branch();
        b.setBranchId(rs.getLong("branch_id"));
        b.setBranchName(rs.getString("branch_name"));
        b.setAddress(rs.getString("address"));
        b.setCity(rs.getString("city"));
        b.setContactPerson(rs.getString("contact_person"));
        b.setPhone(rs.getString("phone"));
        b.setBranchStatus(rs.getString("branch_status"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            b.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            b.setUpdatedAt(updated.toLocalDateTime());
        }
        b.setUpdatedBy(rs.getString("updated_by"));
        return b;
    }
}

