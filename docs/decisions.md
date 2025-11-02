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
AREA: Branch â†” Customer link (cores)
DECISION: No core FK linkage; association handled in transactions and UI cross-reference.
RATIONALE: Keeps masters decoupled; operational link is explicit on tickets/dispatches; easier customer/site merges.


DATE: 2025-09-25
AREA: Charset/engine
DECISION: Use server defaults (no explicit ENGINE/collation in Phase B).
RATIONALE: Maximizes portability across lab machines and dump/restore; weâ€™ll lock collation only if needed.


DATE: 2025-09-25
AREA: Seeds policy (cores)
DECISION: â‰¥ 10 normal + â‰¥ 3 exception rows per core; normal rows match real data; exceptions are insert-only (no intended UNIQUE/FK failures).
RATIONALE: Ensures realistic demos and QA coverage without breaking loads; negative cases will be shown via separate demos/QA, not failing seeds.


DATE: 2025-09-25
AREA: Phone format
DECISION: When present, mobile numbers use PH 11-digit 09xxxxxxxxx; enforced by QA regex checks (not DB constraint).
RATIONALE: Keeps DB flexible while letting QA flag non-compliant inputs.


DATE: 2025-09-25
AREA: Vehicles â€œavailableâ€ in normal seeds
DECISION: All normal vehicle seeds default to vehicle_status='available'.
RATIONALE: Simplifies early scheduling demos; exception seeds cover maintenance/inactive.


DATE: 2025-09-25
AREA: Validation harness (Phase B)
DECISION: qa/validation_queries.sql implements Gate A (counts), Gate B (audit trio), Gate C (domain: non-negatives, email '@', phone regex, reservedâ‰¤on_hand), Gate D (surface exceptions).
RATIONALE: Repeatable acceptance for cores on any clean DB; catches regressions quickly.


DATE: 2025-09-28
AREA: Phase C scope & sequence
DECISION: Implement T1 (Pick Ticket) and T2 (Allocate & Pick) first; freeze columns/enums; then proceed to Java Swing bootstrap; T3â€“T5 follow.
RATIONALE: T2 contains the hardest rule (reserved vs on_hand); freezing the contract early de-risks later transactions and UI; matches rubricâ€™s â€œDDL â†’ Seeds â†’ Validation â†’ Demo â†’ UIâ€ slice.


DATE: 2025-09-28
AREA: T1/T2 table design
DECISION: Add header/line pairs:

pick_ticket_hdr (FKs: customer, branch; ticket_status enum with full lifecycle values; audit)

pick_ticket_line (FK: product; requested_qty DECIMAL(12,2) > 0; UNIQUE(pick_ticket_id, product_id); audit)

picking_hdr (FKs: pick_ticket, picker; picking_status ENUM('Picking','Done','Cancelled'); UNIQUE(pick_ticket_id); audit)

picking_line (FKs: picking, product and ticket_line_id â†’ pick_ticket_line; picked_qty > 0; UNIQUE(picking_id, ticket_line_id); audit)
Indexes on join/status columns as listed in schema TODO.
RATIONALE: Normalized header/line modeling supports joins, prevents duplicates, and enables line-level validation (picked â‰¤ requested).


DATE: 2025-09-28
AREA: Inventory update mechanism (T2)
DECISION: TRIGGERS on picking_line (AFTER INSERT/UPDATE/DELETE) adjust products.reserved_qty; BEFORE guards enforce availability and integrity.

AI: reserved += NEW.picked_qty

AU: reserved += (NEW - OLD)

AD: reserved -= OLD.picked_qty

BEFORE: compute available = on_hand - reserved; SIGNAL SQLSTATE '45000' if available < delta; ensure picked â‰¤ requested per ticket line; ensure product is active; ensure ticket_line_id matches ticket & product.
RATIONALE: Deterministic, DB-enforced rule at the exact write point; eliminates UI drift; safe under concurrency.


DATE: 2025-09-28
AREA: Status automation (T2)
DECISION: On picking_hdr INSERT, set pick_ticket_hdr.ticket_status = 'Picking'.
RATIONALE: Immediate visibility of in-flight work; consistent with status path; DB is source of truth.


DATE: 2025-09-28
AREA: T1/T2 seeds (Phase C)
DECISION:

tx-T1.sql: create 3 tickets with 2â€“3 lines each (active products only).

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

Î”reserved = SUM(picked) per product; reserved â‰¤ on_hand

SUM(picked) â‰¤ requested per ticket_line_id

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
RATIONALE: Early, live integration catches mismatches fast and sets the stage for T3â€“T5 with minimal rework.


DATE: 2025-11-01
AREA: Phase E preflight (ID + branching)
DECISION: Maintain `/docs/seed-id-map.md` as canonical ID register before coding; each Phaseâ€¯E seed PR must update it. Work occurs on feature branches named `feat/phase-e-<scope>` (T3, T4, infra, T5) merged into `phase-e/bootstrap-java` after tests/QA pass and peer review.
RATIONALE: Avoids mismatched foreign keys across seeds/demos/tests and keeps concurrent work isolated with traceable reviews.


// TODO[E-DOC-XCUT-001] Record Phase E decisions (MySQL CHECK policy, inventory logging, PoD alignment, status transitions).
// Why: Documentation must match final implementation to aid reviewers and defense.
// Steps:
//   1) Add entry noting packed_qty â‰¤ picked_qty enforced in PackService (MySQL limitation).
//   2) Add entry stating inventory_txn_log writes only for RESERVE (T2) and CLOSE (T5).
//   3) Clarify pod_ref/pod_ts alignment between dispatch_hdr and close_hdr, and Seed ID coordination map.
// Acceptance:
//   - docs/DECISIONS.md updated before Phase E merge.
//   - README references decisions section.
// | Links: docs/seed-id-map.md, qa/validation_queries.sql

