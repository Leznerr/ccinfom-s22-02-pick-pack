package com.ccinfom.service.impl;

import com.ccinfom.dao.interfaces.BranchDao;
import com.ccinfom.model.Branch;
import com.ccinfom.service.BranchService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.util.CoreValidationUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class BranchServiceImpl implements BranchService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_INACTIVE = "inactive";
    private static final String SYSTEM_USER = "system";

    private final BranchDao branchDao;

    public BranchServiceImpl(BranchDao branchDao) {
        this.branchDao = branchDao;
    }

    @Override
    public List<Branch> listBranches(String filter, boolean includeInactive) throws SQLException {
        String keyword = filter != null ? filter.trim().toLowerCase(Locale.ROOT) : "";
        return branchDao.listAll(includeInactive).stream()
                .filter(b -> keyword.isEmpty() || matches(b, keyword))
                .collect(Collectors.toList());
    }

    @Override
    public Branch createBranch(Branch branch, String actor)
            throws SQLException, ValidationException {
        Branch sanitized = sanitize(branch);
        validateBranch(sanitized, null);
        sanitized.setBranchStatus(STATUS_ACTIVE);
        sanitized.setUpdatedBy(resolveActor(actor));
        long id = branchDao.insert(sanitized);
        return branchDao.findById(id).orElseThrow(() ->
                new SQLException("Could not load branch after insert."));
    }

    @Override
    public Branch updateBranch(Branch branch, String actor)
            throws SQLException, ValidationException {
        if (branch.getBranchId() == null) {
            throw new ValidationException("BRANCH_ID_REQUIRED", "Branch ID is required for updates.");
        }
        Branch sanitized = sanitize(branch);
        validateBranch(sanitized, sanitized.getBranchId());
        sanitized.setUpdatedBy(resolveActor(actor));
        branchDao.update(sanitized);
        return branchDao.findById(sanitized.getBranchId()).orElseThrow(() ->
                new SQLException("Could not load branch after update."));
    }

    @Override
    public void setBranchActive(long branchId, boolean active, String actor)
            throws SQLException, ValidationException {
        Branch branch = branchDao.findById(branchId)
                .orElseThrow(() -> new ValidationException("BRANCH_NOT_FOUND", "Branch not found."));
        String newStatus = active ? STATUS_ACTIVE : STATUS_INACTIVE;
        if (newStatus.equalsIgnoreCase(branch.getBranchStatus())) {
            return;
        }
        if (!active && branchDao.hasActiveTickets(branchId)) {
            throw new ValidationException("BRANCH_HAS_ACTIVE_TICKETS",
                    "Cannot deactivate branch while tickets are still in progress.");
        }
        branchDao.setStatus(branchId, newStatus, resolveActor(actor));
    }

    private boolean matches(Branch branch, String keyword) {
        return contains(branch.getBranchName(), keyword)
                || contains(branch.getCity(), keyword)
                || contains(branch.getContactPerson(), keyword);
    }

    private boolean contains(String field, String keyword) {
        return field != null && field.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Branch sanitize(Branch branch) {
        Branch copy = new Branch();
        copy.setBranchId(branch.getBranchId());
        copy.setBranchName(trim(branch.getBranchName()));
        copy.setAddress(trim(branch.getAddress()));
        copy.setCity(trim(branch.getCity()));
        copy.setContactPerson(trim(branch.getContactPerson()));
        copy.setPhone(trim(branch.getPhone()));
        copy.setBranchStatus(branch.getBranchStatus());
        return copy;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private void validateBranch(Branch branch, Long excludeId)
            throws ValidationException, SQLException {
        CoreValidationUtil.requireNonBlank(branch.getBranchName(),
                "BRANCH_NAME_REQUIRED", "Branch name is required.");
        CoreValidationUtil.requireNonBlank(branch.getAddress(),
                "BRANCH_ADDRESS_REQUIRED", "Address is required.");
        CoreValidationUtil.requireNonBlank(branch.getCity(),
                "BRANCH_CITY_REQUIRED", "City is required.");
        CoreValidationUtil.validatePhoneFormat(branch.getPhone(),
                "BRANCH_PHONE_INVALID", "Phone number");

        if (branchDao.isPhoneExists(branch.getPhone(), excludeId)) {
            throw new ValidationException("BRANCH_PHONE_DUPLICATE",
                    "Phone number already exists: " + branch.getPhone());
        }
    }

    private String resolveActor(String actor) {
        return (actor == null || actor.isBlank()) ? SYSTEM_USER : actor;
    }
}

