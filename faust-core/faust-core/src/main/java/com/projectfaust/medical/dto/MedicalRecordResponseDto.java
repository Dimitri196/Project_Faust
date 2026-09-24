package com.projectfaust.medical.dto;

import com.projectfaust.medical.MedicalConditionCategory;
import com.projectfaust.medical.MedicalRecordType;
import com.projectfaust.medical.MedicalSourceSystem;
import com.projectfaust.medical.MedicalSubjectType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO for {@link com.projectfaust.medical.MedicalRecord}.
 *
 * <p>Resolved fields:</p>
 * <ul>
 *   <li>{@code subjectDisplayName} — resolved from person/institution or falls back to {@code subjectNameRaw}</li>
 *   <li>{@code subjectPersonPublicId} / {@code subjectInstitutionPublicId} — set if FK was resolved</li>
 *   <li>{@code assessmentExpired} — {@code true} if {@code expiryDate} is in the past</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
public record MedicalRecordResponseDto(

        UUID publicId,

        // ── Subject ───────────────────────────────────────────────────────────

        MedicalSubjectType subjectType,
        String subjectDisplayName,
        UUID subjectPersonPublicId,
        UUID subjectInstitutionPublicId,

        // ── Record type ───────────────────────────────────────────────────────

        MedicalRecordType recordType,

        // ── Dates ─────────────────────────────────────────────────────────────

        LocalDate assessmentDate,
        LocalDate reportDate,
        LocalDate expiryDate,

        /** {@code true} if {@code expiryDate} is non-null and in the past. */
        boolean assessmentExpired,

        // ── Clinical cluster ──────────────────────────────────────────────────

        MedicalConditionCategory conditionCategory,
        String diagnosisRaw,
        String diagnosisCodeIcd,
        String treatmentSummary,
        String medications,
        Boolean fitnessForDuty,
        Integer disabilityPercentage,

        // ── Forensic cluster ──────────────────────────────────────────────────

        String examiningPhysician,
        String forensicFindings,
        String toxicologyResults,
        String causeOfDeath,
        String injuryDescription,

        // ── Issuing authority ─────────────────────────────────────────────────

        String issuingAuthority,
        String facilityName,
        String countryCode,

        // ── Source / provenance ───────────────────────────────────────────────

        MedicalSourceSystem sourceSystem,
        String sourceReferenceId,
        String documentUrl,

        // ── Intelligence metadata ─────────────────────────────────────────────

        VerificationStatus verificationStatus,
        BigDecimal confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,
        LocalDateTime ingestedAt
) {}
