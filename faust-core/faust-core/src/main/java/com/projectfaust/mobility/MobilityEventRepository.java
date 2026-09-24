package com.projectfaust.mobility;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MobilityEvent}.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface MobilityEventRepository
        extends JpaRepository<MobilityEvent, Long>,
                JpaSpecificationExecutor<MobilityEvent> {

    Optional<MobilityEvent> findByExternalId(UUID externalId);

    Optional<MobilityEvent> findBySourceSystemAndSourceReferenceId(
            MobilitySourceSystem sourceSystem, String sourceReferenceId);

    boolean existsByExternalId(UUID externalId);

    // ── By person ─────────────────────────────────────────────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findAllBySubjectPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "AND m.eventType = :eventType " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findBySubjectPersonAndType(
            @Param("personExternalId") UUID personExternalId,
            @Param("eventType") MobilityEventType eventType);

    /**
     * Returns all border crossing events for a person ordered by timestamp —
     * reconstructs the subject's travel timeline.
     */
    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "AND m.eventType = com.projectfaust.mobility.MobilityEventType.BORDER_CROSSING " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findBorderCrossingsByPerson(
            @Param("personExternalId") UUID personExternalId);

    // ── By vehicle ────────────────────────────────────────────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectVehicle.externalId = :vehicleExternalId " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findAllBySubjectVehicleExternalId(
            @Param("vehicleExternalId") UUID vehicleExternalId);

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectVehicle.externalId = :vehicleExternalId " +
           "AND m.eventType = :eventType " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findBySubjectVehicleAndType(
            @Param("vehicleExternalId") UUID vehicleExternalId,
            @Param("eventType") MobilityEventType eventType);

    // ── By event type ─────────────────────────────────────────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.eventType = :eventType " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findByEventType(@Param("eventType") MobilityEventType eventType);

    // ── Violation queries ─────────────────────────────────────────────────────

    /**
     * Returns all unpaid fines across any subject — used for debt-tracking
     * and financial intelligence.
     */
    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.eventType IN (" +
           "  com.projectfaust.mobility.MobilityEventType.PARKING_VIOLATION, " +
           "  com.projectfaust.mobility.MobilityEventType.TRAFFIC_VIOLATION) " +
           "AND (m.finePaid IS NULL OR m.finePaid = false) " +
           "AND m.fineAmount IS NOT NULL " +
           "ORDER BY m.fineDueDate ASC NULLS LAST")
    List<MobilityEvent> findUnpaidViolations();

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "AND m.eventType IN (" +
           "  com.projectfaust.mobility.MobilityEventType.PARKING_VIOLATION, " +
           "  com.projectfaust.mobility.MobilityEventType.TRAFFIC_VIOLATION) " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findViolationsByPerson(
            @Param("personExternalId") UUID personExternalId);

    // ── By raw licence plate (ANPR retroactive linking) ───────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE UPPER(m.licensePlateRaw) = UPPER(:plate) " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findByLicensePlateRaw(@Param("plate") String licensePlate);

    // ── By location ───────────────────────────────────────────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.locationCountryCode = :countryCode " +
           "ORDER BY m.eventTimestamp DESC")
    List<MobilityEvent> findByLocationCountryCode(@Param("countryCode") String countryCode);

    // ── Timeline window ───────────────────────────────────────────────────────

    @Query("SELECT m FROM MobilityEvent m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "AND m.eventTimestamp BETWEEN :from AND :to " +
           "ORDER BY m.eventTimestamp ASC")
    List<MobilityEvent> findTimelineForPerson(
            @Param("personExternalId") UUID personExternalId,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);
}
