/*
 * CCINFOM — Phase D
 * File: PickingForm.java
 * Purpose: Minimal Swing form to start picking and add picks (T2).
 *
 * UI:
 *  - Dropdowns: Ticket (with lines), Picker (active pickers).
 *  - Lines table: TicketLine, Product (read-only), Picked Qty, UoM.
 *  - Buttons: Start Picking, Save Lines.
 *
 * TODOs:
 *  [ ] On "Start Picking": call PickingService.start(...) to create/fetch picking_hdr.
 *  [ ] On "Save Lines": call service to insert picks; show DB errors (e.g., over-pick).
 *
 * Definition of Done:
 *  - Status flip to 'Picking' happens; reserved_qty increases per picks (see QA script).
 */
