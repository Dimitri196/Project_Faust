package com.projectfaust.criminal.dto;

import com.projectfaust.criminal.CriminalRecordStatus;
import com.projectfaust.criminal.CriminalSourceSystem;
import com.projectfaust.criminal.CriminalSubjectType;
import com.projectfaust.criminal.OffenseCategory;
import com.projectfaust.criminal.SentenceType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating a {@link com.projectfaust.criminal.CriminalRecord}.
 *
 * @author Dimitri / Project Faust
 */
public record CriminalRecordRequestDto(

        // ── Subject ───────────────────────────────────────────────────────────

        @NotNull(message = "subjectType is required.")
        CriminalSubjectType subjectType,

        /** Set when subjectType = PERSON. */
        UUID subjectPersonPublicId,

        /** Set when subjectType = INSTITUTION. */
        UUID subjectInstitutionPublicId,

        @Size(max = 300) String subjectNameRaw,
        @Size(max = 50)  String subjectNationalId,

        // ── Offense ───────────────────────────────────────────────────────────

        @NotNull(message = "offenseCategory is required.")
        OffenseCategory offenseCategory,

        String offenseDescription,

        @Size(max = 200) String statute,

        LocalDate offenseDate,

        // ── Proceedings ───────────────────────────────────────────────────────

        @Size(max = 100) String caseNumber,
        @Size(max = 300) String courtName,

        @NotBlank(message = "countryCode is required.")
        @Size(min = 2, max = 2) String countryCode,

        // ── Outcome ───────────────────────────────────────────────────────────

        @NotNull(message = "status is required.")
        CriminalRecordStatus status,

        LocalDate convictionDate,

        // ── Sentence ──────────────────────────────────────────────────────────

        SentenceType sentenceType,

        @Min(0) Integer sentenceLengthMonths,

        @DecimalMin("0.0") BigDecimal fineAmount,

        @Size(min = 3, max = 3) String currency,

        LocalDate probationUntil,
        LocalDate paroleEligibleFrom,
        LocalDate expiryDate,

        // ── Source provenance ─────────────────────────────────────────────────

        @NotNull(message = "sourceSystem is required.")
        CriminalSourceSystem sourceSystem,

        @Size(max = 200) String sourceReferenceId,
        @Size(max = 500) String registryUrl,
        String rawData,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0") Double confidenceScore,

        ClearanceLevel clearanceLevel,

        String analyticalNote

) {}
