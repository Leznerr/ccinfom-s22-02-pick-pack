package com.ccinfom.daoTest;

import com.ccinfom.dao.impl.ProductDaoImpl;
import com.ccinfom.model.Product;
import com.ccinfom.service.ProductService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.ProductServiceImpl;
import com.ccinfom.config.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Console runner to exercise Product CRUD operations.
 * Inserts a temporary product, updates it, toggles status, and then deletes it.
 */
public final class ProductCrudRunner {

    private ProductCrudRunner() {}

    public static void main(String[] args) throws Exception {
        ProductService productService = new ProductServiceImpl(new ProductDaoImpl());
        String sku = "TEST-" + UUID.randomUUID().toString().substring(0, 8);

        Product created = null;
        try {
            created = createSampleProduct(productService, sku);
            created = updateSampleProduct(productService, created);
            toggleProduct(productService, created.getProductId(), false);
            toggleProduct(productService, created.getProductId(), true);
            System.out.println("Product CRUD smoke test PASSED.");
        } catch (SQLException | ValidationException ex) {
            System.err.println("Product CRUD test FAILED: " + ex.getMessage());
            throw ex;
        } finally {
            if (created != null) {
                cleanup(created.getProductId());
            }
        }
    }

    private static Product createSampleProduct(ProductService service, String sku)
            throws SQLException, ValidationException {
        Product product = new Product();
        product.setSku(sku);
        product.setProductName("Test Product " + sku);
        product.setCategory("QA");
        product.setUnitPrice(new BigDecimal("123.45"));
        product.setUnitOfMeasure("pcs");
        product.setOnHandQty(new BigDecimal("10"));
        product.setReservedQty(BigDecimal.ZERO);
        Product created = service.createProduct(product, "qa");
        System.out.println("Created product ID=" + created.getProductId());
        return created;
    }

    private static Product updateSampleProduct(ProductService service, Product product)
            throws SQLException, ValidationException {
        product.setProductName(product.getProductName() + " [updated]");
        product.setOnHandQty(new BigDecimal("50"));
        Product updated = service.updateProduct(product, "qa");
        System.out.println("Updated product name=" + updated.getProductName());
        return updated;
    }

    private static void toggleProduct(ProductService service, long productId, boolean active)
            throws SQLException, ValidationException {
        service.setProductActive(productId, active, "qa");
        System.out.println("Set product " + productId + " active=" + active);
    }

    private static void cleanup(long productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, productId);
            int rows = ps.executeUpdate();
            System.out.println("Cleaned up " + rows + " temp product row(s).");
        } catch (SQLException ex) {
            System.err.println("Failed to cleanup temp product: " + ex.getMessage());
        }
    }
}

