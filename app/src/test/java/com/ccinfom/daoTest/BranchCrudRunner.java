package com.ccinfom.daoTest;

import com.ccinfom.dao.impl.BranchDaoImpl;
import com.ccinfom.model.Branch;
import com.ccinfom.service.BranchService;
import com.ccinfom.service.ValidationException;
import com.ccinfom.service.impl.BranchServiceImpl;
import com.ccinfom.config.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class BranchCrudRunner {

    private BranchCrudRunner() {}

    public static void main(String[] args) throws Exception {
        BranchService service = new BranchServiceImpl(new BranchDaoImpl());
        String uniqueName = "QA Branch " + UUID.randomUUID().toString().substring(0, 8);

        Branch created = null;
        try {
            created = createBranch(service, uniqueName);
            created = updateBranch(service, created);
            toggleBranch(service, created.getBranchId(), false);
            toggleBranch(service, created.getBranchId(), true);
            System.out.println("Branch CRUD smoke test PASSED.");
        } catch (SQLException | ValidationException ex) {
            System.err.println("Branch CRUD test FAILED: " + ex.getMessage());
            throw ex;
        } finally {
            if (created != null) {
                cleanup(created.getBranchId());
            }
        }
    }

    private static Branch createBranch(BranchService service, String name)
            throws SQLException, ValidationException {
        Branch branch = new Branch();
        branch.setBranchName(name);
        branch.setCity("Metro Manila");
        branch.setAddress("123 QA Street");
        branch.setContactPerson("QA Contact");
        branch.setPhone(generatePhone());
        Branch created = service.createBranch(branch, "qa");
        System.out.println("Created branch ID=" + created.getBranchId());
        return created;
    }

    private static Branch updateBranch(BranchService service, Branch branch)
            throws SQLException, ValidationException {
        branch.setCity("Updated City");
        branch.setAddress("456 Updated Address");
        branch.setContactPerson("Updated Contact");
        branch.setPhone(generatePhone());
        Branch updated = service.updateBranch(branch, "qa");
        System.out.println("Updated branch city=" + updated.getCity());
        return updated;
    }

    private static void toggleBranch(BranchService service, long branchId, boolean active)
            throws SQLException, ValidationException {
        service.setBranchActive(branchId, active, "qa");
        System.out.println("Set branch " + branchId + " active=" + active);
    }

    private static void cleanup(long branchId) {
        String sql = "DELETE FROM branches WHERE branch_id = ?";
        try (Connection conn = DbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, branchId);
            ps.executeUpdate();
            System.out.println("Cleaned up temp branch row.");
        } catch (SQLException ex) {
            System.err.println("Failed to cleanup temp branch: " + ex.getMessage());
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
