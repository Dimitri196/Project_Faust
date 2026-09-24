package com.projectfaust.traces.repository;

import com.projectfaust.traces.DigitalTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DigitalTraceRepository extends JpaRepository<DigitalTrace, Long> {

    Optional<DigitalTrace> findByExternalId(UUID externalId);

    // ── Person-based queries ──────────────────────────────────────────────────

    @Query("SELECT d FROM DigitalTrace d WHERE d.person.externalId = :personId ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAllByPersonExternalId(@Param("personId") UUID personId);

    @Query("SELECT d FROM DigitalTrace d WHERE d.person.externalId = :personId " +
           "AND d.observedAt BETWEEN :from AND :to ORDER BY d.observedAt DESC")
    List<DigitalTrace> findByPersonAndDateRange(
            @Param("personId") UUID personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Signal-based queries ──────────────────────────────────────────────────

    @Query("SELECT d FROM DigitalTrace d WHERE d.ipAddress = :ip ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAllByIpAddress(@Param("ip") String ipAddress);

    @Query("SELECT d FROM DigitalTrace d WHERE d.deviceFingerprint = :fp ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAllByDeviceFingerprint(@Param("fp") String deviceFingerprint);

    @Query("SELECT d FROM DigitalTrace d WHERE d.accountIdentifier = :accountId " +
           "ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAllByAccountIdentifier(@Param("accountId") String accountIdentifier);

    // ── Analyst review ────────────────────────────────────────────────────────

    @Query("SELECT d FROM DigitalTrace d WHERE d.flagged = true ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAllFlagged();

    @Query("SELECT d FROM DigitalTrace d WHERE d.person.externalId = :personId AND d.flagged = true " +
           "ORDER BY d.observedAt DESC")
    List<DigitalTrace> findFlaggedByPerson(@Param("personId") UUID personId);

    // ── Tor / VPN / datacenter detection ─────────────────────────────────────

    @Query("SELECT d FROM DigitalTrace d WHERE d.person.externalId = :personId " +
           "AND (d.torExitNode = true OR d.vpnDetected = true) ORDER BY d.observedAt DESC")
    List<DigitalTrace> findAnonymisedLoginsByPerson(@Param("personId") UUID personId);
}
