package com.ccinfom.dao.interfaces;

import com.ccinfom.model.pack.PackBox;
import com.ccinfom.model.pack.PackBoxLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for {@code pack_box_hdr} / {@code pack_box_line}.
 * All methods accept an existing connection so the caller controls transactions.
 */
public interface PackDao {

    long insertBox(PackBox box, Connection conn) throws SQLException;

    void insertBoxLines(long boxId, List<PackBoxLine> lines, Connection conn) throws SQLException;

    void sealBox(long boxId, String sealMethod, String updatedBy, Connection conn) throws SQLException;

    Optional<PackBox> findBoxById(long boxId, Connection conn) throws SQLException;
    Optional<PackBox> findOpenBoxByPickingId(long pickingId, Connection conn) throws SQLException;

    List<PackBoxLine> listLinesByBoxId(long boxId, Connection conn) throws SQLException;

    boolean existsPackedLineForPickingLine(long pickingLineId, Connection conn) throws SQLException;

    boolean isTicketFullyPacked(long pickTicketId, Connection conn) throws SQLException;
}


