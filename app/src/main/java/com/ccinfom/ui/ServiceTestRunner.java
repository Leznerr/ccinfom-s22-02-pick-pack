package com.ccinfom.ui;

import com.ccinfom.dao.impl.LookupDaoImpl;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.service.PickingService;
import com.ccinfom.service.TicketService;
import com.ccinfom.service.ValidationException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A comprehensive test suite for Member 3's work: TicketService and PickingService.
 *
 * This runner executes a series of tests for both the "happy path" (success)
 * and, more importantly, the "exception paths" (failure) to ensure that the
 * validation logic and business rules are correctly enforced.
 */
public class ServiceTestRunner {

    // DAOs and Services are shared across all test methods
    private static TicketService ticketService;
    private static PickingService pickingService;

    public static void main(String[] args) {
        System.out.println("--- Comprehensive Service Layer Test Suite ---");

        // --- SETUP ---
        // Instantiate the DAOs and Services once.
        TicketDaoImpl ticketDao = new TicketDaoImpl();
        PickingDaoImpl pickingDao = new PickingDaoImpl();
        LookupDaoImpl lookupDao = new LookupDaoImpl();
        ticketService = new TicketService(ticketDao, lookupDao);
        pickingService = new PickingService(pickingDao, ticketDao, lookupDao);

        // --- EXECUTE TICKET SERVICE TESTS ---
        System.out.println("\n----- Testing TicketService ----- ");
        long validTicketId = test_T1_Success_CreateValidTicket();
        test_T1_Fail_ZeroQuantity();
        test_T1_Fail_DuplicateProducts();
        test_T1_Fail_NonExistentProduct();
        test_T1_Fail_NoLines();

        // --- EXECUTE PICKING SERVICE TESTS ---
        System.out.println("\n----- Testing PickingService ----- ");
        if (validTicketId > 0) {
            test_T2_Success_StartPicking(validTicketId);
            test_T2_Fail_StartPickingAgain(validTicketId);
        } else {
            System.out.println("SKIPPING PickingService tests because no valid ticket was created.");
        }
        test_T2_Fail_StartPickingNonExistentTicket();

        System.out.println("\n--- Test Suite Finished ---");
    }

    // --- T1: TICKET SERVICE TEST CASES ---

    private static long test_T1_Success_CreateValidTicket() {
        System.out.print("1. T1-Success (Create Valid Ticket): ");
        try {
            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L); // Assumes customer 1 exists
            hdr.setBranchId(1L);   // Assumes branch 1 exists
            hdr.setUpdatedBy("testrunner");

            List<PickTicketLine> lines = new ArrayList<>();
            PickTicketLine line = new PickTicketLine();
            line.setProductId(1L); // Assumes product 1 exists and is active
            line.setRequestedQty(new BigDecimal("10.0"));
            line.setUom("PC"); // <-- This line was missing
            lines.add(line);

            long newId = ticketService.createPickTicket(hdr, lines);
            System.out.println("PASSED. Created ticket ID: " + newId);
            return newId;
        } catch (Exception e) {
            System.out.println("FAILED. Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

    private static void test_T1_Fail_ZeroQuantity() {
        System.out.print("2. T1-Fail (Zero Quantity): ");
        try {
            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L);
            hdr.setBranchId(1L);
            hdr.setUpdatedBy("testrunner");

            List<PickTicketLine> lines = new ArrayList<>();
            PickTicketLine line = new PickTicketLine();
            line.setProductId(1L);
            line.setRequestedQty(BigDecimal.ZERO); // Invalid quantity
            lines.add(line);

            ticketService.createPickTicket(hdr, lines);
            System.out.println("FAILED. Service allowed ticket with zero quantity.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }

    private static void test_T1_Fail_DuplicateProducts() {
        System.out.print("3. T1-Fail (Duplicate Products): ");
        try {
            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L);
            hdr.setBranchId(1L);
            hdr.setUpdatedBy("testrunner");

            List<PickTicketLine> lines = new ArrayList<>();
            PickTicketLine line1 = new PickTicketLine();
            line1.setProductId(1L);
            line1.setRequestedQty(new BigDecimal("10"));
            lines.add(line1);

            PickTicketLine line2 = new PickTicketLine(); // Duplicate product ID
            line2.setProductId(1L);
            line2.setRequestedQty(new BigDecimal("5"));
            lines.add(line2);

            ticketService.createPickTicket(hdr, lines);
            System.out.println("FAILED. Service allowed ticket with duplicate products.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }

    private static void test_T1_Fail_NonExistentProduct() {
        System.out.print("4. T1-Fail (Non-Existent Product): ");
        try {
            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L);
            hdr.setBranchId(1L);
            hdr.setUpdatedBy("testrunner");

            List<PickTicketLine> lines = new ArrayList<>();
            PickTicketLine line = new PickTicketLine();
            line.setProductId(99999L); // Assumes product 99999 does not exist
            line.setRequestedQty(new BigDecimal("10"));
            lines.add(line);

            ticketService.createPickTicket(hdr, lines);
            System.out.println("FAILED. Service allowed ticket with non-existent product.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }

    private static void test_T1_Fail_NoLines() {
        System.out.print("5. T1-Fail (No Lines): ");
        try {
            PickTicketHdr hdr = new PickTicketHdr();
            hdr.setCustomerId(1L);
            hdr.setBranchId(1L);
            hdr.setUpdatedBy("testrunner");

            List<PickTicketLine> lines = new ArrayList<>(); // Empty list

            ticketService.createPickTicket(hdr, lines);
            System.out.println("FAILED. Service allowed ticket with no lines.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }

    // --- T2: PICKING SERVICE TEST CASES ---

    private static void test_T2_Success_StartPicking(long ticketId) {
        System.out.print("6. T2-Success (Start Picking): ");
        try {
            long pickerEmployeeId = 1L; // Assumes employee 1 exists
            pickingService.assignPickerAndStartPicking(ticketId, pickerEmployeeId);
            System.out.println("PASSED. Picking started for ticket ID: " + ticketId);
        } catch (Exception e) {
            System.out.println("FAILED. Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void test_T2_Fail_StartPickingAgain(long ticketId) {
        System.out.print("7. T2-Fail (Start Picking Again): ");
        try {
            long pickerEmployeeId = 2L; // Different employee
            pickingService.assignPickerAndStartPicking(ticketId, pickerEmployeeId);
            System.out.println("FAILED. Service allowed picking to be started twice.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }

    private static void test_T2_Fail_StartPickingNonExistentTicket() {
        System.out.print("8. T2-Fail (Non-Existent Ticket): ");
        try {
            pickingService.assignPickerAndStartPicking(99999L, 1L);
            System.out.println("FAILED. Service started picking for a non-existent ticket.");
        } catch (ValidationException e) {
            System.out.println("PASSED. Service correctly blocked request: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("FAILED. Test threw an unexpected SQL error: " + e.getMessage());
        }
    }
}
