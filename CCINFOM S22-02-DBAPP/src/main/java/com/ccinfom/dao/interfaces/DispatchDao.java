package com.ccinfom.dao.interfaces;

import com.ccinfom.model.LookupValue;
import com.ccinfom.model.dispatch.DispatchHeader;
import com.ccinfom.model.dispatch.DispatchLine;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * DAO contract for T4 dispatch persistence/validations.
 */
public interface DispatchDao {

    long insertDispatchHeader(DispatchHeader header, Connection conn) throws SQLException;

    void insertDispatchLines(long dispatchId, List<DispatchLine> lines, Connection conn) throws SQLException;

    boolean isBoxLoaded(long boxId, Connection conn) throws SQLException;

    boolean isManifestNoExists(String manifestNo, Connection conn) throws SQLException;

    Optional<DispatchHeader> findById(long dispatchId, Connection conn) throws SQLException;

    int countBoxesAssignedToVehicle(long vehicleId, Connection conn) throws SQLException;

    boolean isVehicleAvailable(long vehicleId, Connection conn) throws SQLException;

    int fetchVehicleCapacity(long vehicleId, Connection conn) throws SQLException;

    void updateDispatchTimings(long dispatchId,
                               LocalDateTime departTs,
                               LocalDateTime arriveTs,
                               LocalDateTime podTs,
                               String podRef,
                               String updatedBy,
                               Connection conn) throws SQLException;

        // === NEW METHODS REQUIRED BY DispatchServiceImpl ===

    void updateDispatchArrival(long dispatchId, LocalDateTime arriveTs, String updatedBy, Connection conn) throws SQLException;

    void updateDispatchStatus(long dispatchId, String status, String updatedBy, Connection conn) throws SQLException;

    void markLinesDelivered(long dispatchId, LocalDateTime deliveredAt, String receivedBy, String updatedBy, Connection conn) throws SQLException;

    List<LookupValue> findAvailableVehicles(Connection conn) throws SQLException;

    List<LookupValue> findAvailableDrivers(Connection conn) throws SQLException;

    List<DispatchLine> findBoxesForTicket(long ticketId, Connection conn) throws SQLException;
}


