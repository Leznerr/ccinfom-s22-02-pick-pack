# Phase E Seed ID Map

Use this map to record the canonical identifiers referenced by Phase E seeds, demos, QA queries, and automated tests. Replace each `@TODO` as soon as the matching seed script generates the row. Keep the ticket labels stable so cross-references stay valid throughout the project.

## Ticket A
- `pick_ticket_id` → `@TODO`
- `ticket_line_id(s)` → `@TODO`
- `picking_id` → `@TODO`
- `picking_line_id(s)` → `@TODO`
- `pack_box_id(s)` → `@TODO`
- `pack_box_line_id(s)` → `@TODO`
- `dispatch_id` → `@TODO`
- `manifest_no` → `@TODO`
- `dispatch_line_id(s)` → `@TODO`
- `close_id` → `@TODO`
- `close_variance_id(s)` → `@TODO`

## Ticket B
- `pick_ticket_id` → `@TODO`
- `ticket_line_id(s)` → `@TODO`
- `picking_id` → `@TODO`
- `picking_line_id(s)` → `@TODO`
- `pack_box_id(s)` → `@TODO`
- `pack_box_line_id(s)` → `@TODO`
- `dispatch_id` → `@TODO`
- `manifest_no` → `@TODO`
- `dispatch_line_id(s)` → `@TODO`
- `close_id` → `@TODO`
- `close_variance_id(s)` → `@TODO`

Add more ticket sections if we introduce additional sample workflows.

### Update Protocol
- **Member 1 (T3 pack)** fills `pack_box_*` values after `/db/seed/tx-T3.sql` is finalized.
- **Member 2 (T4 dispatch)** populates the `dispatch_*` values after `/db/seed/tx-T4.sql`.
- **Member 4 (T5 close)** completes the `close_*` fields after `/db/seed/tx-T5.sql`.
- **Member 3 (cross-cutting)** verifies the entire table prior to merging Phase E, ensuring demos/QA/tests reference the IDs listed here.

Maintaining this map is a release blocker for Phase E: any PR that introduces new seeds, demo scripts, or tests must update the relevant entries in the same branch.
