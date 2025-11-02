package com.ccinfom.infra;

import java.sql.Connection;
import java.sql.SQLException;

// TODO[E-INFRA-XCUT-001] Implement InventoryHelper for pessimistic locking and delta logging.
// Responsibilities:
//   - Lock products row via SELECT ... FOR UPDATE.
//   - Compute new reserved/on_hand balances based on provided deltas.
//   - Update products table and insert log into inventory_txn_log when delta ≠ 0.
//   - Support RESERVE (T2) and CLOSE (T5) operations only.
// API suggestion:
//   public void applyDelta(Connection conn, long productId, String sourceType, long sourceId,
//                          double deltaReserved, double deltaOnHand, String note, String user) throws SQLException;
// Acceptance:
//   - Used by PickingService (reserve) and CloseService (deliver/short).
//   - QA inventory reconciliation passes.
// Owner: Joshua | Links: db/ddl/phaseE/inventory_txn_log.sql
