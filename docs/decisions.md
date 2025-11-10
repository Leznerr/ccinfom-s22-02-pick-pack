Decisions Log

DATE: 2025-09-25
AREA: Phase B scope
DECISION: Cores only; no foreign keys in cores.
RATIONALE: Establishes stable master data first and avoids premature coupling; lets us reseed/clean cores without cascade issues. Relationships are clearer and enforceable later in transactions where business context exists (pick/pack/dispatch).


DATE: 2025-09-25
AREA: Naming & audit
DECISION: snake_case; PK = <table>_id (BIGINT UNSIGNED AUTO_INCREMENT); audit trio (created_at, updated_at, updated_by) on all tables.
RATIONALE: Predictable names ease SQL/Java integration; BIGINT UNSIGNED scales; the audit trio provides traceability (who/when changed).


DATE: 2025-09-25
AREA: Quantities & money precision
DECISION: Use DECIMAL(12,2) for money/qty; CHECK (value >= 0) where applicable.
RATIONALE: Two decimal places are standard for price/qty; 12 total digits safely cover B2B volumes without overflow; checks catch negatives early.


DATE: 2025-09-25
AREA: Uniqueness keys (cores)
DECISION: Enforce UNIQUE on products.sku, employees.email, vehicles.plate_number.
RATIONALE: Prevents duplicates at the source; aligns with master-data reality and simplifies joins/reports.


DATE: 2025-09-25
AREA: Vehicles core normalization
DECISION: vehicle_id BIGINT AI; vehicle_type ENUM('van','truck','motorcycle'); vehicle_status ENUM('available','maintenance','inactive'); capacity DECIMAL(12,2) CHECK (capacity >= 0).
RATIONALE: Normalized types simplify scheduling and capacity checks; status enum supports dispatch workflow; precise capacity supports packing/dispatch later.


DATE: 2025-09-25
AREA: Customers fields
DECISION: Use company as customer_name; keep contact_person, phone, email, default_delivery_address.
RATIONALE: B2B is account-driven; defaults speed up ticket creation; enables dispatch coordination and reporting without early over-normalization.


DATE: 2025-09-25
AREA: Customers email check
DECISION: CHECK (email IS NULL OR email LIKE '%@%').
RATIONALE: Catches obvious typos while allowing phone-only onboarding; fits progressive data-quality approach.


DATE: 2025-09-25
AREA: Branches fields
DECISION: branch_name, address, city NOT NULL, contact_person, phone.
RATIONALE: City is mandatory for routing; address/contacts enable labeling and receiving coordination.


DATE: 2025-09-25
AREA: Branch ↔ Customer link (cores)
DECISION: No core FK linkage; association handled in transactions and UI cross-reference.
RATIONALE: Keeps masters decoupled; operational link is explicit on tickets/dispatches; easier customer/site merges.


DATE: 2025-09-25
AREA: Charset/engine
DECISION: Use server defaults (no explicit ENGINE/collation in Phase B).
RATIONALE: Maximizes portability across lab machines and dump/restore; we’ll lock collation only if needed.


DATE: 2025-09-25
AREA: Seeds policy (cores)
DECISION: ≥ 10 normal + ≥ 3 exception rows per core; normal rows match real data; exceptions are insert-only (no intended UNIQUE/FK failures).
RATIONALE: Ensures realistic demos and QA coverage without breaking loads; negative cases will be shown via separate demos/QA, not failing seeds.


DATE: 2025-09-25
AREA: Phone format
DECISION: When present, mobile numbers use PH 11-digit 09xxxxxxxxx; enforced by QA regex checks (not DB constraint).
RATIONALE: Keeps DB flexible while letting QA flag non-compliant inputs.


DATE: 2025-09-25
AREA: Vehicles “available” in normal seeds
DECISION: All normal vehicle seeds default to vehicle_status='available'.
RATIONALE: Simplifies early scheduling demos; exception seeds cover maintenance/inactive.


DATE: 2025-09-25
AREA: Validation harness (Phase B)
DECISION: qa/validation_queries.sql implements Gate A (counts), Gate B (audit trio), Gate C (domain: non-negatives, email '@', phone regex, reserved≤on_hand), Gate D (surface exceptions).
RATIONALE: Repeatable acceptance for cores on any clean DB; catches regressions quickly.


