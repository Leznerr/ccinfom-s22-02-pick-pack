package com.ccinfom.service;

import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.sql.SQLException;
import java.util.List;

/**
 * Business facade for Phase E T3 (Pack & Box).
 */
public interface PackService {

    long createBox(PackBox box) throws SQLException, ValidationException;

    void addLines(long boxId, List<PackBoxLine> lines) throws SQLException, ValidationException;

    void sealBox(long boxId, String sealMethod, String user) throws SQLException, ValidationException;

    boolean isTicketPacked(long pickTicketId) throws SQLException;
}

