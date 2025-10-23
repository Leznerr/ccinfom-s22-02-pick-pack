package com.ccinfom.daoTest;

import com.ccinfom.dao.impl.TicketDaoImpl;
import com.ccinfom.dao.interfaces.TicketDao;
import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import com.ccinfom.config.DbConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TicketDaoTest {

    public static void main(String[] args) {

        try (Connection conn = DbConnection.getConnection()) { // pass connection
            TicketDao dao = new TicketDaoImpl(conn);

            Scanner scanner = new Scanner(System.in);

            // 1. LIST ALL TICKETS
            System.out.println("=== LIST ALL TICKETS ===");
            List<PickTicketHdr> tickets = dao.listAllTickets();
            for (PickTicketHdr hdr : tickets) {
                System.out.println(hdr);
            }

            // 2. FIND ONE TICKET BY ID using Scanner
            System.out.println("\n=== FIND TICKET BY ID ===");
            System.out.println("Enter Ticket ID to find: ");
            long searchId = scanner.nextLong();
            PickTicketHdr hdr = dao.findTicketById(searchId);
            if (hdr != null) {
                System.out.println(hdr);;
            } else {
                System.out.println("Ticket ID " + searchId + " not found.");
            }

            // 3. LIST TICKET LINES FOR HEADER
            System.out.println("\n=== LIST TICKET LINES ===");
            if (hdr != null) {
                List<PickTicketLine> lines = dao.listTicketLines(hdr.getPickTicketId());
                if (!lines.isEmpty()) {
                    for (PickTicketLine line : lines) {
                        System.out.println(line);
                    }
                } else {
                    System.out.println("No lines found for ticket " + hdr.getPickTicketId());
                }             
            }

            // 4. INSERT NEW TICKET HEADER + LINES
            System.out.println("\n=== INSERT NEW HEADER + LINES ===");

            // Make the ticket (like a shopping cart)
            PickTicketHdr newHdr = new PickTicketHdr();
            newHdr.setCustomerId(1L);
            newHdr.setBranchId(1L);
            newHdr.setTicketStatus(PickTicketHdr.TicketStatus.Open);
            newHdr.setRemarks("New order for testing insert");
            newHdr.setUpdatedBy("tester");

            long newId = dao.insertTicketHeader(newHdr); // save the ticket in the database
            System.out.println("Inserted header ID: " + newId);

            List<PickTicketLine> newLines = new ArrayList<>(); // add items(lines) to the ticket
            // Example: multiple lines
            PickTicketLine l1 = new PickTicketLine();
            l1.setPickTicketId(newId); // link to the ticket
            l1.setProductId(2L); // product id
            l1.setRequestedQty(new BigDecimal("5")); // how many
            l1.setUom("pcs"); // unit
            l1.setLineStatus(PickTicketLine.LineStatus.Valid);
            l1.setUpdatedBy("tester");
            newLines.add(l1); // add to the list

            PickTicketLine l2 = new PickTicketLine();
            l2.setPickTicketId(newId);
            l2.setProductId(3L);
            l2.setRequestedQty(new BigDecimal("10"));
            l2.setUom("pcs");
            l2.setLineStatus(PickTicketLine.LineStatus.Valid);
            l2.setUpdatedBy("tester");
            newLines.add(l2);

            dao.insertTicketLines(newId, newLines); // save all items to the database
            System.out.println("Inserted lines for ticket " + newId);

            // 5. UPDATE STATUS
            System.out.println("\n=== UPDATE STATUS ===");
            dao.updateTicketStatus(newId, PickTicketHdr.TicketStatus.Closed, "tester");
            System.out.println("Ticket " + newId + " marked as Closed.");

            // 6. CLOSE OR CANCEL
            System.out.println("\n=== CLOSE OR CANCEL ===");
            dao.closeOrCancelTicket(newId, PickTicketHdr.TicketStatus.Picking, "tester");
            System.out.println("Ticket " + newId + " marked as Closed.");

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