DATE: 2025-09-28
AREA: Phase C scope & sequence
DECISION: Implement T1 (Pick Ticket) and T2 (Allocate & Pick) first; freeze columns/enums; then proceed to Java Swing bootstrap; T3–T5 follow.
RATIONALE: T2 contains the hardest rule (reserved vs on_hand); freezing the contract early de-risks later transactions and UI; matches rubric’s “DDL → Seeds → Validation → Demo → UI” slice.


DATE: 2025-09-28
AREA: T1/T2 table design
DECISION: Add header/line pairs:

pick_ticket_hdr (FKs: customer, branch; ticket_status enum with full lifecycle values; audit)

pick_ticket_line (FK: product; requested_qty DECIMAL(12,2) > 0; UNIQUE(pick_ticket_id, product_id); audit)

picking_hdr (FKs: pick_ticket, picker; picking_status ENUM('Picking','Done','Cancelled'); UNIQUE(pick_ticket_id); audit)

picking_line (FKs: picking, product and ticket_line_id → pick_ticket_line; picked_qty > 0; UNIQUE(picking_id, ticket_line_id); audit)
Indexes on join/status columns as listed in schema TODO.
RATIONALE: Normalized header/line modeling supports joins, prevents duplicates, and enables line-level validation (picked ≤ requested).


DATE: 2025-09-28
AREA: Inventory update mechanism (T2)
DECISION: TRIGGERS on picking_line (AFTER INSERT/UPDATE/DELETE) adjust products.reserved_qty; BEFORE guards enforce availability and integrity.

AI: reserved += NEW.picked_qty

AU: reserved += (NEW - OLD)

AD: reserved -= OLD.picked_qty

BEFORE: compute available = on_hand - reserved; SIGNAL SQLSTATE '45000' if available < delta; ensure picked ≤ requested per ticket line; ensure product is active; ensure ticket_line_id matches ticket & product.
RATIONALE: Deterministic, DB-enforced rule at the exact write point; eliminates UI drift; safe under concurrency.


DATE: 2025-09-28
AREA: Status automation (T2)
DECISION: On picking_hdr INSERT, set pick_ticket_hdr.ticket_status = 'Picking'.
RATIONALE: Immediate visibility of in-flight work; consistent with status path; DB is source of truth.


DATE: 2025-09-28
AREA: T1/T2 seeds (Phase C)
DECISION:

tx-T1.sql: create 3 tickets with 2–3 lines each (active products only).

tx-T2.sql: 1 full pick + 1 partial pick; no failing inserts.
RATIONALE: Provides clean, reproducible scenarios for demos and QA invariants.


DATE: 2025-09-28
AREA: Demos (Phase C)
DECISION: Single script demo-T1-T2.sql for happy path + inline optional over-pick block (ROLLBACK) proving the guard (error SQLSTATE '45000').
RATIONALE: Keeps demo focused and reproducible; shows both success and protection without breaking data.


DATE: 2025-09-28
AREA: QA extensions (Phase C)
DECISION: Extend qa/validation_queries.sql to include T1/T2 gates:

Row counts for all 4 T1/T2 tables; orphans=0

Δreserved = SUM(picked) per product; reserved ≤ on_hand

SUM(picked) ≤ requested per ticket_line_id

Anti-join to prove picked SKU exists on ticket

EXPLAIN shows index usage on joins
RATIONALE: Objective acceptance of inventory math, integrity, and performance hints.


DATE: 2025-09-28
AREA: Documentation & freeze (Phase C)
DECISION: Maintain /docs/runbook.md (run order + outcomes), /docs/decisions.md (this file), update /docs/erd.png to include T1/T2 relations, and tag v0.3-phaseC-green when QA passes.
RATIONALE: Reproducibility for graders; single source of truth for schema; stable contract for Swing integration.


DATE: 2025-09-28
AREA: Java sequence (heads-up for Phase D)
DECISION: After Phase C is green, bootstrap Java Swing (config, DAO, UI shells for T1/T2) using the frozen contract; no hardcoded creds; show DB errors verbatim.
RATIONALE: Early, live integration catches mismatches fast and sets the stage for T3–T5 with minimal rework.


