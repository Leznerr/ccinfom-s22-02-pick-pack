package com.ccinfom.dao.interfaces;

import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// TODO[E-DAO-T4-001] Define DispatchDao operations for manifest management.
// Why: DispatchService needs DAO contract for creating headers/lines and validating uniqueness.
// Methods to declare:
//   - long insertDispatchHeader(DispatchHeader header, Connection conn)
//   - void insertDispatchLines(long dispatchId, List<DispatchLine> lines, Connection conn)
//   - boolean isBoxLoaded(long boxId, Connection conn)
//   - boolean isVehicleAvailable(long vehicleId, Connection conn)
// Acceptance: DispatchDaoImpl implements contract; PhaseEServiceTestRunner dispatch tests compile.
// 

