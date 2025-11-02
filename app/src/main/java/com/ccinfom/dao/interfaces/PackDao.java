package com.ccinfom.dao.interfaces;

import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// TODO[E-DAO-T3-001] Define PackDao contract for T3 pack/box operations.
// Why: Services need DAO interface to insert headers/lines and query packed state within transactions.
// Methods to add:
//   - long insertBox(PackBox box, Connection conn)
//   - void insertBoxLines(List<PackBoxLine> lines, Connection conn)
//   - boolean isTicketFullyPacked(long pickTicketId, Connection conn)
//   - boolean isBoxSealed(long boxId, Connection conn)
//   - Optional getters for exception handling (e.g., findBoxLineByPickingLineId)
// Acceptance:
//   - PackDaoImpl implements the interface with JDBC and audit trio support.
//   - PackService unit tests (PhaseEServiceTestRunner) pass using this contract.
// | Links: docs/seed-id-map.md, docs/decisions.md#phase-e

