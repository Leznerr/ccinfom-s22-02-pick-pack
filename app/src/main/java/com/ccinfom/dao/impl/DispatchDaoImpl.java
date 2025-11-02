package com.ccinfom.dao.impl;

import com.ccinfom.dao.interfaces.DispatchDao;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// TODO[E-DAO-T4-002] Implement DispatchDao JDBC logic.
// Why: Required to persist dispatch headers/lines and enforce uniqueness checks.
// Steps:
//   1) insertDispatchHeader: INSERT dispatch_hdr with audit trio; return generated key.
//   2) insertDispatchLines: batch insert dispatch_line respecting UNIQUE(box_id).
//   3) Provide helper queries: isBoxLoaded, isVehicleAvailable, sumLoadForManifest, etc.
//   4) Utilize passed Connection (no auto-commit) and throw SQLException on violations.
// Acceptance: DispatchServiceImpl tests (PhaseEServiceTestRunner) pass; duplicate load detection works.
