# Phase E Decisions & Stage 5 Safeguards

## Inventory Logging Policy
- Only **PickingService** (reserve) and **CloseService** (close) call `InventoryHelper.applyDelta`, which enforces `SELECT … FOR UPDATE` and writes to `inventory_txn_log` when a non-zero delta occurs.
- Packing does **not** log because it does not change on-hand/reserved totals; QA relies on the log rollup to prove Σ(logs) = balances.
- QA Query `inventory_log_vs_products` (see `qa/validation_queries.sql`) compares each product’s current `reserved_qty`/`on_hand_qty` against the seed baseline plus logged deltas. Any mismatch fails the pipeline.

## Dispatch Capacity Guard
- `vehicles.capacity` is treated as the maximum number of sealed boxes a manifest may load. (`dispatch_line` stores one row per box, so the count is deterministic.)
- `qa/validation_queries.sql` includes `dispatch_vehicle_capacity` which returns manifests where `vehicle_status <> 'available'` or `box_count > capacity`. This complements the service-level `DISPATCH_VEHICLE_UNAVAILABLE` and `DISPATCH_CAPACITY_EXCEEDED` exceptions.

## QA Automation & Evidence
- `scripts/qa/run-validation.ps1` standardises execution of all QA SQL gates. It accepts CLI parameters or the `CCINFOM_DB_*` environment variables, then stores the raw MySQL output under `qa/tests/phaseE-validation-<timestamp>.log`.
- Automated regression tests live in `com.ccinfom.test.PhaseEServiceTestRunner`. The suite covers every happy path plus each validation exception (Pack, Dispatch, Close) and InventoryHelper lock handling. Stage 5 is considered green only when this runner prints `Summary: N passed, 0 failed` **and** the QA script reports zero violations.

## Core Record Status Flags (Phase F)
- Added `customer_status` and `branch_status` columns so every core table now has an explicit active/inactive marker (`products.active_flag`, `employees.employee_status`, `vehicles.vehicle_status`).
- Migrations: `db/migrations/phase_f_core_status.sql` alters existing databases and defaults new rows to `active`.
- Why: upcoming CRUD modules need a consistent deactivate/reactivate mechanism, and Reporting Rule 9 requires “only active cores are usable in T1–T5.”

## Core Records Menu & Lookup Enforcement (Phase F)
- MainApp now exposes a “Core Records” menu linking to Products/Customers/Branches/Employees/Vehicles to keep access consistent with the home buttons.
- LookupDao(active*) methods now filter by status columns (customers, branches, employees, vehicles) so dropdowns honor deactivation without service-layer overrides.
- Vehicle lookups also include `sla_hours` alongside capacity/type/status to keep UI/model mappings complete.

Keep this file updated whenever a new policy impacts how we prove correctness (e.g., additional log types, capacity heuristics, or QA tooling). Reviewed: 2025‑11‑09.

> TODO[Phase F]: Append reporting policies here (time-grain standards, ISO-week helper definition, on-time delivery formula, PoD compliance rules, picker productivity metrics).
