package com.ccinfom.service;

import com.ccinfom.model.Product;

import java.sql.SQLException;
import java.util.List;

public interface ProductService {

    List<Product> listProducts(String filter, boolean includeInactive) throws SQLException;

    Product createProduct(Product product, String actor) throws SQLException, ValidationException;

    Product updateProduct(Product product, String actor) throws SQLException, ValidationException;

    void setProductActive(long productId, boolean active, String actor) throws SQLException, ValidationException;
}

