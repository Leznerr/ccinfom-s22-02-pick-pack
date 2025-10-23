/*
 * CCINFOM — Phase D
 * File: TicketDao.java
 * Purpose: DB operations for T1 (pick_ticket_hdr, pick_ticket_line).
 *
 * Required methods:
 *  - long insertHeader(PickTicketHdr hdr)
 *  - void  insertLines(long pickTicketId, List<PickTicketLine> lines)
 *  - Optional: List<PickTicketHdr> listRecent(int limit)
 *
 * TODOs:
 *  [ ] Use PreparedStatement only; no string concatenation.
 *  [ ] Return generated keys; throw SQLException with context on failure.
 *
 * Definition of Done:
 *  - Inserts match schema; UNIQUE/FK constraints honored.
 *  - Methods close all JDBC resources reliably.
 */

package com.ccinfom.dao.interfaces;

import com.ccinfom.model.PickTicketHdr;
import com.ccinfom.model.PickTicketLine;
import java.sql.SQLException;
import java.util.List;

public interface TicketDao {

    // -------------------- READ --------------------
    List<PickTicketHdr> listAllTickets() throws SQLException;

    PickTicketHdr findTicketById(long pickTicketId) throws SQLException;

    List<PickTicketLine> listTicketLines(long pickTicketId) throws SQLException;

    // -------------------- CREATE --------------------
    long insertTicketHeader(PickTicketHdr hdr) throws SQLException;

    void insertTicketLines(long pickTicketId, List<PickTicketLine> lines) throws SQLException;

    // -------------------- UPDATE --------------------
    void updateTicketStatus(long pickTicketId, PickTicketHdr.TicketStatus status, String updatedBy) throws SQLException;

    // -------------------- DELETE / CLOSE --------------------
    void closeOrCancelTicket(long pickTicketId, PickTicketHdr.TicketStatus status, String updatedBy) throws SQLException;
}