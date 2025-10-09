/*
 * CCINFOM — Phase D
 * File: PickingDaoImpl.java
 * Purpose: JDBC implementation of PickingDao.
 *
 * TODOs:
 *  [ ] insertPickingHeader(): try insert; if duplicate (unique by ticket), fetch existing id.
 *  [ ] insertPickingLines(): batch insert with anti-dup filter.
 *
 * Definition of Done:
 *  - Idempotent behavior (re-running inserts does not duplicate rows).
 *  - Works with triggers: ticket status flips to 'Picking'.
 */
