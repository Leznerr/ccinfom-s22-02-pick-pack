package com.ccinfom.dao.interfaces;

import com.ccinfom.model.Branch;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface BranchDao {

    List<Branch> listAll(boolean includeInactive) throws SQLException;

    Optional<Branch> findById(long branchId) throws SQLException;

    boolean isPhoneExists(String phone, Long excludeBranchId) throws SQLException;

    long insert(Branch branch) throws SQLException;

    void update(Branch branch) throws SQLException;

    void setStatus(long branchId, String status, String actor) throws SQLException;

    boolean hasActiveTickets(long branchId) throws SQLException;
}

