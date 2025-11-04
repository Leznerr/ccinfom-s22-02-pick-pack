-- Phase E Seeds: T5 Close Ticket

-- TODO[E-SEED-T5-001] Insert fully delivered close scenario.
-- Steps:
--   1) Reference dispatch_hdr/dispatch_line records.
--   2) Insert close_hdr (final_status='Delivered') and close_variance rows with short_qty=0.
-- Acceptance: CloseService sets ticket to Delivered and logs inventory deltas (reserved -, on_hand -).
-- | Links: docs/seed-id-map.md
USE ccinfom_dev;

-- ==========================================================
-- TODO[E-SEED-T5-001] Fully delivered close scenario
-- ==========================================================

INSERT INTO close_hdr (
  pick_ticket_id,
  dispatch_id,
  final_status,
  pod_ref,
  pod_ts,
  notes,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  1,                          -- Ticket 1 (gadgets)
  @dispatch_unsealed_id,                          
  'Delivered',
  'POD-2024-001',             -- TODO: Match pod_ref from dispatch_hdr
  CURRENT_TIMESTAMP,
  'Full delivery completed - all gadgets delivered',
  'CLOSE-FULL-001',
  CURRENT_TIMESTAMP,
  'seed_t5_001',
  CURRENT_TIMESTAMP,
  'seed_t5_001'
);

-- Variance for Line 1: Product 1, requested 5
INSERT INTO close_variance (
  close_id,
  ticket_line_id,
  requested_qty,
  delivered_qty,
  short_qty,
  reason,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  LAST_INSERT_ID(),
  1,  
  5.00,    
  5.00,                
  0.00,
  NULL,
  'VAR-FULL-001-1',
  CURRENT_TIMESTAMP,
  'seed_t5_001',
  CURRENT_TIMESTAMP,
  'seed_t5_001'
);

-- Variance for Line 2: Product 2, requested 10
INSERT INTO close_variance (
  close_id,
  ticket_line_id,
  requested_qty,
  delivered_qty,
  short_qty,
  reason,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  LAST_INSERT_ID(),
  2,                         
  10.00,                      
  10.00,                      
  0.00,                       
  NULL,
  'VAR-FULL-001-2',
  CURRENT_TIMESTAMP,
  'seed_t5_001',
  CURRENT_TIMESTAMP,
  'seed_t5_001'
);
-- TODO[E-SEED-T5-002] Insert short-close scenario with variance reasons.
-- Steps:
--   1) Provide close_hdr final_status='Short-Closed'.
--   2) close_variance rows with delivered < requested, short_qty recorded.
--   3) Ensure pod_ref reuses dispatch seed value.
-- Acceptance: QA reconciliation query passes; demo-full-flow.sql highlights short-close handling.
-- 
-- ==========================================================
-- TODO[E-SEED-T5-002] Short-close scenario with variances
-- ==========================================================

INSERT INTO close_hdr (
  pick_ticket_id,
  dispatch_id,
  final_status,
  pod_ref,
  pod_ts,
  notes,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  2,                          -- Ticket 2 (tools)
  @dispatch_unsealed_id,                          
  'Short-Closed',
  'POD-2024-002',             -- TODO: Match pod_ref from dispatch_hdr
  CURRENT_TIMESTAMP,
  'Partial delivery - some tools damaged in transit',
  'CLOSE-SHORT-002',
  CURRENT_TIMESTAMP,
  'seed_t5_002',
  CURRENT_TIMESTAMP,
  'seed_t5_002'
);

-- Variance for Line 3: Product 3, requested 8 (shortage)
INSERT INTO close_variance (
  close_id,
  ticket_line_id,
  requested_qty,
  delivered_qty,
  short_qty,
  reason,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  LAST_INSERT_ID(),
  3,                         
  8.00,                      
  5.00,                     
  3.00,                     
  'damaged',                  
  'VAR-SHORT-002-1',
  CURRENT_TIMESTAMP,
  'seed_t5_002',
  CURRENT_TIMESTAMP,
  'seed_t5_002'
);

-- Variance for Line 4: Product 4, requested 4 (shortage)
INSERT INTO close_variance (
  close_id,
  ticket_line_id,
  requested_qty,
  delivered_qty,
  short_qty,
  reason,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  LAST_INSERT_ID(),
  4,                          
  4.00,                       
  3.00,                       
  1.00,                      
  'not_available',            
  'VAR-SHORT-002-2',
  CURRENT_TIMESTAMP,
  'seed_t5_002',
  CURRENT_TIMESTAMP,
  'seed_t5_002'
);

-- Variance for Line 5: Product 5, requested 2 (fully delivered in mixed ticket)
INSERT INTO close_variance (
  close_id,
  ticket_line_id,
  requested_qty,
  delivered_qty,
  short_qty,
  reason,
  source_ref,
  created_at,
  created_by,
  updated_at,
  updated_by
) VALUES (
  LAST_INSERT_ID(),
  5,                          
  2.00,                       
  2.00,                       
  0.00,                       
  NULL,                      
  'VAR-SHORT-002-3',
  CURRENT_TIMESTAMP,
  'seed_t5_002',
  CURRENT_TIMESTAMP,
  'seed_t5_002'
);
