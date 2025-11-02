package com.ccinfom.dao.interfaces;

import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

// TODO[E-DAO-T5-001] Define CloseDao for final ticket closure writes.
// Methods to declare:
//   - long insertCloseHeader(CloseHeader header, Connection conn)
//   - void insertCloseVariances(long closeId, List<CloseVariance> variances, Connection conn)
//   - CloseHeader findByTicketId(long pickTicketId, Connection conn)
// Acceptance: CloseDaoImpl implements interface; CloseService tests compile.
