/*
 * CCINFOM — Phase D
 * File: TicketForm.java
 * Purpose: Minimal Swing form to create a pick ticket (T1).
 *
 * UI:
 *  - Dropdowns: Customer, Branch.
 *  - Lines table: Product, Requested Qty, UoM (allow 2–3 rows).
 *  - Buttons: Save, Clear.
 *
 * TODOs:
 *  [ ] Load dropdowns via LookupDao.
 *  [ ] On Save → TicketService.createTicket(hdr, lines) with validation prompts.
 *
 * Definition of Done:
 *  - Saving inserts rows; refresh shows new ticket id; errors are shown clearly.
 */
