package com.ccinfom.service;

import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.sql.SQLException;
import java.util.List;

// TODO[E-SVC-T3-001] Define PackService contract for T3 packing operations.
// Why: UI and tests need abstraction to create/seal boxes and enforce validations.
// Methods to declare:
//   - long createBox(PackBox box) throws SQLException, ValidationException
//   - void addLines(long boxId, List<PackBoxLine> lines) throws SQLException, ValidationException
//   - void sealBox(long boxId, String user) throws SQLException, ValidationException
//   - Optional helper: boolean isTicketPacked(long pickTicketId)
// Acceptance: PackServiceImpl implements this interface; UI + tests compile.
