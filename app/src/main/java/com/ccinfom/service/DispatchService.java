package com.ccinfom.service;

import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import java.sql.SQLException;
import java.util.List;

/**
 * Business facade for Phase E T4 (Dispatch).
 */
public interface DispatchService {

    DispatchHeader createDispatch(DispatchHeader header, List<DispatchLine> lines)
            throws SQLException, ValidationException;

    void registerDeparture(long dispatchId, DispatchHeader updates)
            throws SQLException, ValidationException;

    boolean canLoadBox(long boxId) throws SQLException;
}

