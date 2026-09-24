package com.projectfaust.court.dto;

import com.projectfaust.court.CourtLevel;
import com.projectfaust.court.CourtOutcome;
import com.projectfaust.court.CourtPartySubjectType;
import com.projectfaust.court.CourtSourceSystem;
import com.projectfaust.court.PartyRole;
import com.projectfaust.court.ProceedingType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound representation of a {@link com.projectfaust.court.CourtRecord}.
 *
 * <p>Parties are embedded as a list of {@link PartyResponseDto}; each party's
 * subject is resolved to a display name and public UUID so consumers do not
 * need a second lookup.</p>
 *
 * @author Dimitri / Project Faust
 */
public record CourtRecordResponseDto(

        UUID publicId,

        // ── Proceeding metadata ───────────────────────────────────────────────

        String caseNumber,
        String courtName,
        CourtLevel courtLevel,
        String countryCode,
        ProceedingType proceedingType,

        // ── Subject matter ────────────────────────────────────────────────────

        String subjectMatter,
        String statute,

        // ── Timeline ──────────────────────────────────────────────────────────

        LocalDate filedDate,
        LocalDate firstHearingDate,
        LocalDate judgmentDate,
        LocalDate appealDeadline,
        LocalDate closedDate,

        // ── Outcome ───────────────────────────────────────────────────────────

        CourtOutcome outcome,
        boolean appealed,
        String judgmentSummary,

        // ── Parties ───────────────────────────────────────────────────────────

        List<PartyResponseDto> parties,

        // ── Criminal cross-reference ──────────────────────────────────────────

        UUID linkedCriminalRecordPublicId,

        // ── Source provenance ─────────────────────────────────────────────────

        CourtSourceSystem sourceSystem,
        String sourceReferenceId,
        String registryUrl,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,
        Double confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,

        OffsetDateTime ingestedAt

) {

    /**
     * Resolved representation of a single party in this proceeding.
     */
    public record PartyResponseDto(

            UUID publicId,

            CourtPartySubjectType partySubjectType,
            UUID personPublicId,
            UUID institutionPublicId,

            /** Display name resolved from Person/Institution, or raw fallback. */
            String displayName,

            String partyNameRaw,
            String partyNationalId,

            PartyRole partyRole,
            String roleDetail,

            Boolean legallyRepresented,
            String legalRepresentativeName,

            String analyticalNote,
            OffsetDateTime ingestedAt

    ) {}
}
