package com.ccinfom.service.impl;

import com.ccinfom.dao.interfaces.ProductDao;
import com.ccinfom.model.Product;
import com.ccinfom.service.ProductService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.util.CoreValidationUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProductServiceImpl implements ProductService {

    private static final String SYSTEM_USER = "system";

    private final ProductDao productDao;

    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public List<Product> listProducts(String filter, boolean includeInactive) throws SQLException {
        String keyword = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        return productDao.listAll(includeInactive).stream()
                .filter(p -> keyword.isEmpty() || matchesFilter(p, keyword))
                .collect(Collectors.toList());
    }

    @Override
    public Product createProduct(Product product, String actor)
            throws SQLException, ValidationException {
        Product sanitized = sanitize(product);
        validateProduct(sanitized, null);
        sanitized.setActiveFlag(true);
        sanitized.setUpdatedBy(resolveActor(actor));
        long productId = productDao.insert(sanitized);
        return productDao.findById(productId).orElseThrow(() ->
                new SQLException("Unable to load product after insert."));
    }

    @Override
    public Product updateProduct(Product product, String actor)
            throws SQLException, ValidationException {
        if (product.getProductId() == null) {
            throw new ValidationException("PRODUCT_ID_REQUIRED", "Product ID is required for updates.");
        }
        Product sanitized = sanitize(product);
        validateProduct(sanitized, sanitized.getProductId());
        sanitized.setUpdatedBy(resolveActor(actor));
        productDao.update(sanitized);
        return productDao.findById(sanitized.getProductId()).orElseThrow(() ->
                new SQLException("Unable to load product after update."));
    }

    @Override
    public void setProductActive(long productId, boolean active, String actor)
            throws SQLException, ValidationException {
        Product target = productDao.findById(productId)
                .orElseThrow(() -> new ValidationException("PRODUCT_NOT_FOUND", "Product not found."));
        if (!active && target.getReservedQty() != null && target.getReservedQty().signum() > 0) {
            throw new ValidationException("PRODUCT_HAS_RESERVATION",
                    "Cannot deactivate a product with reserved quantity.");
        }
        productDao.setActive(productId, active, resolveActor(actor));
    }

    private boolean matchesFilter(Product product, String keyword) {
        return contains(product.getSku(), keyword)
                || contains(product.getProductName(), keyword)
                || contains(product.getCategory(), keyword);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Product sanitize(Product product) {
        Product copy = new Product();
        copy.setProductId(product.getProductId());
        copy.setSku(trim(product.getSku()));
        copy.setProductName(trim(product.getProductName()));
        copy.setCategory(trim(product.getCategory()));
        copy.setUnitPrice(defaultZero(product.getUnitPrice()));
        copy.setUnitOfMeasure(trim(product.getUnitOfMeasure()));
        copy.setOnHandQty(defaultZero(product.getOnHandQty()));
        copy.setReservedQty(defaultZero(product.getReservedQty()));
        copy.setActiveFlag(product.isActiveFlag());
        return copy;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void validateProduct(Product product, Long excludeId)
            throws ValidationException, SQLException {
        CoreValidationUtil.requireNonBlank(product.getSku(), "PRODUCT_SKU_REQUIRED", "SKU is required.");
        CoreValidationUtil.requireNonBlank(product.getProductName(), "PRODUCT_NAME_REQUIRED", "Product name is required.");
        CoreValidationUtil.requireNonBlank(product.getCategory(), "PRODUCT_CATEGORY_REQUIRED", "Category is required.");
        CoreValidationUtil.requireNonBlank(product.getUnitOfMeasure(), "PRODUCT_UOM_REQUIRED", "Unit of measure is required.");
        CoreValidationUtil.requireNonNegative(product.getUnitPrice(), "PRODUCT_PRICE_INVALID", "Unit price");
        CoreValidationUtil.requireNonNegative(product.getOnHandQty(), "PRODUCT_ON_HAND_INVALID", "On hand quantity");
        CoreValidationUtil.requireNonNegative(product.getReservedQty(), "PRODUCT_RESERVED_INVALID", "Reserved quantity");

        if (product.getReservedQty().compareTo(product.getOnHandQty()) > 0) {
            throw new ValidationException("PRODUCT_RESERVED_GT_ON_HAND",
                    "Reserved quantity cannot exceed on-hand quantity.");
        }

        boolean skuExists = productDao.isSkuExists(product.getSku(), excludeId);
        CoreValidationUtil.ensureNotDuplicate(skuExists, "PRODUCT_SKU_DUPLICATE",
                "SKU already exists: " + product.getSku());
    }

    private String resolveActor(String actor) {
        return (actor == null || actor.isBlank()) ? SYSTEM_USER : actor;
    }
}

