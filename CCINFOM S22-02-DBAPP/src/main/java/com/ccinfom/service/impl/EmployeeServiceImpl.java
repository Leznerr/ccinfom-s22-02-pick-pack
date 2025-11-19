package com.ccinfom.service.impl;

import com.ccinfom.dao.interfaces.EmployeeDao;
import com.ccinfom.model.Employee;
import com.ccinfom.service.EmployeeService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.util.CoreValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EmployeeServiceImpl implements EmployeeService {

    private static final String SYSTEM_USER = "system";

    private final EmployeeDao employeeDao;

    public EmployeeServiceImpl(EmployeeDao employeeDao) {
        this.employeeDao = employeeDao;
    }

    @Override
    public List<Employee> listEmployees(String filter, boolean includeInactive) throws SQLException {
        String keyword = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        return employeeDao.listAll(includeInactive).stream()
                .filter(emp -> keyword.isEmpty() || matches(emp, keyword))
                .collect(Collectors.toList());
    }

    @Override
    public Employee createEmployee(Employee employee, String actor)
            throws SQLException, ValidationException {
        Employee sanitized = sanitize(employee);
        validatedEmployee(sanitized, null);
        sanitized.setEmployeeStatus(Employee.Status.ACTIVE);
        sanitized.setUpdatedBy(resolveActor(actor));
        long id = employeeDao.insert(sanitized);
        return employeeDao.findById(id).orElseThrow(() ->
                new SQLException("Could not load employee after insert."));
    }

    @Override
    public Employee updateEmployee(Employee employee, String actor)
            throws SQLException, ValidationException {
        if (employee.getEmployeeId() == null) {
            throw new ValidationException("EMPLOYEE_ID_REQUIRED", "Employee ID is required for updates.");
        }
        Employee sanitized = sanitize(employee);
        validatedEmployee(sanitized, sanitized.getEmployeeId());
        sanitized.setUpdatedBy(resolveActor(actor));
        employeeDao.update(sanitized);
        return employeeDao.findById(sanitized.getEmployeeId()).orElseThrow(() ->
                new SQLException("Could not load employee after update."));
    }

    @Override
    public void setEmployeeActive(long employeeId, boolean active, String actor)
            throws SQLException, ValidationException {
        Employee employee = employeeDao.findById(employeeId)
                .orElseThrow(() -> new ValidationException("EMPLOYEE_NOT_FOUND", "Employee not found."));
        Employee.Status newStatus = active ? Employee.Status.ACTIVE : Employee.Status.INACTIVE;
        if (newStatus == employee.getEmployeeStatus()) {
            return;
        }
        if (!active) {
            if (employee.getEmployeeRole() == Employee.Role.PICKER
                    && employeeDao.hasOpenPickingAssignments(employeeId)) {
                throw new ValidationException("EMPLOYEE_HAS_PICKING",
                        "Cannot deactivate picker while picking sessions are still open.");
            }
            if (employee.getEmployeeRole() == Employee.Role.DRIVER
                    && employeeDao.hasOpenDispatchAssignments(employeeId)) {
                throw new ValidationException("EMPLOYEE_HAS_DISPATCH",
                        "Cannot deactivate driver while dispatch manifests are still open.");
            }
        }
        employeeDao.setStatus(employeeId, newStatus, resolveActor(actor));
    }

    private boolean matches(Employee employee, String keyword) {
        return contains(employee.getFirstName(), keyword)
                || contains(employee.getLastName(), keyword)
                || contains(employee.getEmail(), keyword)
                || contains(employee.getEmployeeRole().name(), keyword);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Employee sanitize(Employee employee) {
        Employee copy = new Employee();
        copy.setEmployeeId(employee.getEmployeeId());
        copy.setFirstName(trim(employee.getFirstName()));
        copy.setLastName(trim(employee.getLastName()));
        copy.setEmployeeRole(employee.getEmployeeRole());
        copy.setPhone(trim(employee.getPhone()));
        copy.setEmail(trim(employee.getEmail()));
        copy.setEmployeeStatus(employee.getEmployeeStatus());
        return copy;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validatedEmployee(Employee employee, Long excludeId)
            throws ValidationException, SQLException {
        CoreValidationUtil.requireNonBlank(employee.getFirstName(),
                "EMPLOYEE_FIRST_NAME_REQUIRED", "First name is required.");
        CoreValidationUtil.requireNonBlank(employee.getLastName(),
                "EMPLOYEE_LAST_NAME_REQUIRED", "Last name is required.");
        if (employee.getEmployeeRole() == null) {
            throw new ValidationException("EMPLOYEE_ROLE_REQUIRED", "Employee role is required.");
        }
        CoreValidationUtil.validatePhoneFormat(employee.getPhone(),
                "EMPLOYEE_PHONE_INVALID", "Phone number");
        CoreValidationUtil.validateEmailFormat(employee.getEmail(),
                "EMPLOYEE_EMAIL_INVALID", "Email");

        if (employeeDao.isPhoneExists(employee.getPhone(), excludeId)) {
            throw new ValidationException("EMPLOYEE_PHONE_DUPLICATE",
                    "Phone number already exists: " + employee.getPhone());
        }
        if (employeeDao.isEmailExists(employee.getEmail(), excludeId)) {
            throw new ValidationException("EMPLOYEE_EMAIL_DUPLICATE",
                    "Email already exists: " + employee.getEmail());
        }
        if (employee.getEmployeeStatus() == null) {
            employee.setEmployeeStatus(Employee.Status.ACTIVE);
        }
    }

    private String resolveActor(String actor) {
        return (actor == null || actor.isBlank()) ? SYSTEM_USER : actor;
    }
}

