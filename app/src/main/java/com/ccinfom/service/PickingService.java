/*
 * CCINFOM — Phase D
 * File: PickingService.java
 * Purpose: Orchestrate T2 (start picking + insert lines) with a single transaction.
 *
 * Flow:
 *  1) Validate pickerId, ticketId, lines (qty > 0).
 *  2) Begin transaction.
 *  3) Dao.insertPickingHeader(...) → pickingId (or fetch existing).
 *  4) Dao.insertPickingLines(pickingId, lines).
 *  5) commit(); handle SQL exceptions from DB guards clearly.
 *
 * TODOs:
 *  [ ] Surface DB guard messages (e.g., over-pick) as user-friendly prompts.
 */
