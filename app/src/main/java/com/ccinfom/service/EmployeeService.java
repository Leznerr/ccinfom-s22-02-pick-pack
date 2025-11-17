package com.ccinfom.service;

import com.ccinfom.model.Employee;

import java.sql.SQLException;
import java.util.List;

public interface EmployeeService {

    List<Employee> listEmployees(String filter, boolean includeInactive) throws SQLException;

    Employee createEmployee(Employee employee, String actor) throws SQLException, ValidationException;

    Employee updateEmployee(Employee employee, String actor) throws SQLException, ValidationException;

    void setEmployeeActive(long employeeId, boolean active, String actor) throws SQLException, ValidationException;
}

