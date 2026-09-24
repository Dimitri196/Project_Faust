package com.projectfaust.court;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link CourtRecord} entities.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface CourtRecordRepository
        extends JpaRepository<CourtRecord, Long>, JpaSpecificationExecutor<CourtRecord> {

    Optional<CourtRecord> findByExternalId(UUID externalId);

    boolean existsByExternalId(UUID externalId);

    // ── By party — person ─────────────────────────────────────────────────────

    /**
     * Returns all proceedings in which the specified person appears in any role.
     */
    @Query("SELECT DISTINCT c FROM CourtRecord c " +
           "JOIN c.parties p " +
           "WHERE p.person.externalId = :personExternalId " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findAllByPartyPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all proceedings in which the specified person appears in a given role.
     */
    @Query("SELECT DISTINCT c FROM CourtRecord c " +
           "JOIN c.parties p " +
           "WHERE p.person.externalId = :personExternalId " +
           "AND p.partyRole = :role " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findAllByPartyPersonAndRole(
            @Param("personExternalId") UUID personExternalId,
            @Param("role") PartyRole role);

    // ── By party — institution ────────────────────────────────────────────────

    /**
     * Returns all proceedings in which the specified institution appears in any role.
     */
    @Query("SELECT DISTINCT c FROM CourtRecord c " +
           "JOIN c.parties p " +
           "WHERE p.institution.externalId = :institutionExternalId " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findAllByPartyInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    /**
     * Returns all proceedings in which the specified institution appears in a given role.
     */
    @Query("SELECT DISTINCT c FROM CourtRecord c " +
           "JOIN c.parties p " +
           "WHERE p.institution.externalId = :institutionExternalId " +
           "AND p.partyRole = :role " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findAllByPartyInstitutionAndRole(
            @Param("institutionExternalId") UUID institutionExternalId,
            @Param("role") PartyRole role);

    // ── By proceeding type & outcome ──────────────────────────────────────────

    @Query("SELECT c FROM CourtRecord c " +
           "WHERE c.proceedingType = :type " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findByProceedingType(@Param("type") ProceedingType type);

    @Query("SELECT c FROM CourtRecord c " +
           "WHERE c.outcome = :outcome " +
           "ORDER BY c.judgmentDate DESC NULLS LAST")
    List<CourtRecord> findByOutcome(@Param("outcome") CourtOutcome outcome);

    // ── Criminal cross-reference ──────────────────────────────────────────────

    @Query("SELECT c FROM CourtRecord c " +
           "WHERE c.linkedCriminalRecord.externalId = :criminalRecordPublicId")
    Optional<CourtRecord> findByLinkedCriminalRecordPublicId(
            @Param("criminalRecordPublicId") UUID criminalRecordPublicId);

    // ── Deduplication ─────────────────────────────────────────────────────────

    Optional<CourtRecord> findBySourceSystemAndSourceReferenceId(
            CourtSourceSystem sourceSystem,
            String sourceReferenceId);
}
