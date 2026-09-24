package com.projectfaust.criminal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link CriminalRecord} entities.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface CriminalRecordRepository
        extends JpaRepository<CriminalRecord, Long>, JpaSpecificationExecutor<CriminalRecord> {

    Optional<CriminalRecord> findByExternalId(UUID externalId);

    boolean existsByExternalId(UUID externalId);

    // ── By person subject ─────────────────────────────────────────────────────

    @Query("SELECT c FROM CriminalRecord c " +
           "JOIN FETCH c.subjectPerson p " +
           "WHERE p.externalId = :personExternalId " +
           "ORDER BY c.convictionDate DESC NULLS LAST")
    List<CriminalRecord> findAllBySubjectPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    @Query("SELECT c FROM CriminalRecord c " +
           "JOIN FETCH c.subjectPerson p " +
           "WHERE p.externalId = :personExternalId " +
           "AND c.status NOT IN ('EXPUNGED', 'PARDONED', 'ACQUITTAL', 'CASE_DROPPED') " +
           "ORDER BY c.convictionDate DESC NULLS LAST")
    List<CriminalRecord> findActiveBySubjectPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    // ── By institution subject ────────────────────────────────────────────────

    @Query("SELECT c FROM CriminalRecord c " +
           "JOIN FETCH c.subjectInstitution i " +
           "WHERE i.externalId = :institutionExternalId " +
           "ORDER BY c.convictionDate DESC NULLS LAST")
    List<CriminalRecord> findAllBySubjectInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    // ── By offense category ───────────────────────────────────────────────────

    @Query("SELECT c FROM CriminalRecord c " +
           "WHERE c.offenseCategory = :category " +
           "ORDER BY c.convictionDate DESC NULLS LAST")
    List<CriminalRecord> findByOffenseCategory(
            @Param("category") OffenseCategory category);

    // ── By status ─────────────────────────────────────────────────────────────

    @Query("SELECT c FROM CriminalRecord c " +
           "WHERE c.status = :status " +
           "ORDER BY c.ingestedAt DESC")
    List<CriminalRecord> findByStatus(@Param("status") CriminalRecordStatus status);

    // ── Deduplication ─────────────────────────────────────────────────────────

    Optional<CriminalRecord> findBySourceSystemAndSourceReferenceId(
            CriminalSourceSystem sourceSystem,
            String sourceReferenceId);

    // ── Interpol / wanted ─────────────────────────────────────────────────────

    @Query("SELECT c FROM CriminalRecord c " +
           "WHERE c.status = 'WANTED' " +
           "ORDER BY c.ingestedAt DESC")
    List<CriminalRecord> findAllWanted();
}
