package com.ccinfom.service;

import com.ccinfom.model.Customer;

import java.sql.SQLException;
import java.util.List;

public interface CustomerService {

    List<Customer> listCustomers(String filter, boolean includeInactive) throws SQLException;

    Customer createCustomer(Customer customer, String actor) throws SQLException, ValidationException;

    Customer updateCustomer(Customer customer, String actor) throws SQLException, ValidationException;

    void setCustomerActive(long customerId, boolean active, String actor) throws SQLException, ValidationException;
}

