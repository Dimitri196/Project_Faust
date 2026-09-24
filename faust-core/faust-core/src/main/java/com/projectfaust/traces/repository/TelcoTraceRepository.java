package com.projectfaust.traces.repository;

import com.projectfaust.traces.TelcoTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelcoTraceRepository extends JpaRepository<TelcoTrace, Long> {

    Optional<TelcoTrace> findByExternalId(UUID externalId);

    // ── Person-based ──────────────────────────────────────────────────────────

    @Query("SELECT t FROM TelcoTrace t WHERE t.person.externalId = :personId ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllByPersonExternalId(@Param("personId") UUID personId);

    @Query("SELECT t FROM TelcoTrace t WHERE t.person.externalId = :personId " +
           "AND t.observedAt BETWEEN :from AND :to ORDER BY t.observedAt DESC")
    List<TelcoTrace> findByPersonAndDateRange(
            @Param("personId") UUID personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Device identification ─────────────────────────────────────────────────

    @Query("SELECT t FROM TelcoTrace t WHERE t.imsi = :imsi ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllByImsi(@Param("imsi") String imsi);

    @Query("SELECT t FROM TelcoTrace t WHERE t.imei = :imei ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllByImei(@Param("imei") String imei);

    @Query("SELECT t FROM TelcoTrace t WHERE t.msisdn = :msisdn ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllByMsisdn(@Param("msisdn") String msisdn);

    // ── Burner phone detection ────────────────────────────────────────────────

    /**
     * Finds distinct IMSIs seen on the same IMEI — used to detect SIM swapping
     * behaviour (burner phone pattern: same handset, multiple SIM cards).
     */
    @Query("SELECT DISTINCT t.imsi FROM TelcoTrace t WHERE t.imei = :imei AND t.imsi IS NOT NULL")
    List<String> findDistinctImsiByImei(@Param("imei") String imei);

    /**
     * Finds distinct IMEIs associated with the same IMSI — used to detect
     * device swapping (same SIM moved between handsets).
     */
    @Query("SELECT DISTINCT t.imei FROM TelcoTrace t WHERE t.imsi = :imsi AND t.imei IS NOT NULL")
    List<String> findDistinctImeiByImsi(@Param("imsi") String imsi);

    // ── Co-presence at BTS ────────────────────────────────────────────────────

    /**
     * Returns all traces on the same cell tower within the given time window.
     * Correlating two subjects on the same BTS without camera evidence
     * can corroborate a meeting.
     */
    @Query("SELECT t FROM TelcoTrace t WHERE t.cellId = :cellId " +
           "AND t.observedAt BETWEEN :from AND :to ORDER BY t.observedAt ASC")
    List<TelcoTrace> findCoPresenceAtCell(
            @Param("cellId") String cellId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    // ── IMSI catcher / active intercepts ──────────────────────────────────────

    @Query("SELECT t FROM TelcoTrace t WHERE t.activeIntercept = true ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllActiveIntercepts();

    @Query("SELECT t FROM TelcoTrace t WHERE t.person.externalId = :personId AND t.activeIntercept = true " +
           "ORDER BY t.observedAt DESC")
    List<TelcoTrace> findActiveInterceptsByPerson(@Param("personId") UUID personId);

    // ── Analyst review ────────────────────────────────────────────────────────

    @Query("SELECT t FROM TelcoTrace t WHERE t.flagged = true ORDER BY t.observedAt DESC")
    List<TelcoTrace> findAllFlagged();
}
