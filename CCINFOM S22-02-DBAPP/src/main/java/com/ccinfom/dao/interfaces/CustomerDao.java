package com.ccinfom.dao.interfaces;

import com.ccinfom.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CustomerDao {

    List<Customer> listAll(boolean includeInactive) throws SQLException;

    Optional<Customer> findById(long customerId) throws SQLException;

    boolean isEmailExists(String email, Long excludeCustomerId) throws SQLException;

    boolean isPhoneExists(String phone, Long excludeCustomerId) throws SQLException;

    long insert(Customer customer) throws SQLException;

    void update(Customer customer) throws SQLException;

    void setStatus(long customerId, String status, String actor) throws SQLException;
}

