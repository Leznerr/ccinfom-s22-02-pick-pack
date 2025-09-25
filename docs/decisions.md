# Phase B — Decisions Log (TEAM + ELEYDO)

**DATE:** 2025-09-25  
**AREA:** Phase B scope  
**DECISION:** Cores only; **no foreign keys in cores**.  
**RATIONALE:** Establishes stable master data first and avoids premature coupling; lets us reseed/clean cores without cascade issues. Relationships are clearer and enforceable later in transactions where business context exists (pick/pack/dispatch).  
**OWNER:** TEAM

---

**DATE:** 2025-09-25  
**AREA:** Naming & audit  
**DECISION:** `snake_case`; PK = `<table>_id` (BIGINT UNSIGNED AUTO_INCREMENT); audit trio on all tables.  
**RATIONALE:** Predictable names ease SQL/Java integration; BIGINT UNSIGNED scales to real volumes; the audit trio provides operational traceability (who/when changed) for incident review and data corrections.  
**OWNER:** TEAM

---

**DATE:** 2025-09-25  
**AREA:** Customers fields  
**DECISION:** Use **company** as `customer_name`; keep `contact_person`, `phone`, `email`, `default_delivery_address` (TEXT).  
**RATIONALE:** B2B logistics operates at the account level; a default site shortens pick creation and supports dispatch coordination, POD follow-ups, and geographic reporting without needing early normalization.  
**OWNER:** ELEYDO

---

**DATE:** 2025-09-25  
**AREA:** Customers email check  
**DECISION:** `CHECK (email IS NULL OR email LIKE '%@%')`.  
**RATIONALE:** Catches obvious typos (e.g., missing “@”) while allowing phone-only or incomplete records so operations aren’t blocked; supports a progressive data-quality model and later enrichment.  
**OWNER:** ELEYDO

---

**DATE:** 2025-09-25  
**AREA:** Branches fields  
**DECISION:** `branch_name`, `address` (TEXT), `city` (NOT NULL), `contact_person`, `phone`.  
**RATIONALE:** Route planning and delivery scheduling are location-driven; `city` is mandatory for filtering/capacity planning; address/contacts enable labels and receiving coordination from day one.  
**OWNER:** ELEYDO

---

**DATE:** 2025-09-25  
**AREA:** Branch ↔ Customer link (cores)  
**DECISION:** **No core linkage**; association handled in transactions/UI cross-reference.  
**RATIONALE:** Keeps masters decoupled for onboarding/mass updates; the operational link (which customer to which site) becomes explicit on tickets/dispatches, simplifying merges or renames later.  
**OWNER:** TEAM

---

**DATE:** 2025-09-25  
**AREA:** Charset/engine  
**DECISION:** Use server defaults (no explicit ENGINE/charset/collation for Phase B).  
**RATIONALE:** Maintains compatibility with lab/team machines and dump/restore tools; avoids cross-environment drift now. We’ll set explicit collation only if multilingual or case-sensitive uniqueness needs arise later.  
**OWNER:** TEAM

---

**DATE:** 2025-09-25  
**AREA:** Seeds policy  
**DECISION:** ≥10 **normal** + ≥3 **exception** rows per core.  
**RATIONALE:** Provides enough variety for meaningful counts/filters and realistic demos; ensures validation queries can detect incomplete/edge cases while leaving normal flows statistically representative.  
**OWNER:** TEAM

---

**DATE:** 2025-09-25  
**AREA:** Customers exceptions  
**DECISION:** Allow rows with NULL `phone`/`email`/`contact_person`; placeholder phone allowed in exception seeds.  
**RATIONALE:** Mirrors real onboarding gaps so UI and SQL checks can surface follow-ups; keeps operations flowing while still making issues visible in dashboards/queries (no constraint failures).  
**OWNER:** ELEYDO

---

**DATE:** 2025-09-25  
**AREA:** Branches exceptions  
**DECISION:** Allow rows with NULL `contact_person` or NULL `phone`; generic addresses allowed for exceptions.  
**RATIONALE:** Reflects common site data gaps; validation will flag non-contactable locations, guiding ops to complete records without blocking creation of tickets referencing new branches.  
**OWNER:** ELEYDO

---

**DATE:** 2025-09-25  
**AREA:** Validation (Renzel)  
**DECISION:** `qa/validate_renzel.sql` implements Gate A (counts), Gate B (audit trio), Gate C (domain sanity), Gate D (exceptions).  
**RATIONALE:** Acts as automated acceptance: proves data is populated, audit fields work for traceability, domain rules hold, and exception records are detectable—giving reproducible evidence for QA and stakeholders.  
**OWNER:** ELEYDO
