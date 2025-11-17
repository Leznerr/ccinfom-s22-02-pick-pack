package com.ccinfom.dao.interfaces;

import com.ccinfom.model.Product;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO for CRUD operations on products (core data).
 */
public interface ProductDao {

    List<Product> listAll(boolean includeInactive) throws SQLException;

    Optional<Product> findById(long productId) throws SQLException;

    boolean isSkuExists(String sku, Long excludeProductId) throws SQLException;

    long insert(Product product) throws SQLException;

    void update(Product product) throws SQLException;

    void setActive(long productId, boolean active, String actor) throws SQLException;
}

