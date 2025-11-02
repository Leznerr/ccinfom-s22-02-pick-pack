package com.ccinfom.service;

import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import java.sql.SQLException;
import java.util.List;

// TODO[E-SVC-T4-001] Define DispatchService contract for manifest operations.
// Methods to declare:
//   - long createDispatch(DispatchHeader header, List<DispatchLine> lines) throws SQLException, ValidationException
//   - void registerDeparture(long dispatchId, DispatchHeader updated) throws SQLException, ValidationException
//   - Optional: boolean canLoadBox(long boxId)
// Acceptance: DispatchServiceImpl implements interface; UI/test layers compile.
// 

