// THIS FILE IS ONLY USED FOR MANUAL TESTING PURPOSES

//package com.ccinfom.dao.impl;
package com.ccinfom.daoTest;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.model.*;

import java.util.List;

public class TestLookupDao {
    public static void main(String[] args) {
        LookupDao lookupDao = new LookupDaoImpl();

        System.out.println("=== Testing listCustomers() ===");
        List<Customer> customers = lookupDao.listCustomers();
        for (Customer c : customers) {
            System.out.println(c.getCustomerName() + " - " + c.getContactPerson());
        }

        System.out.println("\n=== Testing listBranches() ===");
        List<Branch> branches = lookupDao.listBranches();
        for (Branch b : branches) {
            System.out.println(b.getBranchName() + " - " + b.getCity());
        }

        System.out.println("\n=== Testing listActivePickers() ===");
        List<Employee> employees = lookupDao.listActivePickers();
        for (Employee e : employees) {
            System.out.println(e.getFirstName() + " " + e.getLastName() + " (" + e.getEmployeeRole() + ")");
        }

        System.out.println("\n=== Testing listActiveProducts() ===");
        List<Product> products = lookupDao.listActiveProducts();
        for (Product p : products) {
            System.out.println(p.getProductName() + " - Qty: " + p.getOnHandQty());
        }

        System.out.println("\n=== Testing listActiveVehicles() ===");
        List<Vehicle> vehicles = lookupDao.listActiveVehicles();
        for (Vehicle v : vehicles) {
            System.out.println(v.getPlateNumber() + " - " + v.getVehicleType() + " (" + v.getCapacity() + ")");
        }

        System.out.println("\n DAO test complete!");
    }
}
