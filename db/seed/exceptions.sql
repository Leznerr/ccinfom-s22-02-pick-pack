-- PHASE B — EXCEPTION/EDGE SEEDS (≥3 per core) to exercise rules during demos.
-- These are intentionally odd/edge values to validate UI/DB checks later.

-- [PRODUCTS] (≥3)
-- TODO: Inactive product (active_flag = FALSE)
-- TODO: Very high/low unit_price or long name to test UI lengths
-- TODO: (Optional) Prepare a would-be duplicate SKU case (document expectation: UNIQUE violation)

-- [CUSTOMERS] (≥3)
-- TODO: Missing contact_person OR malformed phone/email (to see how UI handles)

-- [EMPLOYEES] (≥3)
-- TODO: status = 'inactive' OR unexpected role to test controlled values

-- [VEHICLES] (≥3)
-- TODO: status = 'maintenance' OR capacity = 0

-- [BRANCHES] (≥3)
-- TODO: Missing contact or phone OR generic address

-- NOTE:
-- • Exceptions need not count toward the “≥10 normal” target.
-- • Document any intended constraint errors in docs/decisions.md (what should fail vs pass).
