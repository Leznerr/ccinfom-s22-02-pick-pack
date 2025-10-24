/*
 * CCINFOM — Phase D
 * File: PickingDao.java
 * Purpose: DB operations for T2 (picking_hdr, picking_line).
 *
 * Required methods:
 *  - long insertPickingHeader(PickingHdr hdr)  // ignore insert if exists (unique by ticket)
 *  - void  insertPickingLines(long pickingId, List<PickingLine> lines)
 *  - Optional: PickingHdr findByTicketId(long pickTicketId)
 *
 * TODOs:
 *  [ ] Anti-dup logic consistent with UNIQUE (pick_ticket_id) and (picking_id, ticket_line_id).
 *
 * Definition of Done:
 *  - Works with triggers/guards defined in schema; clear errors on violations.
 */

package com.ccinfom.dao.interfaces;

import java.sql.SQLException;
import java.util.List;

import com.ccinfom.model.PickingHdr;
import com.ccinfom.model.PickingLine;

public interface PickingDao {
    long insertPickingHeader(PickingHdr hdr) throws Exception;
    void insertPickingLines(long pickingId, List<PickingLine> lines) throws Exception;
    PickingHdr findByTicketId(long pickTicketId) throws Exception;
    List<PickingLine> listLinesByPickingId(long pickingId) throws SQLException;
}