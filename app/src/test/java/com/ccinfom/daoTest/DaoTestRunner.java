package com.ccinfom.daoTest;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.LookupDao;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DaoTestRunner: A non-interactive, automated test suite for the DAO layer.
 * This class replaces the old interactive tests and provides a comprehensive,
 * repeatable way to verify the work of Member 2.
 *
 * Each test method that modifies data uses a transaction and then rolls it back
 * to ensure the database is left clean after the test run.
 */
public class DaoTestRunner {

    public static void main(String[] args) {
        System.out.println("--- Comprehensive DAO Layer Test Suite ---");

        boolean allTestsPassed = true;
        allTestsPassed &= testLookupDao();
        allTestsPassed &= testTicketDao();
        allTestsPassed &= testPickingDao();

        System.out.println("\n--- Test Suite Finished ---");
        if (allTestsPassed) {
            System.out.println("Result: ALL DAO TESTS PASSED");
        } else {
            System.out.println("Result: ONE OR MORE DAO TESTS FAILED");
        }
    }

    private static boolean testLookupDao() {
        System.out.println("\n----- Testing LookupDao ----- ");
        LookupDao lookupDao = new LookupDaoImpl();
        try {
            // Test listing active products
            List<Product> products = lookupDao.listActiveProducts();
            assert !products.isEmpty() : "listActiveProducts should not be empty";
            System.out.println("1. listActiveProducts: PASSED");

            // Test finding a specific product
            Product product = lookupDao.findProductById(1L);
            assert product != null : "findProductById(1) should return a product";
            assert product.getProductId() == 1L : "Product ID should be 1";
            System.out.println("2. findProductById: PASSED");

            return true;
        } catch (Exception e) {
            System.out.println("LookupDao tests FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testTicketDao() {
        System.out.println("\n----- Testing TicketDao ----- ");
        TicketDao ticketDao = new TicketDaoImpl();
        long testTicketId = -1;

        // Use a single connection and transaction for the entire test method
        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Test insertTicketHeader
            PickTicketHdr newHdr = new PickTicketHdr();
            newHdr.setCustomerId(1L);
            newHdr.setBranchId(1L);
            newHdr.setTicketStatus(PickTicketHdr.TicketStatus.Open);
            newHdr.setUpdatedBy("daotester");
            testTicketId = ticketDao.insertTicketHeader(newHdr, conn);
            assert testTicketId > 0 : "insertTicketHeader should return a positive ID";
            System.out.println("1. insertTicketHeader: PASSED");

            // 2. Test findTicketById
            PickTicketHdr foundHdr = ticketDao.findTicketById(testTicketId);
            assert foundHdr != null : "findTicketById should retrieve the inserted header";
            assert foundHdr.getCustomerId() == 1L : "Retrieved customer ID does not match";
            System.out.println("2. findTicketById: PASSED");

            // 3. Test insertTicketLines
            List<PickTicketLine> newLines = new ArrayList<>();
            PickTicketLine line1 = new PickTicketLine();
            line1.setPickTicketId(testTicketId);
            line1.setProductId(1L);
            line1.setRequestedQty(new BigDecimal("100"));
            line1.setUom("PC");
            line1.setLineStatus(PickTicketLine.LineStatus.Valid);
            line1.setUpdatedBy("daotester");
            newLines.add(line1);
            ticketDao.insertTicketLines(newLines, conn);
            System.out.println("3. insertTicketLines: PASSED");

            // 4. Test listTicketLines
            List<PickTicketLine> foundLines = ticketDao.listTicketLines(testTicketId);
            assert foundLines.size() == 1 : "listTicketLines should return 1 line";
            assert foundLines.get(0).getRequestedQty().compareTo(new BigDecimal("100")) == 0 : "Retrieved quantity does not match";
            System.out.println("4. listTicketLines: PASSED");

            // 5. Test updateTicketStatus
            ticketDao.updateTicketStatus(testTicketId, PickTicketHdr.TicketStatus.Closed, "daotester", conn);
            PickTicketHdr updatedHdr = ticketDao.findTicketById(testTicketId);
            assert updatedHdr.getTicketStatus() == PickTicketHdr.TicketStatus.Closed : "Ticket status was not updated";
            System.out.println("5. updateTicketStatus: PASSED");

            // If all assertions pass, we rollback and return true
            conn.rollback();
            return true;

        } catch (Exception e) {
            System.out.println("TicketDao tests FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean testPickingDao() {
        System.out.println("\n----- Testing PickingDao ----- ");
        TicketDao ticketDao = new TicketDaoImpl();
        PickingDao pickingDao = new PickingDaoImpl();
        long testTicketId = -1;
        long testPickingId = -1;

        try (Connection conn = DbConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Setup: Create a ticket to pick against
            PickTicketHdr newHdr = new PickTicketHdr();
            newHdr.setCustomerId(1L);
            newHdr.setBranchId(1L);
            newHdr.setTicketStatus(PickTicketHdr.TicketStatus.Open);
            newHdr.setUpdatedBy("daotester");
            testTicketId = ticketDao.insertTicketHeader(newHdr, conn);

            // 1. Test insertPickingHeader
            PickingHdr newPickingHdr = new PickingHdr();
            newPickingHdr.setPickTicketId(testTicketId);
            newPickingHdr.setPickerEmployeeId(1L);
            newPickingHdr.setPickingStatus("Picking");
            newPickingHdr.setUpdatedBy("daotester");
            testPickingId = pickingDao.insertPickingHeader(newPickingHdr, conn);
            assert testPickingId > 0 : "insertPickingHeader should return a positive ID";
            System.out.println("1. insertPickingHeader: PASSED");

            // 2. Test findByTicketId
            PickingHdr foundHdr = pickingDao.findByTicketId(testTicketId);
            assert foundHdr != null : "findByTicketId should retrieve the header";
            assert foundHdr.getPickerEmployeeId() == 1L : "Retrieved picker ID does not match";
            System.out.println("2. findByTicketId: PASSED");

            // Rollback changes at the end
            conn.rollback();
            return true;

        } catch (Exception e) {
            System.out.println("PickingDao tests FAILED: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
