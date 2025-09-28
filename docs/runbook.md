
<!-- =======================================
PHASE C — RUNBOOK (T1/T2)
Run order (clean DB):
1) db/schema.sql
2) db/seed/cores.sql
3) db/seed/exceptions.sql
4) db/seed/tx-T1.sql
5) db/seed/tx-T2.sql
6) qa/validation_queries.sql
7) scripts/demo/demo-T1-T2.sql 

Expected outcomes:
- Ticket status flips to 'Picking' when picking_hdr is created.
- products.reserved_qty increases by picked quantities; on_hand_qty unchanged.
- QA shows 0 orphans; invariants pass; EXPLAIN uses indexes.

Freeze:
- After green run, freeze column names and ENUMs for Java integration.
- Tag: v0.3-phaseC-green
======================================= -->
