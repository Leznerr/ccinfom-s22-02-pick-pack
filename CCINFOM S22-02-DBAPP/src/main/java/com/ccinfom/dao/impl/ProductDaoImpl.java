package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.ProductDao;
import com.ccinfom.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDaoImpl implements ProductDao {

    private static final String BASE_SELECT = """
            SELECT product_id, sku, product_name, category,
                   unit_price, unit_of_measure, on_hand_qty, reserved_qty,
                   active_flag, created_at, updated_at, updated_by
              FROM products
            """;

    @Override
    public List<Product> listAll(boolean includeInactive) throws SQLException {
        String sql = BASE_SELECT + (includeInactive ? "" : " WHERE active_flag = TRUE") + " ORDER BY product_name";
        List<Product> products = new ArrayList<>();
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                products.add(mapRow(rs));
            }
        }
        return products;
    }

    @Override
    public Optional<Product> findById(long productId) throws SQLException {
        String sql = BASE_SELECT + " WHERE product_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isSkuExists(String sku, Long excludeProductId) throws SQLException {
        String sql = "SELECT COUNT(1) FROM products WHERE sku = ?" +
                (excludeProductId != null ? " AND product_id <> ?" : "");
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sku);
            if (excludeProductId != null) {
                ps.setLong(2, excludeProductId);
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
    public long insert(Product product) throws SQLException {
        String sql = """
                INSERT INTO products (sku, product_name, category, unit_price,
                                      unit_of_measure, on_hand_qty, reserved_qty,
                                      active_flag, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindMutableFields(product, ps);
            ps.setBoolean(8, product.isActiveFlag());
            ps.setString(9, product.getUpdatedBy());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert product record.");
    }

    @Override
    public void update(Product product) throws SQLException {
        String sql = """
                UPDATE products
                   SET sku=?, product_name=?, category=?, unit_price=?, unit_of_measure=?,
                       on_hand_qty=?, reserved_qty=?, updated_by=?, updated_at=CURRENT_TIMESTAMP
                 WHERE product_id=?
                """;
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindMutableFields(product, ps);
            ps.setString(8, product.getUpdatedBy());
            ps.setLong(9, product.getProductId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setActive(long productId, boolean active, String actor) throws SQLException {
        String sql = "UPDATE products SET active_flag=?, updated_by=?, updated_at=CURRENT_TIMESTAMP WHERE product_id=?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setString(2, actor);
            ps.setLong(3, productId);
            ps.executeUpdate();
        }
    }

    private void bindMutableFields(Product product, PreparedStatement ps) throws SQLException {
        ps.setString(1, product.getSku());
        ps.setString(2, product.getProductName());
        ps.setString(3, product.getCategory());
        ps.setBigDecimal(4, defaultZero(product.getUnitPrice()));
        ps.setString(5, product.getUnitOfMeasure());
        ps.setBigDecimal(6, defaultZero(product.getOnHandQty()));
        ps.setBigDecimal(7, defaultZero(product.getReservedQty()));
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getLong("product_id"));
        product.setSku(rs.getString("sku"));
        product.setProductName(rs.getString("product_name"));
        product.setCategory(rs.getString("category"));
        product.setUnitPrice(rs.getBigDecimal("unit_price"));
        product.setUnitOfMeasure(rs.getString("unit_of_measure"));
        product.setOnHandQty(rs.getBigDecimal("on_hand_qty"));
        product.setReservedQty(rs.getBigDecimal("reserved_qty"));
        product.setActiveFlag(rs.getBoolean("active_flag"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) {
            product.setCreatedAt(created.toLocalDateTime());
        }
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) {
            product.setUpdatedAt(updated.toLocalDateTime());
        }
        product.setUpdatedBy(rs.getString("updated_by"));
        return product;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

