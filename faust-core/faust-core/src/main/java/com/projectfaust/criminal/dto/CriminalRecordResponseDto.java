package com.projectfaust.criminal.dto;

import com.projectfaust.criminal.CriminalRecordStatus;
import com.projectfaust.criminal.CriminalSourceSystem;
import com.projectfaust.criminal.CriminalSubjectType;
import com.projectfaust.criminal.OffenseCategory;
import com.projectfaust.criminal.SentenceType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound representation of a {@link com.projectfaust.criminal.CriminalRecord}.
 *
 * <p>The subject's display name is resolved from the linked Person or Institution
 * entity, falling back to the raw registry name if no FK is linked. The
 * {@code recordExpired} flag is computed on read — no scheduler needed.</p>
 *
 * @author Dimitri / Project Faust
 */
public record CriminalRecordResponseDto(

        UUID publicId,

        // ── Subject ───────────────────────────────────────────────────────────

        CriminalSubjectType subjectType,
        UUID subjectPersonPublicId,
        UUID subjectInstitutionPublicId,
        String subjectDisplayName,
        String subjectNameRaw,
        String subjectNationalId,

        // ── Offense ───────────────────────────────────────────────────────────

        OffenseCategory offenseCategory,
        String offenseDescription,
        String statute,
        LocalDate offenseDate,

        // ── Proceedings ───────────────────────────────────────────────────────

        String caseNumber,
        String courtName,
        String countryCode,

        // ── Outcome ───────────────────────────────────────────────────────────

        CriminalRecordStatus status,
        LocalDate convictionDate,

        // ── Sentence ──────────────────────────────────────────────────────────

        SentenceType sentenceType,
        Integer sentenceLengthMonths,
        BigDecimal fineAmount,
        String currency,
        LocalDate probationUntil,
        LocalDate paroleEligibleFrom,
        LocalDate expiryDate,

        /**
         * Computed: true if expiryDate is in the past — record has been legally
         * rehabilitated regardless of the stored status value.
         */
        boolean recordExpired,

        // ── Source provenance ─────────────────────────────────────────────────

        CriminalSourceSystem sourceSystem,
        String sourceReferenceId,
        String registryUrl,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,
        Double confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,

        OffsetDateTime ingestedAt

) {}
