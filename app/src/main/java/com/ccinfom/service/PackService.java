package com.ccinfom.service;

import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Business facade for Phase E T3 (Pack & Box).
 */
public interface PackService {

    long createBox(PackBox box) throws SQLException, ValidationException;

    void addLines(long boxId, List<PackBoxLine> lines) throws SQLException, ValidationException;

    void sealBox(long boxId, String sealMethod, String user) throws SQLException, ValidationException;

    boolean isTicketPacked(long pickTicketId) throws SQLException;

    Optional<PackBox> findOpenBox(long pickingId) throws SQLException;

    List<PackBoxLine> listLinesByBoxId(long boxId) throws SQLException;

    List<Long> listPackedLineIdsByPicking(long pickingId) throws SQLException;

    Long findBoxIdByPickingLine(long pickingLineId) throws SQLException;
}

