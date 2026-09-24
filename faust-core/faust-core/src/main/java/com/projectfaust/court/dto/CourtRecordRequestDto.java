package com.projectfaust.court.dto;

import com.projectfaust.court.CourtLevel;
import com.projectfaust.court.CourtOutcome;
import com.projectfaust.court.CourtPartySubjectType;
import com.projectfaust.court.CourtSourceSystem;
import com.projectfaust.court.PartyRole;
import com.projectfaust.court.ProceedingType;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating a {@link com.projectfaust.court.CourtRecord}.
 *
 * <p>Parties are supplied inline as a list of {@link PartyDto}. On create, all
 * parties are persisted as {@link com.projectfaust.court.CourtRecordParty} children.
 * On update, the service replaces the party list entirely — partial party updates
 * are not supported at this layer.</p>
 *
 * @author Dimitri / Project Faust
 */
public record CourtRecordRequestDto(

        // ── Proceeding metadata ───────────────────────────────────────────────

        @Size(max = 100) String caseNumber,

        @NotBlank(message = "courtName is required.")
        @Size(max = 300) String courtName,

        CourtLevel courtLevel,

        @NotBlank(message = "countryCode is required.")
        @Size(min = 2, max = 2) String countryCode,

        @NotNull(message = "proceedingType is required.")
        ProceedingType proceedingType,

        // ── Subject matter ────────────────────────────────────────────────────

        String subjectMatter,

        @Size(max = 500) String statute,

        // ── Timeline ──────────────────────────────────────────────────────────

        LocalDate filedDate,
        LocalDate firstHearingDate,
        LocalDate judgmentDate,
        LocalDate appealDeadline,
        LocalDate closedDate,

        // ── Outcome ───────────────────────────────────────────────────────────

        CourtOutcome outcome,

        Boolean appealed,

        String judgmentSummary,

        // ── Parties ───────────────────────────────────────────────────────────

        @Valid List<PartyDto> parties,

        // ── Criminal cross-reference ──────────────────────────────────────────

        /** Public UUID of the CriminalRecord produced by this proceeding. */
        UUID linkedCriminalRecordPublicId,

        // ── Source provenance ─────────────────────────────────────────────────

        @NotNull(message = "sourceSystem is required.")
        CourtSourceSystem sourceSystem,

        @Size(max = 200) String sourceReferenceId,
        @Size(max = 500) String registryUrl,
        String rawData,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0") Double confidenceScore,

        ClearanceLevel clearanceLevel,

        String analyticalNote

) {

    /**
     * Inline DTO for a single party in this proceeding.
     */
    public record PartyDto(

            @NotNull(message = "partySubjectType is required.")
            CourtPartySubjectType partySubjectType,

            /** Set when partySubjectType = PERSON. */
            UUID personPublicId,

            /** Set when partySubjectType = INSTITUTION. */
            UUID institutionPublicId,

            @Size(max = 300) String partyNameRaw,
            @Size(max = 50)  String partyNationalId,

            @NotNull(message = "partyRole is required.")
            PartyRole partyRole,

            String roleDetail,

            Boolean legallyRepresented,

            @Size(max = 300) String legalRepresentativeName,

            String analyticalNote

    ) {}
}
