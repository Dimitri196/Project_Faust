package com.projectfaust.medical;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link MedicalRecord} — highest-clearance module in Project Faust.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface MedicalRecordRepository
        extends JpaRepository<MedicalRecord, Long>,
                JpaSpecificationExecutor<MedicalRecord> {

    // ── Single-record lookup ──────────────────────────────────────────────────

    Optional<MedicalRecord> findByExternalId(UUID externalId);

    Optional<MedicalRecord> findBySourceSystemAndSourceReferenceId(
            MedicalSourceSystem sourceSystem, String sourceReferenceId);

    boolean existsByExternalId(UUID externalId);

    // ── By person subject ─────────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findAllBySubjectPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.subjectPerson.externalId = :personExternalId " +
           "AND m.recordType = :recordType " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findBySubjectPersonAndType(
            @Param("personExternalId") UUID personExternalId,
            @Param("recordType") MedicalRecordType recordType);

    // ── By institution subject ────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.subjectInstitution.externalId = :institutionExternalId " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findAllBySubjectInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    // ── By record type ────────────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.recordType = :recordType " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findByRecordType(@Param("recordType") MedicalRecordType recordType);

    // ── Fitness-for-duty: active assessments ─────────────────────────────────

    /**
     * Returns fitness-for-duty records that have not expired.
     * Used to determine whether a subject currently holds a valid clearance.
     */
    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.recordType = com.projectfaust.medical.MedicalRecordType.FITNESS_FOR_DUTY " +
           "AND m.subjectPerson.externalId = :personExternalId " +
           "AND (m.expiryDate IS NULL OR m.expiryDate >= :today) " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findActiveFitnessAssessmentsByPerson(
            @Param("personExternalId") UUID personExternalId,
            @Param("today") LocalDate today);

    // ── Forensic: by ICD code ─────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.diagnosisCodeIcd LIKE :icdPrefix% " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findByIcdCodePrefix(@Param("icdPrefix") String icdCodePrefix);

    // ── By condition category ─────────────────────────────────────────────────

    @Query("SELECT m FROM MedicalRecord m " +
           "WHERE m.conditionCategory = :category " +
           "ORDER BY m.assessmentDate DESC NULLS LAST")
    List<MedicalRecord> findByConditionCategory(
            @Param("category") MedicalConditionCategory category);
}
