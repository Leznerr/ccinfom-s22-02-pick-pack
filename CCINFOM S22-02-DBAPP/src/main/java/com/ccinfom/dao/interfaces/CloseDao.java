package com.ccinfom.dao.interfaces;

import com.ccinfom.model.close.CloseHeader;
import com.ccinfom.model.close.CloseVariance;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for {@code close_hdr}/{@code close_variance}.
 */
public interface CloseDao {

    long insertCloseHeader(CloseHeader header, Connection conn) throws SQLException;

    void insertCloseVariances(long closeId, List<CloseVariance> variances, Connection conn) throws SQLException;

    Optional<CloseHeader> findByTicketId(long pickTicketId, Connection conn) throws SQLException;

    List<CloseVariance> listVariancesByCloseId(long closeId, Connection conn) throws SQLException;
}


