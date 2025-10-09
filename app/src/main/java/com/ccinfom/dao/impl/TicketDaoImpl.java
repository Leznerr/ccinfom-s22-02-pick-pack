/*
 * CCINFOM — Phase D
 * File: TicketDaoImpl.java
 * Purpose: JDBC implementation of TicketDao.
 *
 * TODOs:
 *  [ ] Implement insertHeader(): INSERT pick_ticket_hdr (...) RETURN generated key.
 *  [ ] Implement insertLines(): batch insert pick_ticket_line with same ticket_id.
 *  [ ] Wrap SQLExceptions with context (which method, key values).
 *
 * Definition of Done:
 *  - Works against seeded DB; verified in Workbench and Java.
 *  - No resource leaks; PreparedStatements closed.
 */
