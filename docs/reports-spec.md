# Phase F Reporting Specs (TODO)

> TODO[Stage 0]: Each report owner must replace the placeholders below with the final, agreed-upon definitions (purpose, time grain, metrics, joins, sample expectations) before implementation starts. Once filled, this document becomes the frozen reference for Phase F.

## R1 – Daily Pick & Pack Outcomes (Owner: Renzel)
- **Purpose:** TODO – Describe the daily business question this report answers.
- **Time Grain & Timestamp:** TODO – Specify the exact column driving the day bucket (e.g., `pick_ticket_hdr.created_at`).
- **Metrics & Formulas:** TODO – List each status/shortage metric and how it is calculated.
- **Required Tables/Joins:** TODO – Enumerate tables (tickets, picking, packing, customers, products) and key join paths.
- **Sample Expectation:** TODO – Cite a seeded ticket/date combo and the counts it should show.

## R2 – Weekly Picker Productivity (Owner: Joshua)
- **Purpose:** TODO – Summarize the operational insight (efficiency per picker per ISO week).
- **Time Grain & Timestamp:** TODO – Confirm iso_year/iso_week logic and source timestamp from picking sessions.
- **Metrics & Formulas:** TODO – Lines picked, units picked, units/hour, error counts with formulas.
- **Required Tables/Joins:** TODO – List picking_hdr/line, employees, products, dim_date joins.
- **Sample Expectation:** TODO – Provide a seeded picker/week scenario with expected values.

## R3 – Monthly Inventory / Pack–Dispatch Throughput (Owner: Gab)
- **Purpose:** TODO – Define how this report illustrates monthly throughput + inventory deltas.
- **Time Grain & Timestamp:** TODO – Specify month/year bucket and timestamp source (pack/dispatch/inventory log).
- **Metrics & Formulas:** TODO – Boxes packed, manifests, inventory delta, SLA metrics.
- **Required Tables/Joins:** TODO – Mention pack_box_hdr/line, dispatch_hdr/line, inventory_txn_log, products, branches.
- **Sample Expectation:** TODO – Identify a month from seeds and the totals expected.

## R4 – On-Time Delivery & PoD Compliance (Owner: Carlo)
- **Purpose:** TODO – Explain how the report highlights on-time vs late deliveries and PoD capture.
- **Time Grain & Timestamp:** TODO – Commit to Month or ISO Week and identify timestamps (promised vs actual).
- **Metrics & Formulas:** TODO – Define on-time flag, PoD compliance %, shortage rollups.
- **Required Tables/Joins:** TODO – List dispatch_hdr/line, close_hdr, customers, vehicles, employees, dim_date.
- **Sample Expectation:** TODO – Cite Ticket A/B (or other seeds) showing on-time vs late classification.

## Change Control
- **TODO:** Document the date and summary whenever the team revises any report spec after the initial lock.
