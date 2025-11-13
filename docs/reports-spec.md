# Phase F Reporting Specs (Stage 0 – Locked)

The definitions below were agreed upon by the team on 2025‑11‑10 and will remain frozen unless the group explicitly approves revisions.

## R1 – Daily Pick & Pack Outcomes (Owner: Renzel)

- **Purpose:** Provide a daily end-of-pipeline summary showing how many tickets were closed as Delivered vs Short-Closed, plus shortage incidents (lines and units) so the team can react to spikes in shortages or throughput.
- **Time Grain & Timestamp:** Day; use `DATE(close_hdr.created_at)` to anchor each ticket close date.
- **Metrics & Formulas:**
  - Delivered tickets: `COUNT(DISTINCT CASE WHEN ch.final_status='Delivered' THEN ch.close_id END)`
  - Short-Closed tickets: `COUNT(DISTINCT CASE WHEN ch.final_status='Short-Closed' THEN ch.close_id END)`
  - Shortage lines: `COUNT(CASE WHEN cv.short_qty>0 THEN cv.variance_id END)`
  - Shorted units: `SUM(CASE WHEN cv.short_qty>0 THEN cv.short_qty ELSE 0 END)`
  - Tickets closed (total): `COUNT(DISTINCT ch.close_id)`
  - Optional top shortage reasons per day: aggregate `cv.reason` where `cv.short_qty>0`
- **Required Tables/Joins:** `close_hdr ch` → `close_variance cv`; optional joins to `pick_ticket_hdr`, `customers`, `branches`, `products` for drill-downs.
- **Sample Expectation:** Using seed tickets A & B (close IDs 1 & 2) closed on 2025‑11‑10:
  - Delivered = 1, Short-Closed = 1, Shortage lines = 3, Shorted units = 7.00, Tickets closed = 2.

## R2 – Weekly Picker Productivity (Owner: Joshua)

- **Purpose:** Measure picker efficiency per ISO week (lines, units, units/hour, avg pick time, error counts) broken down by product category to highlight top performers and bottlenecks.
- **Time Grain & Timestamp:** ISO week (iso_year + iso_week) derived from `picking_hdr.completed_at` using `YEARWEEK(...,3)`.
- **Metrics & Formulas:**
  - Lines picked: `COUNT(DISTINCT pl.picking_line_id)`
  - Units picked: `SUM(pl.picked_qty)`
  - Units per hour: `SUM(pl.picked_qty) / NULLIF(SUM(TIMESTAMPDIFF(MINUTE, ph.started_at, ph.completed_at))/60,0)`
  - Average pick time (minutes): `AVG(TIMESTAMPDIFF(MINUTE, ph.started_at, ph.completed_at))`
  - Short/error lines: `COUNT(DISTINCT CASE WHEN pl.short_reason IS NOT NULL THEN pl.picking_line_id END)`
- **Required Tables/Joins:** `picking_hdr ph` → `employees e` → `picking_line pl` → `products p` (category) → optional `pick_ticket_hdr t` for branch filtering.
- **Sample Expectation:** Seed week ISO 2025‑W45 – Ticket A (picker 1) + Ticket B (picker 2):
  - Picker 1 lines=2 units=10 errors=0; Picker 2 lines=3 units=15 errors=1; totals lines=5, units=25, error lines=1.

## R3 – Monthly Return Cost & Shortage Exposure (Owner: Mark)

- **Purpose:** Quantify the monthly financial impact of short/return activity by converting shorted quantities into cost (short_qty × unit_price) and tracking exposure (tickets, quantities, fulfillment rate).
- **Time Grain & Timestamp:** Month (Year + Month) using `close_hdr.pod_ts`.
- **Metrics & Formulas:**
  - Total tickets closed: `COUNT(DISTINCT ch.close_id)`
  - Short-closed tickets: `COUNT(DISTINCT CASE WHEN ch.final_status='Short-Closed' THEN ch.close_id END)`
  - Short-close rate %: `100 * short_closed / NULLIF(total_tickets,0)`
  - Total requested qty: `SUM(cv.requested_qty)`
  - Total delivered qty: `SUM(cv.delivered_qty)`
  - Total short qty: `SUM(cv.short_qty)`
  - Estimated return cost: `SUM(cv.short_qty * p.unit_price)` via `pick_ticket_line → products`
  - Fulfillment rate %: `100 * SUM(cv.delivered_qty) / NULLIF(SUM(cv.requested_qty),0)`
- **Required Tables/Joins:** `close_hdr`, `close_variance`, `pick_ticket_line`, `products` (unit_price), optional `pick_ticket_hdr` for customer/branch filters.
- **Sample Expectation:** Seeds `seed-T5-hdr-delivered` & `seed-T5-hdr-short` in Nov 2024:
  - Tickets closed=2, Short-closed=1, Short qty=10, Return cost = 10 × unit_price, Fulfillment ≈ 71%.

## R4 – Monthly On-Time Delivery & Shortage Reasons (Owner: Carlo)

- **Purpose:** Show monthly on-time vs late deliveries per customer/product and summarize shortage reasons to improve dispatch reliability and PoD compliance.
- **Time Grain & Timestamp:** Month (Year + Month) using `close_hdr.pod_ts` for delivery completion.
- **Metrics & Formulas:**
  - On-time deliveries: `COUNT(DISTINCT CASE WHEN ch.final_status='Delivered' AND ch.pod_ts <= dh.depart_ts + INTERVAL IFNULL(v.sla_hours,24) HOUR THEN ch.pick_ticket_id END)`
  - Late deliveries: same count but `>` SLA threshold.
  - Short-closed shipments: `COUNT(DISTINCT CASE WHEN ch.final_status='Short-Closed' THEN ch.pick_ticket_id END)`
  - Shortage reasons count: `COUNT(CASE WHEN cv.short_qty>0 THEN cv.reason END)`
  - On-time %: `100 * on_time / NULLIF(delivered,0)`
- **Required Tables/Joins:** `pick_ticket_hdr` → `dispatch_hdr` → `dispatch_line` → `close_hdr` → `close_variance` → `pick_ticket_line` → `products`; joins to `customers`, `vehicles` (SLA), `employees` (driver).
- **Sample Expectation:** Seeds `seed-T4-hdr-001/002` in Nov 2025:
  - On-time deliveries=1 (Ticket A), Late=0 (assuming SLA met), Short-closed=1 (Ticket B), Shortage reasons=2, On-time %=100%.

## Change Control

- **2025‑11‑10:** Initial Stage 0 lock for R1–R4 (Renzel, Joshua, Mark, Carlo). Any changes require group consensus and an entry here.
