## Phase F Reporting Helpers

- `db/views/helpers.sql` now defines a reusable `dim_date` calendar (2023-01-01 → 2030-12-31) plus `dim_iso_week`. These views power ISO-week grouping for R2/R4 and are sourced automatically by `db/schema.sql` after the core tables and dispatch/close modules compile.
- Java shared components under `app/src/main/java/com/ccinfom/report/` provide:
  - `ReportFilterPanel`: year dropdown + Month/ISO-week toggle plus slots for extra filters.
  - `ReportDaoBase`: JDBC helper for executing queries with consistent date-range binding.
  - `ReportTableModel`: non-editable table model with CSV export utility.

Use `dim_date` to join on `calendar_date` (or `iso_year/iso_week`) inside report SQL so every report uses the same definition for ISO weeks, weekends, and month boundaries.
