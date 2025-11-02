package com.ccinfom.service;

import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import java.sql.SQLException;
import java.util.List;

// TODO[E-SVC-T5-001] Define CloseService contract for ticket closure.
// Methods:
//   - void closeTicket(CloseHeader header, List<CloseVariance> variances) throws SQLException, ValidationException
//   - CloseHeader getCloseSummary(long pickTicketId) throws SQLException
// Acceptance: CloseServiceImpl implements interface; UI/tests compile.
// 

