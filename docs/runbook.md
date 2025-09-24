<!--
PHASE B — RUNBOOK STEPS (CORES ONLY)

1) Create/refresh clean DB (e.g., pick_pack)
2) Run db/schema.sql  (cores only; no FKs; audit trio present)
3) Run db/seed/cores.sql  (≥10 normal rows per core)
4) Run db/seed/exceptions.sql  (≥3 exception rows per core)
5) Run qa/validation_queries.sql  (Expect ≥10 per core; confirm exceptions exist)
6) Screenshot COUNT(*) results and attach to PR
7) Update docs/decisions.md for any field/status choices
-->