DATE: 2025-11-01
AREA: Phase E preflight (ID + branching)
DECISION: Maintain `/docs/seed-id-map.md` as canonical ID register before coding; each Phase E seed PR must update it. Work occurs on feature branches named `feat/phase-e-<scope>` (T3, T4, infra, T5) merged into `phase-e/bootstrap-java` after tests/QA pass and peer review.
RATIONALE: Avoids mismatched foreign keys across seeds/demos/tests and keeps concurrent work isolated with traceable reviews.



DATE: 2025-11-06
AREA: Phase E inventory safeguards
DECISION: Keep `packed_qty <= picked_qty` enforced in PackService (service validation + QA query) rather than cross-table CHECK constraints.
RATIONALE: MySQL 8 still lacks relational CHECK support; centralising the rule in code keeps behaviour portable and testable.
IMPACTS: PackServiceImpl.validatePackLine(), qa/validate.sql (packed_vs_picked) update.

DATE: 2025-11-06
AREA: Inventory logging + audit trail
DECISION: Route all reserve/close deltas through InventoryHelper with pessimistic locking; write inventory_txn_log entries (source_ref, created_by, created_at) only when delta != 0.
RATIONALE: Single adjustment path prevents double-counting after removing legacy triggers and satisfies audit trio requirements.
IMPACTS: PickingService.savePickedItems(), CloseServiceImpl.closeTicket(), InventoryHelper.applyDelta().

DATE: 2025-11-06
AREA: Proof of Delivery & ID coordination
DECISION: Align dispatch_hdr/close_hdr pod_ref + pod_ts fields and source all canonical IDs from `/docs/seed-id-map.md`; seeds reuse dispatch PoD data during close.
RATIONALE: Ensures UI/QA flows reference the same PoD metadata and prevents divergent seed identifiers across T3T5.
IMPACTS: db/ddl/phaseE/dispatch.sql, db/ddl/phaseE/close.sql, db/seed/tx-T5.sql, docs/seed-id-map.md.


DATE: 2025-11-10
AREA: Phase E dispatch guards
DECISION: Enforce dispatch business rules in the `DispatchService` layer. Key rules include: only sealed boxes can be dispatched, a box cannot be loaded onto multiple manifests, and vehicle status/capacity must be respected.
RATIONALE: Centralizing these rules in the service layer ensures consistent application across the UI and any future API. This is safer than relying on UI-only validation and more flexible than complex database triggers.
IMPACTS: `DispatchServiceImpl.java`, `qa/validation_queries.sql`.

DATE: 2025-11-10
AREA: Phase E close reconciliation
DECISION: The `CloseService` must enforce the reconciliation rule `requested_qty = delivered_qty + short_qty` for every line in a ticket before it can be closed. A ticket closure will be rejected if the numbers do not add up.
RATIONALE: This is the core integrity rule for the entire pick-and-pack lifecycle. Enforcing it in the service layer makes it a non-negotiable invariant, preventing data corruption and ensuring accurate inventory accounting.
IMPACTS: `CloseServiceImpl.java`, `PhaseEServiceTestRunner.java`.

DATE: 2025-11-10
AREA: Phase E QA automation
DECISION: Consolidate all validation SQL queries into a single, executable PowerShell script at `scripts/qa/run-validation.ps1`.
RATIONALE: This provides a "one-click" method for any team member to run a full database integrity check after making changes, seeding data, or running demos. It standardizes the QA process and makes it easily repeatable.
IMPACTS: `scripts/qa/run-validation.ps1`, `app/README-APP.md`.

DATE: 2025-11-10
AREA: Phase E demo scripting
DECISION: All demo scripts (e.g., `demo-full-flow.sql`) must be runnable from top to bottom without manual intervention. They will use transactions and intentional rollbacks to demonstrate exception cases safely.
RATIONALE: A fully automated script ensures that the demo is reliable, repeatable, and does not leave the database in an inconsistent state. This is critical for defense and for onboarding new team members.
IMPACTS: `scripts/demo/demo-full-flow.sql`, `scripts/demo/demo-T1-to-T4.sql`.