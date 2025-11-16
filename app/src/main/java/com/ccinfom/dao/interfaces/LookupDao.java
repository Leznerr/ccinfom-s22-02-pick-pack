/*
 * CCINFOM â€" Phase D
 * File: LookupDao.java
 * Purpose: Read-only lookups for UI dropdowns (customers, branches, employees, products).
 *
 * Required methods (examples):
 *  - List<Customer> listCustomers()
 *  - List<Branch> listBranches()
 *  - List<Employee> listActivePickers()
 *  - List<Product> listActiveProducts()
 *
 * TODOs:
 *  [ ] Queries must be ordered predictably (e.g., by name).
 */

package com.ccinfom.dao.interfaces;

import com.ccinfom.model.*;
import java.sql.SQLException;
import java.util.List;

public interface LookupDao {
    List<Customer> listCustomers();
    List<Customer> listActiveCustomers();
    List<Employee> listActiveEmployees() throws SQLException;
    List<Branch> listBranches();
    List<Branch> listActiveBranches();
    List<Employee> listActivePickers();
    List<String> listProductCategories() throws SQLException;
    List<Product> listActiveProducts();
    List<Vehicle> listActiveVehicles();
    Product findProductById(Long productId) throws SQLException;
}

