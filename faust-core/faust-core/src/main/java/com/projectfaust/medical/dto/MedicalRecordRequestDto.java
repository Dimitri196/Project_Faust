package com.projectfaust.medical.dto;

import com.projectfaust.medical.MedicalConditionCategory;
import com.projectfaust.medical.MedicalRecordType;
import com.projectfaust.medical.MedicalSourceSystem;
import com.projectfaust.medical.MedicalSubjectType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating a {@link com.projectfaust.medical.MedicalRecord}.
 *
 * <p><b>Subject resolution:</b> supply exactly one of
 * {@code subjectPersonPublicId} or {@code subjectInstitutionPublicId}
 * matching {@code subjectType}. If neither is supplied, only
 * {@code subjectNameRaw} is stored (unresolved raw subject).</p>
 *
 * <p><b>Cluster convention:</b><br>
 * Clinical types → populate {@code conditionCategory}, {@code diagnosisRaw},
 * {@code diagnosisCodeIcd}, {@code treatmentSummary}, {@code medications},
 * {@code fitnessForDuty}, {@code disabilityPercentage}.<br>
 * Forensic types → populate {@code examiningPhysician}, {@code forensicFindings},
 * {@code toxicologyResults}, {@code causeOfDeath}, {@code injuryDescription}.</p>
 *
 * @author Dimitri / Project Faust
 */
public record MedicalRecordRequestDto(

        // ── Subject ───────────────────────────────────────────────────────────

        @NotNull
        MedicalSubjectType subjectType,

        UUID subjectPersonPublicId,

        UUID subjectInstitutionPublicId,

        @Size(max = 300)
        String subjectNameRaw,

        // ── Record type ───────────────────────────────────────────────────────

        @NotNull
        MedicalRecordType recordType,

        // ── Dates ─────────────────────────────────────────────────────────────

        LocalDate assessmentDate,
        LocalDate reportDate,
        LocalDate expiryDate,

        // ── Clinical cluster ──────────────────────────────────────────────────

        MedicalConditionCategory conditionCategory,
        String diagnosisRaw,

        @Size(max = 20)
        String diagnosisCodeIcd,

        String treatmentSummary,
        String medications,
        Boolean fitnessForDuty,

        @Min(0) @Max(100)
        Integer disabilityPercentage,

        // ── Forensic cluster ──────────────────────────────────────────────────

        @Size(max = 200)
        String examiningPhysician,

        String forensicFindings,
        String toxicologyResults,
        String causeOfDeath,
        String injuryDescription,

        // ── Issuing authority ─────────────────────────────────────────────────

        @Size(max = 300)
        String issuingAuthority,

        @Size(max = 300)
        String facilityName,

        @Size(min = 2, max = 2)
        String countryCode,

        // ── Source / provenance ───────────────────────────────────────────────

        @NotNull
        MedicalSourceSystem sourceSystem,

        @Size(max = 200)
        String sourceReferenceId,

        @Size(max = 2000)
        String documentUrl,

        // ── Intelligence metadata ─────────────────────────────────────────────

        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal confidenceScore,

        ClearanceLevel clearanceLevel,

        String analyticalNote
) {}
