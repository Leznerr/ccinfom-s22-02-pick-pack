package com.ccinfom.daoTest;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.EmployeeDaoImpl;
import com.ccinfom.model.Employee;
import com.ccinfom.service.EmployeeService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.EmployeeServiceImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Minimal smoke test to verify Employee CRUD + status toggling with validations.
 */
public final class EmployeeCrudRunner {

    private EmployeeCrudRunner() {}

    public static void main(String[] args) throws Exception {
        EmployeeService service = new EmployeeServiceImpl(new EmployeeDaoImpl());
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        Employee created = null;
        try {
            created = createEmployee(service, suffix);
            created = updateEmployee(service, created, suffix);
            toggleEmployee(service, created.getEmployeeId(), false);
            toggleEmployee(service, created.getEmployeeId(), true);
            System.out.println("Employee CRUD smoke test PASSED.");
        } catch (SQLException | ValidationException ex) {
            System.err.println("Employee CRUD test FAILED: " + ex.getMessage());
            throw ex;
        } finally {
            if (created != null) {
                cleanup(created.getEmployeeId());
            }
        }
    }

    private static Employee createEmployee(EmployeeService service, String suffix)
            throws SQLException, ValidationException {
        Employee emp = new Employee();
        emp.setLastName("QA");
        emp.setFirstName("Employee " + suffix);
        emp.setEmployeeRole(Employee.Role.PICKER);
        emp.setPhone(generatePhone());
        emp.setEmail("qa-employee-" + suffix + "@example.com");
        Employee created = service.createEmployee(emp, "qa");
        System.out.println("Created employee ID=" + created.getEmployeeId());
        return created;
    }

    private static Employee updateEmployee(EmployeeService service, Employee employee, String suffix)
            throws SQLException, ValidationException {
        employee.setPhone(generatePhone());
        employee.setEmail("qa-employee-" + suffix + "-upd@example.com");
        employee.setEmployeeRole(Employee.Role.DRIVER);
        Employee updated = service.updateEmployee(employee, "qa");
        System.out.println("Updated employee role=" + updated.getEmployeeRole());
        return updated;
    }

    private static void toggleEmployee(EmployeeService service, long employeeId, boolean active)
            throws SQLException, ValidationException {
        service.setEmployeeActive(employeeId, active, "qa");
        System.out.println("Set employee " + employeeId + " active=" + active);
    }

    private static void cleanup(long employeeId) {
        String sql = "DELETE FROM employees WHERE employee_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, employeeId);
            ps.executeUpdate();
            System.out.println("Cleaned up temp employee row.");
        } catch (SQLException ex) {
            System.err.println("Failed to cleanup temp employee: " + ex.getMessage());
        }
    }

    private static String generatePhone() {
        StringBuilder sb = new StringBuilder("09");
        for (int i = 0; i < 9; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}
