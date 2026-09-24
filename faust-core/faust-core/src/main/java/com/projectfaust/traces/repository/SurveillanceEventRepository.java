package com.projectfaust.traces.repository;

import com.projectfaust.traces.SurveillanceEvent;
import com.projectfaust.traces.enums.SurveillanceEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SurveillanceEventRepository extends JpaRepository<SurveillanceEvent, Long> {

    Optional<SurveillanceEvent> findByExternalId(UUID externalId);

    // ── Subject-based ─────────────────────────────────────────────────────────

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.person.externalId = :personId ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllByPersonExternalId(@Param("personId") UUID personId);

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.person.externalId = :personId " +
           "AND s.observedAt BETWEEN :from AND :to ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findByPersonAndDateRange(
            @Param("personId") UUID personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Meeting participant graph ─────────────────────────────────────────────

    /**
     * Returns all surveillance events where the given person appeared as a
     * meeting participant (not the primary subject).
     *
     * <p>Enables bi-directional meeting graph traversal: a subject can be the
     * primary person on one event and a participant on another.</p>
     */
    @Query("SELECT s FROM SurveillanceEvent s " +
           "JOIN s.meetingParticipants p WHERE p.externalId = :personId ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllByMeetingParticipantExternalId(@Param("personId") UUID personId);

    /**
     * Returns all surveillance events where two specific persons were observed together —
     * either as subject+participant or both as participants.
     */
    @Query("SELECT DISTINCT s FROM SurveillanceEvent s " +
           "JOIN s.meetingParticipants p1 JOIN s.meetingParticipants p2 " +
           "WHERE p1.externalId = :personA AND p2.externalId = :personB " +
           "ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findMeetingsBetween(
            @Param("personA") UUID personAId,
            @Param("personB") UUID personBId);

    // ── Event type ────────────────────────────────────────────────────────────

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.eventType = :eventType ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllByEventType(@Param("eventType") SurveillanceEventType eventType);

    // ── Operation ─────────────────────────────────────────────────────────────

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.operationName = :operation ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllByOperationName(@Param("operation") String operationName);

    // ── Counter-surveillance detection ────────────────────────────────────────

    @Query("SELECT s FROM SurveillanceEvent s " +
           "WHERE s.person.externalId = :personId " +
           "AND s.eventType = com.projectfaust.traces.enums.SurveillanceEventType.SURVEILLANCE_DETECTED " +
           "ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findCounterSurveillanceByPerson(@Param("personId") UUID personId);

    // ── Analyst review ────────────────────────────────────────────────────────

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.flagged = true ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllFlagged();

    @Query("SELECT s FROM SurveillanceEvent s WHERE s.photoEvidence = true OR s.avRecording = true " +
           "ORDER BY s.observedAt DESC")
    List<SurveillanceEvent> findAllWithPhysicalEvidence();
}
