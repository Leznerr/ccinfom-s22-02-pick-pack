package com.ccinfom.daoTest;

import com.ccinfom.dao.impl.CustomerDaoImpl;
import com.ccinfom.model.Customer;
import com.ccinfom.service.CustomerService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.CustomerServiceImpl;
import com.ccinfom.config.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Smoke test for the Customer CRUD service.
 */
public final class CustomerCrudRunner {

    private CustomerCrudRunner() {}

    public static void main(String[] args) throws Exception {
        CustomerService service = new CustomerServiceImpl(new CustomerDaoImpl());
        String uniqueName = "QA Customer " + UUID.randomUUID().toString().substring(0, 8);
        String uniqueEmail = uniqueName.replace(" ", "").toLowerCase() + "@example.com";

        Customer created = null;
        try {
            created = createCustomer(service, uniqueName, uniqueEmail);
            created = updateCustomer(service, created);
            toggleCustomer(service, created.getCustomerId(), false);
            toggleCustomer(service, created.getCustomerId(), true);
            System.out.println("Customer CRUD smoke test PASSED.");
        } catch (SQLException | ValidationException ex) {
            System.err.println("Customer CRUD test FAILED: " + ex.getMessage());
            throw ex;
        } finally {
            if (created != null) {
                cleanup(created.getCustomerId());
            }
        }
    }

    private static Customer createCustomer(CustomerService service, String name, String email)
            throws SQLException, ValidationException {
        Customer customer = new Customer();
        customer.setCustomerName(name);
        customer.setContactPerson("QA Contact");
        customer.setPhone("0917123" + (int) (Math.random() * 10000 + 1000));
        customer.setEmail(email);
        customer.setDefaultDeliveryAddress("123 QA Street, Metro Manila");
        Customer created = service.createCustomer(customer, "qa");
        System.out.println("Created customer ID=" + created.getCustomerId());
        return created;
    }

    private static Customer updateCustomer(CustomerService service, Customer customer)
            throws SQLException, ValidationException {
        customer.setContactPerson("Updated Contact");
        customer.setDefaultDeliveryAddress("456 Updated Ave, Metro Manila");
        Customer updated = service.updateCustomer(customer, "qa");
        System.out.println("Updated customer contact=" + updated.getContactPerson());
        return updated;
    }

    private static void toggleCustomer(CustomerService service, long customerId, boolean active)
            throws SQLException, ValidationException {
        service.setCustomerActive(customerId, active, "qa");
        System.out.println("Set customer " + customerId + " active=" + active);
    }

    private static void cleanup(long customerId) {
        String sql = "DELETE FROM customers WHERE customer_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, customerId);
            ps.executeUpdate();
            System.out.println("Cleaned up temp customer row.");
        } catch (SQLException ex) {
            System.err.println("Failed to cleanup temp customer: " + ex.getMessage());
        }
    }
}

