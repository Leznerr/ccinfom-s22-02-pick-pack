package com.ccinfom.service;

import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import java.sql.SQLException;
import java.util.List;

/**
 * Business facade for Phase E T5 (Close Ticket).
 */
public interface CloseService {

    void closeTicket(CloseHeader header, List<CloseVariance> variances)
            throws SQLException, ValidationException;

    CloseHeader getCloseSummary(long pickTicketId) throws SQLException;

    List<CloseVariance> getVariances(long closeId) throws SQLException;
}

