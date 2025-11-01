-- Phase E full lifecycle demo scaffold (T1 → T5)

-- TODO[E-DEMO-T5-001] Compose full happy path across T1–T5.
-- Outline:
--   1) Run existing T1/T2 steps (refer demo-T1-to-T4.sql).
--   2) Execute Pack (T3) happy path (no inventory log message).
--   3) Execute Dispatch (T4) happy path (PoD stub recorded).
--   4) Execute Close (T5) delivered path (inventory log shows deltas).
-- Acceptance: Script runs without manual edits on clean DB; statuses progress Open → Delivered.
-- Owner: Renzel

-- TODO[E-DEMO-T5-002] Add short-close branch.
-- Steps:
--   1) After happy path, run separate transaction showing short-close (reserved release only).
--   2) Highlight QA queries to run after each branch.
-- Acceptance: Demo used in defense; README references commands.
-- Owner: Renzel
