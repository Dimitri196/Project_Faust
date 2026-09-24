package com.projectfaust.traces.repository;

import com.projectfaust.traces.FinancialTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FinancialTraceRepository extends JpaRepository<FinancialTrace, Long> {

    Optional<FinancialTrace> findByExternalId(UUID externalId);

    // ── Person-based ──────────────────────────────────────────────────────────

    @Query("SELECT f FROM FinancialTrace f WHERE f.person.externalId = :personId ORDER BY f.observedAt DESC")
    List<FinancialTrace> findAllByPersonExternalId(@Param("personId") UUID personId);

    @Query("SELECT f FROM FinancialTrace f WHERE f.person.externalId = :personId " +
           "AND f.observedAt BETWEEN :from AND :to ORDER BY f.observedAt DESC")
    List<FinancialTrace> findByPersonAndDateRange(
            @Param("personId") UUID personId,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);

    // ── Account linkage ───────────────────────────────────────────────────────

    @Query("SELECT f FROM FinancialTrace f WHERE f.bankAccount.externalId = :accountId " +
           "ORDER BY f.observedAt DESC")
    List<FinancialTrace> findAllByBankAccountExternalId(@Param("accountId") UUID bankAccountExternalId);

    @Query("SELECT f FROM FinancialTrace f WHERE f.ibanRaw = :iban ORDER BY f.observedAt DESC")
    List<FinancialTrace> findAllByIban(@Param("iban") String iban);

    // ── AML signals ───────────────────────────────────────────────────────────

    @Query("SELECT f FROM FinancialTrace f WHERE f.amlFlagged = true ORDER BY f.observedAt DESC")
    List<FinancialTrace> findAllAmlFlagged();

    @Query("SELECT f FROM FinancialTrace f WHERE f.person.externalId = :personId AND f.amlFlagged = true " +
           "ORDER BY f.observedAt DESC")
    List<FinancialTrace> findAmlFlaggedByPerson(@Param("personId") UUID personId);

    // ── Co-presence at merchant within time window ────────────────────────────

    /**
     * Finds all financial traces at the same merchant within ±windowMinutes of the given timestamp.
     * Used to detect co-presence of two subjects at the same POS terminal.
     */
    @Query("SELECT f FROM FinancialTrace f WHERE f.merchantName = :merchant " +
           "AND f.merchantCity = :city " +
           "AND f.observedAt BETWEEN :from AND :to " +
           "ORDER BY f.observedAt ASC")
    List<FinancialTrace> findCoPresenceAtMerchant(
            @Param("merchant") String merchantName,
            @Param("city")     String merchantCity,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to);
}
