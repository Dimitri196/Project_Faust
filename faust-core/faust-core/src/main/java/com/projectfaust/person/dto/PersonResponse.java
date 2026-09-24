package com.projectfaust.person.dto;

import com.projectfaust.appointment.dto.AppointmentResponse;
import com.projectfaust.financial.dto.BankAccountResponse;
import com.projectfaust.shared.enums.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a full person dossier within Project Faust.
 *
 * <p>Aggregates all HUMINT dimensions of a subject into a single response:</p>
 * <ul>
 *   <li><b>Identity</b> — primary name, aliases, historical names via {@code nameHistory}.</li>
 *   <li><b>Contact vectors</b> — full contact history including SIGINT metadata.</li>
 *   <li><b>Financial intelligence</b> — account relationships for FININT analysis.</li>
 *   <li><b>Current positions</b> — active appointments for immediate operational context.</li>
 *   <li><b>Provenance</b> — {@code verificationStatus} and timestamps for data quality assessment.</li>
 * </ul>
 *
 * <p>Internal DB IDs are never exposed — all identifiers are public UUIDs.</p>
 *
 * @author Dimitri / Project Faust
 */
public record PersonResponse(
        UUID publicId,

        // Primary name fields — sourced from the primary PersonName record
        String firstName,
        String lastName,
        String displayName,
        String titleBefore,
        String titleAfter,

        // Intelligence collections
        List<PersonNameDto> nameHistory,
        List<PersonContactDto> contactHistory,
        List<PersonFinancialDto> financialAccounts,

        // Currently active positions
        List<AppointmentResponse> currentPositions,

        // Contact shortcuts for dashboard display
        String primaryEmail,
        String primaryPhone,

        // Biographical fields
        Integer age,
        EducationLevel educationLevel,
        String fieldOfStudy,
        String biography,
        String photoUrl,
        String politicalAffiliation,
        LocalDate birthDate,
        Gender gender,
        String nationality,
        String placeOfBirth,
        LocalDate deathDate,
        ClearanceLevel clearanceLevel,

        // Intelligence metadata
        VerificationStatus verificationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    /**
     * DTO for a single name record in the person's identity history.
     *
     * @param firstName  first name component.
     * @param lastName   last name component.
     * @param type       classification (PRIMARY, ALIAS, COVER, MAIDEN, etc.).
     * @param primary    whether this is the current primary display name.
     * @param validFrom  date from which this name was in use.
     * @param validTo    date until which this name was in use; null if still active.
     * @param note       analyst context note for this name record.
     */
    public record PersonNameDto(
            String firstName,
            String lastName,
            NameType type,
            boolean primary,
            LocalDate validFrom,
            LocalDate validTo,
            String note
    ) {}

    /**
     * DTO for a single contact vector record.
     *
     * <p>Includes full SIGINT metadata — operator, IMEI, confidence score,
     * and clearance level — for intelligence analysis.</p>
     *
     * @param externalId              public UUID of this contact record.
     * @param contactType             the type of contact channel.
     * @param contactValueRaw         the raw contact value.
     * @param contactValueNormalized  normalised form for deduplication and search.
     * @param operatorName            telecoms operator or service provider.
     * @param imei                    IMEI of the associated device.
     * @param verificationStatus      evidentiary provenance of this contact.
     * @param confidenceScore         analyst confidence score (0.0–1.0).
     * @param clearanceLevel          minimum clearance to view this contact.
     * @param active                  whether this contact is currently active.
     * @param validFrom               date from which this contact was valid.
     * @param validTo                 date until which this contact was valid.
     * @param analyticalNote          free-text analyst context note.
     */
    public record PersonContactDto(
            UUID externalId,
            ContactType contactType,
            String contactValueRaw,
            String contactValueNormalized,
            String operatorName,
            String imei,
            VerificationStatus verificationStatus,
            Double confidenceScore,
            ClearanceLevel clearanceLevel,
            boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            String analyticalNote
    ) {}

    /**
     * DTO for a single financial account relationship in the person dossier.
     *
     * <p>Uses {@code relationId} (UUID) rather than an internal Long.
     * Bank account details use the shared {@link BankAccountResponse}
     * from the {@code financial} module.</p>
     *
     * @param relationId  public UUID of the account relationship record.
     * @param bankAccount the bank account details.
     * @param roleType    the person's role with respect to this account.
     * @param validFrom   date from which this relationship is valid.
     * @param validTo     date until which this relationship is valid.
     * @param active      whether this financial relationship is currently active.
     */
    public record PersonFinancialDto(
            UUID relationId,
            BankAccountResponse bankAccount,
            PersonAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}
}