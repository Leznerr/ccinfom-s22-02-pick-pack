package com.ccinfom.daoTest;

import com.ccinfom.config.DbConnection;
import com.ccinfom.dao.impl.PickingDaoImpl;
import com.ccinfom.dao.interfaces.PickingDao;
import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public class PickingDaoTest {

    public static void main(String[] args) {
        try (Connection conn = DbConnection.getConnection();
             Scanner scanner = new Scanner(System.in)) {

            PickingDao dao = new PickingDaoImpl(conn);

            System.out.println("=== PICKING DAO TEST START ===");

            // 1. INSERT PICKING HEADER
            System.out.println("\n=== INSERT NEW PICKING HEADER ===");
            System.out.print("Enter pick_ticket_id to insert: ");
            long ticketId = scanner.nextLong();

            PickingHdr newHdr = new PickingHdr();
            newHdr.setPickTicketId(ticketId);
            newHdr.setPickerEmployeeId(2L);
            newHdr.setPickingStatus("Picking");
            newHdr.setStartedAt(LocalDateTime.now());
            newHdr.setUpdatedBy("tester");

            long pickingId = dao.insertPickingHeader(newHdr);
            System.out.println("Inserted or fetched picking header ID: " + pickingId);

            // 2. FIND PICKING BY TICKET ID
            System.out.println("\n=== FIND PICKING BY TICKET ID ===");
            System.out.print("Enter ticket ID to search picking: ");
            long searchTicketId = scanner.nextLong();

            PickingHdr foundHdr = dao.findByTicketId(searchTicketId);
            if (foundHdr != null) {
                System.out.println("Found picking header: " + foundHdr.getPickingId());
                System.out.println("Status: " + foundHdr.getPickingStatus());
            } else {
                System.out.println("No picking found for ticket ID " + searchTicketId);
            }

            // 3. LIST LINES BY PICKING ID
            System.out.println("\n=== LIST LINES BY PICKING ID ===");
            System.out.print("Enter picking ID to list lines: ");
            long searchPickingId = scanner.nextLong();

            List<PickingLine> lines = dao.listLinesByPickingId(searchPickingId);
            if (!lines.isEmpty()) {
                for (PickingLine l : lines) {
                    System.out.println("Line ID: " + l.getPickingLineId()
                            + ", Product: " + l.getProductId()
                            + ", Qty: " + l.getPickedQty());
                }
            } else {
                System.out.println("No lines found for picking ID " + searchPickingId);
            }

            System.out.println("\n=== PICKING DAO TEST END ===");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
