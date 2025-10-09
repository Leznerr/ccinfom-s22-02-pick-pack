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
