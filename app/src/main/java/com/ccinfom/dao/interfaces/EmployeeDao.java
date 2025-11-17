package com.ccinfom.dao.interfaces;

import com.ccinfom.model.Employee;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface EmployeeDao {

    List<Employee> listAll(boolean includeInactive) throws SQLException;

    Optional<Employee> findById(long employeeId) throws SQLException;

    boolean isEmailExists(String email, Long excludeEmployeeId) throws SQLException;

    boolean isPhoneExists(String phone, Long excludeEmployeeId) throws SQLException;

    long insert(Employee employee) throws SQLException;

    void update(Employee employee) throws SQLException;

    void setStatus(long employeeId, Employee.Status status, String actor) throws SQLException;

    boolean hasOpenPickingAssignments(long employeeId) throws SQLException;

    boolean hasOpenDispatchAssignments(long employeeId) throws SQLException;
}

