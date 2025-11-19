package com.ccinfom.service.impl;

import com.ccinfom.dao.interfaces.CustomerDao;
import com.ccinfom.model.Customer;
import com.ccinfom.service.CustomerService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.util.CoreValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CustomerServiceImpl implements CustomerService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String SYSTEM_USER = "system";

    private final CustomerDao customerDao;

    public CustomerServiceImpl(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    @Override
    public List<Customer> listCustomers(String filter, boolean includeInactive) throws SQLException {
        String keyword = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        return customerDao.listAll(includeInactive).stream()
                .filter(c -> keyword.isEmpty() || matches(c, keyword))
                .collect(Collectors.toList());
    }

    @Override
    public Customer createCustomer(Customer customer, String actor)
            throws SQLException, ValidationException {
        Customer sanitized = sanitize(customer);
        validateCustomer(sanitized, null);
        sanitized.setCustomerStatus(STATUS_ACTIVE);
        sanitized.setUpdatedBy(resolveActor(actor));
        long id = customerDao.insert(sanitized);
        return customerDao.findById(id).orElseThrow(() ->
                new SQLException("Could not load customer after insert."));
    }

    @Override
    public Customer updateCustomer(Customer customer, String actor)
            throws SQLException, ValidationException {
        if (customer.getCustomerId() == null) {
            throw new ValidationException("CUSTOMER_ID_REQUIRED", "Customer ID is required for updates.");
        }
        Customer sanitized = sanitize(customer);
        validateCustomer(sanitized, sanitized.getCustomerId());
        sanitized.setUpdatedBy(resolveActor(actor));
        customerDao.update(sanitized);
        return customerDao.findById(sanitized.getCustomerId()).orElseThrow(() ->
                new SQLException("Could not load customer after update."));
    }

    @Override
    public void setCustomerActive(long customerId, boolean active, String actor)
            throws SQLException, ValidationException {
        Customer customer = customerDao.findById(customerId)
                .orElseThrow(() -> new ValidationException("CUSTOMER_NOT_FOUND", "Customer not found."));
        String newStatus = active ? STATUS_ACTIVE : STATUS_INACTIVE;
        if (newStatus.equals(customer.getCustomerStatus())) {
            return;
        }
        customerDao.setStatus(customerId, newStatus, resolveActor(actor));
    }

    private boolean matches(Customer customer, String keyword) {
        return contains(customer.getCustomerName(), keyword)
                || contains(customer.getContactPerson(), keyword)
                || contains(customer.getEmail(), keyword);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Customer sanitize(Customer customer) {
        Customer copy = new Customer();
        copy.setCustomerId(customer.getCustomerId());
        copy.setCustomerName(trim(customer.getCustomerName()));
        copy.setContactPerson(trim(customer.getContactPerson()));
        copy.setPhone(trim(customer.getPhone()));
        copy.setEmail(trim(customer.getEmail()));
        copy.setDefaultDeliveryAddress(trim(customer.getDefaultDeliveryAddress()));
        copy.setCustomerStatus(customer.getCustomerStatus());
        return copy;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateCustomer(Customer customer, Long excludeId)
            throws ValidationException, SQLException {
        CoreValidationUtil.requireNonBlank(customer.getCustomerName(),
                "CUSTOMER_NAME_REQUIRED", "Customer name is required.");
        CoreValidationUtil.requireNonBlank(customer.getDefaultDeliveryAddress(),
                "CUSTOMER_ADDRESS_REQUIRED", "Delivery address is required.");
        CoreValidationUtil.validatePhoneFormat(customer.getPhone(),
                "CUSTOMER_PHONE_INVALID", "Phone number");
        CoreValidationUtil.validateEmailFormat(customer.getEmail(),
                "CUSTOMER_EMAIL_INVALID", "Email");

        if (customerDao.isPhoneExists(customer.getPhone(), excludeId)) {
            throw new ValidationException("CUSTOMER_PHONE_DUPLICATE",
                    "Phone number already exists: " + customer.getPhone());
        }
        if (customerDao.isEmailExists(customer.getEmail(), excludeId)) {
            throw new ValidationException("CUSTOMER_EMAIL_DUPLICATE",
                    "Email already exists: " + customer.getEmail());
        }
    }

    private String resolveActor(String actor) {
        return (actor == null || actor.isBlank()) ? SYSTEM_USER : actor;
    }
}

