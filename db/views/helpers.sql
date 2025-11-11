-- Phase F helpers: shared calendar + ISO-week utilities for R1-R4.
-- This file is sourced by db/schema.sql after all base tables exist.

USE ccinfom_dev;

-- Helper inline table for digits 0..9 (avoids recursive CTEs for compatibility)
DROP VIEW IF EXISTS _dim_digit;
CREATE OR REPLACE VIEW _dim_digit AS
SELECT 0 AS d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9;

-- ==========================================================
-- Date dimension (covers 2023-01-01 .. 2030-12-31)
-- ==========================================================
DROP VIEW IF EXISTS dim_date;
CREATE OR REPLACE VIEW dim_date AS
SELECT
  calendar_date,
  YEAR(calendar_date)                    AS calendar_year,
  MONTH(calendar_date)                   AS calendar_month,
  DAY(calendar_date)                     AS calendar_day,
  QUARTER(calendar_date)                 AS quarter_of_year,
  CONCAT(YEAR(calendar_date), '-', LPAD(MONTH(calendar_date),2,'0')) AS year_month_label,
  MONTHNAME(calendar_date) AS month_name_label,
  DAYNAME(calendar_date) AS day_name_label,
  DAYOFWEEK(calendar_date)               AS day_of_week_sun1,
  (DAYOFWEEK(calendar_date) + 5) % 7 + 1 AS day_of_week_mon1,
  WEEK(calendar_date, 0)                 AS week_of_year,
  MOD(YEARWEEK(calendar_date, 3), 100)   AS iso_week,
  FLOOR(YEARWEEK(calendar_date, 3) / 100) AS iso_year,
  CASE WHEN DAYOFWEEK(calendar_date) IN (1,7) THEN 1 ELSE 0 END AS is_weekend,
  DATE_SUB(calendar_date, INTERVAL ((DAYOFWEEK(calendar_date) + 5) % 7) DAY) AS week_start_monday,
  DATE_ADD(
    DATE_SUB(calendar_date, INTERVAL ((DAYOFWEEK(calendar_date) + 5) % 7) DAY),
    INTERVAL 6 DAY
  ) AS week_end_sunday,
  DATE_SUB(calendar_date, INTERVAL DAY(calendar_date) - 1 DAY) AS month_start,
  LAST_DAY(calendar_date)                AS month_end
FROM (
  SELECT DATE('2023-01-01') + INTERVAL offsets.day_offset DAY AS calendar_date
  FROM (
    SELECT
      d4.d * 1000 + d3.d * 100 + d2.d * 10 + d1.d AS day_offset
    FROM _dim_digit d1
    CROSS JOIN _dim_digit d2
    CROSS JOIN _dim_digit d3
    CROSS JOIN _dim_digit d4
  ) offsets
  WHERE DATE('2023-01-01') + INTERVAL offsets.day_offset DAY <= DATE('2030-12-31')
) span;

-- ==========================================================
-- ISO-week lookup view (helps map iso_year+iso_week -> date ranges)
-- ==========================================================
DROP VIEW IF EXISTS dim_iso_week;
CREATE OR REPLACE VIEW dim_iso_week AS
SELECT
  iso_year,
  iso_week,
  MIN(calendar_date) AS iso_week_start,
  MAX(calendar_date) AS iso_week_end
FROM dim_date
GROUP BY iso_year, iso_week;
