/*
 * CCINFOM — Phase D
 * File: LookupDaoImpl.java
 * Purpose: Provide dropdown data for UI; read-only.
 *
 * TODOs:
 *  [ ] Implement list methods using SELECT ... ORDER BY.
 *  [ ] Map ResultSet → model cleanly; no business rules here.
 */

package com.ccinfom.dao.impl;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LookupDaoImpl implements LookupDao {

    @Override
    public List<Customer> listCustomers() {
        String sql = "SELECT customer_id, customer_name, contact_person, phone, email, default_delivery_address, " +
                     "created_at, updated_at, updated_by " +
                     "FROM customers ORDER BY customer_name";
        List<Customer> customers = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Customer c = new Customer();
                c.setCustomerId(rs.getLong("customer_id"));
                c.setCustomerName(rs.getString("customer_name"));
                c.setContactPerson(rs.getString("contact_person"));
                c.setPhone(rs.getString("phone"));
                c.setEmail(rs.getString("email"));
                c.setDefaultDeliveryAddress(rs.getString("default_delivery_address"));

                Timestamp createdTs = rs.getTimestamp("created_at");
                c.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                c.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

                c.setUpdatedBy(rs.getString("updated_by"));
                customers.add(c);
            }
        } catch (SQLException e) {
            System.err.println("LookupDaoImpl.listCustomers() failed: " + e.getMessage());
        }
        return customers;
    }

    @Override
    public List<Branch> listBranches() {
        String sql = "SELECT branch_id, branch_name, address, city, contact_person, phone, " +
                     "created_at, updated_at, updated_by " +
                     "FROM branches ORDER BY branch_name";
        List<Branch> branches = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Branch b = new Branch();
                b.setBranchId(rs.getLong("branch_id"));
                b.setBranchName(rs.getString("branch_name"));
                b.setAddress(rs.getString("address"));
                b.setCity(rs.getString("city"));
                b.setContactPerson(rs.getString("contact_person"));
                b.setPhone(rs.getString("phone"));

                Timestamp createdTs = rs.getTimestamp("created_at");
                b.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                b.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

                b.setUpdatedBy(rs.getString("updated_by"));
                branches.add(b);
            }
        } catch (SQLException e) {
            System.err.println("LookupDaoImpl.listBranches() failed: " + e.getMessage());
        }
        return branches;
    }

    @Override
    public List<Employee> listActivePickers() {
        String sql = "SELECT employee_id, last_name, first_name, employee_role, phone, email, " +
                     "created_at, updated_at, updated_by " +
                     "FROM employees WHERE employee_status = 'active' AND employee_role = 'picker' " +
                     "ORDER BY last_name, first_name";
        List<Employee> employees = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Employee e = new Employee();
                e.setEmployeeId(rs.getLong("employee_id"));
                e.setLastName(rs.getString("last_name"));
                e.setFirstName(rs.getString("first_name"));
                e.setEmployeeRole(Employee.Role.valueOf(rs.getString("employee_role").toUpperCase()));
                e.setPhone(rs.getString("phone"));
                e.setEmail(rs.getString("email"));

                Timestamp createdTs = rs.getTimestamp("created_at");
                e.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                e.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

                e.setUpdatedBy(rs.getString("updated_by"));
                employees.add(e);
            }
        } catch (SQLException e) {
            System.err.println("LookupDaoImpl.listActivePickers() failed: " + e.getMessage());
        }
        return employees;
    }


    @Override
    public List<Product> listActiveProducts() {
        String sql = "SELECT product_id, sku, product_name, category, unit_price, unit_of_measure, " +
                     "on_hand_qty, reserved_qty, active_flag, created_at, updated_at, updated_by " +
                     "FROM products WHERE active_flag = TRUE ORDER BY product_name";
        List<Product> products = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getLong("product_id"));
                p.setSku(rs.getString("sku"));
                p.setProductName(rs.getString("product_name"));
                p.setCategory(rs.getString("category"));
                p.setUnitPrice(rs.getBigDecimal("unit_price"));
                p.setUnitOfMeasure(rs.getString("unit_of_measure"));
                p.setOnHandQty(rs.getBigDecimal("on_hand_qty"));
                p.setReservedQty(rs.getBigDecimal("reserved_qty"));
                p.setActiveFlag(rs.getBoolean("active_flag"));

                Timestamp createdTs = rs.getTimestamp("created_at");
                p.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                p.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

                p.setUpdatedBy(rs.getString("updated_by"));
                products.add(p);
            }
        } catch (SQLException e) {
            System.err.println("LookupDaoImpl.listActiveProducts() failed: " + e.getMessage());
        }
        return products;
    }

    @Override
    public List<Vehicle> listActiveVehicles() {
        String sql = "SELECT vehicle_id, plate_number, vehicle_type, capacity, vehicle_status, " +
                     "created_at, updated_at, updated_by " +
                     "FROM vehicles WHERE vehicle_status = 'available' ORDER BY plate_number";
        List<Vehicle> vehicles = new ArrayList<>();

        try (Connection conn = DbConnection.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Vehicle v = new Vehicle();
                v.setVehicleId(rs.getLong("vehicle_id"));
                v.setPlateNumber(rs.getString("plate_number"));
                v.setVehicleType(rs.getString("vehicle_type"));
                v.setCapacity(rs.getBigDecimal("capacity"));
                v.setVehicleStatus(rs.getString("vehicle_status"));
                
                Timestamp createdTs = rs.getTimestamp("created_at");
                v.setCreatedAt(createdTs != null ? createdTs.toLocalDateTime() : null);

                Timestamp updatedTs = rs.getTimestamp("updated_at");
                v.setUpdatedAt(updatedTs != null ? updatedTs.toLocalDateTime() : null);

                v.setUpdatedBy(rs.getString("updated_by"));
                vehicles.add(v);
            }
        } catch (SQLException e) {
            System.err.println("LookupDaoImpl.listActiveVehicles() failed: " + e.getMessage());
        }

        return vehicles;
    }
}
