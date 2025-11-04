# Phase E Seed ID Map & Workflow

This document tracks the canonical IDs created by the Phase E seed scripts. Update the entries as soon as your seed inserts the rows so demos, QA queries, and automated tests always reference the correct data.

> **Release blocker:** Any pull request that touches Phase E seeds, demos, or tests must update these entries in the same branch. Leaving @TODO in a completed area will block the review.

---

## How to populate the fields

1. Run your seed script and capture the generated IDs (`SELECT LAST_INSERT_ID();` or an explicit query).
2. Replace the `@TODO` placeholder with the actual value. For multiple IDs, use a comma-separated list inside brackets, for example `[101, 102]`.
3. Commit the updated map alongside the seed file so reviewers can confirm referential integrity.

---

## Ticket A - Delivered (happy path)
- pick_ticket_id: 1
- ticket_line_id(s): [1, 2]
- picking_id: 1
- picking_line_id(s): [1, 2]
- pack_box_id(s): [1, 2]
- pack_box_line_id(s): [1, 2]
- dispatch_id: 5  <!-- first committed dispatch header after rollback scaffolds -->
- manifest_no: MANIFEST-0001
- dispatch_line_id(s): [6]
- close_id: 1
- close_variance_id(s): [1, 2]

Use Ticket A for the full Delivered flow (no shortages). QA queries should show zero variances once populated.

---

## Ticket B - Short-Closed (variance scenario)
- pick_ticket_id: 2
- ticket_line_id(s): [3, 4, 5]
- picking_id: 2
- picking_line_id(s): [3, 4, 5]
- pack_box_id(s): [3]
- pack_box_line_id(s): [3, 4, 5]
- dispatch_id: 6
- manifest_no: MANIFEST-B-0002
- dispatch_line_id(s): [7]
- close_id: 2
- close_variance_id(s): [3, 4, 5]

Use Ticket B for the Short-Closed flow with variance rows where delivered_qty < requested_qty.

---

## Update responsibilities

| Member | Phase focus | Columns to fill | When to update |
|--------|-------------|-----------------|----------------|
| Member 1 | T3 Pack & Box | `pack_box_id(s)`, `pack_box_line_id(s)` | Right after `/db/seed/tx-T3.sql` runs |
| Member 2 | T4 Dispatch | `dispatch_id`, `manifest_no`, `dispatch_line_id(s)` | Right after `/db/seed/tx-T4.sql` runs |
| Member 4 | T5 Close | `close_id`, `close_variance_id(s)` | Right after `/db/seed/tx-T5.sql` runs |
| Member 3 | Infra/Test | Verify all fields are filled; sync README/demo/tests | Before merging Phase E |

---

## PR checklist

- [ ] All placeholders touched by your branch are replaced with real IDs.
- [ ] Demo scripts reference the IDs exactly as listed here.
- [ ] QA validation queries succeed using these IDs.
- [ ] Automated tests (PhaseEServiceTestRunner) use the same ticket labels.

When every entry is filled and the checklist passes, the pick -> pack -> dispatch -> close lifecycle can be reproduced reliably for Phase E.
