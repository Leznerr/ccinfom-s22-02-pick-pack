/*
 * CCINFOM — Phase D
 * File: TicketService.java
 * Purpose: Orchestrate T1 creation (header + lines) with a single transaction.
 *
 * Flow:
 *  1) Validate inputs (customerId, branchId, lines not empty, qty > 0).
 *  2) Begin connection.setAutoCommit(false).
 *  3) Dao.insertHeader(...) → ticketId.
 *  4) Dao.insertLines(ticketId, lines).
 *  5) commit(); on failure rollback() and rethrow a friendly message.
 *
 * TODOs:
 *  [ ] Implement validation; return meaningful errors to UI.
 *
 * Definition of Done:
 *  - Success creates 1 header + N lines; constraints intact.
 *  - Failure leaves DB unchanged; connection always closed.
 */
