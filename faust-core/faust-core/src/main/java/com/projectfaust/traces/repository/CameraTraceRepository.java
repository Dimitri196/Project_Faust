package com.projectfaust.traces.repository;

import com.projectfaust.traces.CameraTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CameraTraceRepository extends JpaRepository<CameraTrace, Long> {

    Optional<CameraTrace> findByExternalId(UUID externalId);

    // ── Person-based ──────────────────────────────────────────────────────────

    @Query("SELECT c FROM CameraTrace c WHERE c.person.externalId = :personId ORDER BY c.observedAt DESC")
    List<CameraTrace> findAllByPersonExternalId(@Param("personId") UUID personId);

    @Query("SELECT c FROM CameraTrace c WHERE c.person.externalId = :personId " +
           "AND c.observedAt BETWEEN :from AND :to ORDER BY c.observedAt DESC")
    List<CameraTrace> findByPersonAndDateRange(
            @Param("personId") UUID personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Vehicle-based ─────────────────────────────────────────────────────────

    @Query("SELECT c FROM CameraTrace c WHERE c.vehicleRecord.externalId = :vehicleId " +
           "ORDER BY c.observedAt DESC")
    List<CameraTrace> findAllByVehicleRecordExternalId(@Param("vehicleId") UUID vehicleId);

    @Query("SELECT c FROM CameraTrace c WHERE c.anprPlateRaw = :plate ORDER BY c.observedAt DESC")
    List<CameraTrace> findAllByAnprPlate(@Param("plate") String anprPlateRaw);

    // ── Camera / location ─────────────────────────────────────────────────────

    @Query("SELECT c FROM CameraTrace c WHERE c.cameraId = :cameraId ORDER BY c.observedAt DESC")
    List<CameraTrace> findAllByCameraId(@Param("cameraId") String cameraId);

    @Query("SELECT c FROM CameraTrace c WHERE c.cameraId = :cameraId " +
           "AND c.observedAt BETWEEN :from AND :to ORDER BY c.observedAt ASC")
    List<CameraTrace> findByCameraAndTimeWindow(
            @Param("cameraId") String cameraId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Co-presence detection ─────────────────────────────────────────────────

    /**
     * Returns all traces at the same camera within a time window — used to
     * detect co-presence of two subjects at the same CCTV / ANPR point.
     */
    @Query("SELECT c FROM CameraTrace c WHERE c.cameraId = :cameraId " +
           "AND c.observedAt BETWEEN :from AND :to " +
           "AND c.person IS NOT NULL ORDER BY c.observedAt ASC")
    List<CameraTrace> findCoPresenceAtCamera(
            @Param("cameraId") String cameraId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Analyst review ────────────────────────────────────────────────────────

    @Query("SELECT c FROM CameraTrace c WHERE c.flagged = true ORDER BY c.observedAt DESC")
    List<CameraTrace> findAllFlagged();
}
