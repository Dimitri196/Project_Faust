package com.projectfaust.court;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for direct queries against {@link CourtRecordParty} rows.
 *
 * <p>Most party access goes through the {@link CourtRecord#parties} collection.
 * This repository serves cross-proceeding intelligence queries — e.g.
 * "find all proceedings where persons A and B were co-defendants".</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface CourtRecordPartyRepository extends JpaRepository<CourtRecordParty, Long> {

    Optional<CourtRecordParty> findByExternalId(UUID externalId);

    // ── Person-focused queries ────────────────────────────────────────────────

    @Query("SELECT p FROM CourtRecordParty p " +
           "JOIN FETCH p.courtRecord c " +
           "WHERE p.person.externalId = :personExternalId " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecordParty> findByPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    @Query("SELECT p FROM CourtRecordParty p " +
           "JOIN FETCH p.courtRecord c " +
           "WHERE p.person.externalId = :personExternalId " +
           "AND p.partyRole = :role " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecordParty> findByPersonAndRole(
            @Param("personExternalId") UUID personExternalId,
            @Param("role") PartyRole role);

    // ── Institution-focused queries ───────────────────────────────────────────

    @Query("SELECT p FROM CourtRecordParty p " +
           "JOIN FETCH p.courtRecord c " +
           "WHERE p.institution.externalId = :institutionExternalId " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecordParty> findByInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    // ── Co-party detection ────────────────────────────────────────────────────

    /**
     * Finds all court records in which two persons appeared together
     * (regardless of role) — useful for network / co-offender analysis.
     */
    @Query("SELECT DISTINCT p1.courtRecord FROM CourtRecordParty p1 " +
           "JOIN CourtRecordParty p2 ON p2.courtRecord = p1.courtRecord " +
           "WHERE p1.person.externalId = :personAExternalId " +
           "AND p2.person.externalId = :personBExternalId " +
           "ORDER BY p1.courtRecord.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findSharedProceedings(
            @Param("personAExternalId") UUID personAExternalId,
            @Param("personBExternalId") UUID personBExternalId);

    // ── By parent proceeding ──────────────────────────────────────────────────

    @Query("SELECT p FROM CourtRecordParty p " +
           "WHERE p.courtRecord.externalId = :courtRecordPublicId " +
           "ORDER BY p.partyRole ASC")
    List<CourtRecordParty> findByCourtRecordPublicId(
            @Param("courtRecordPublicId") UUID courtRecordPublicId);
}
