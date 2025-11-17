package com.ccinfom.service;

import com.ccinfom.model.Branch;

import java.sql.SQLException;
import java.util.List;

public interface BranchService {

    List<Branch> listBranches(String filter, boolean includeInactive) throws SQLException;

    Branch createBranch(Branch branch, String actor) throws SQLException, ValidationException;

    Branch updateBranch(Branch branch, String actor) throws SQLException, ValidationException;

    void setBranchActive(long branchId, boolean active, String actor) throws SQLException, ValidationException;
}

