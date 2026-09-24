package com.projectfaust.person.dto;

import com.projectfaust.financial.dto.BankAccountRequest;
import com.projectfaust.shared.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating or updating a person's profile within Project Faust.
 *
 * <p>Covers the full HUMINT subject profile in a single request — primary name,
 * additional aliases, contact vectors, and financial account relationships.
 * All mandatory fields carry Bean Validation constraints; the controller must
 * annotate the parameter with {@code @Valid} for cascaded validation to fire.</p>
 *
 * <p><b>Name management:</b> {@code firstName} and {@code lastName} become the
 * primary {@link com.projectfaust.person.PersonName} record with {@code primaryNameType}.
 * Additional aliases, cover names, and historical names are supplied via
 * {@code additionalNames}.</p>
 *
 * <p><b>Financial accounts:</b> {@link FinancialRelationRequest} supports both
 * attaching an existing global account (by {@code bankAccountExternalId}) and
 * creating a new one (via {@code newBankAccountData}). Uses the shared
 * {@link BankAccountRequest} from the {@code financial} module — not duplicated here.</p>
 *
 * @author Dimitri / Project Faust
 */
public record PersonRequest(

        @NotBlank(message = "Primary first name is required.")
        @Size(max = 255, message = "First name must not exceed 255 characters.")
        String firstName,

        @NotBlank(message = "Primary last name is required.")
        @Size(max = 255, message = "Last name must not exceed 255 characters.")
        String lastName,

        @NotNull(message = "Primary name type classification is required.")
        NameType primaryNameType,

        List<@Valid AdditionalNameRequest> additionalNames,

        @Valid List<ContactRequest> contacts,

        @Valid List<FinancialRelationRequest> financialAccounts,

        @Size(max = 50, message = "Title before name must not exceed 50 characters.")
        String titleBefore,

        @Size(max = 50, message = "Title after name must not exceed 50 characters.")
        String titleAfter,

        EducationLevel educationLevel,
        String fieldOfStudy,

        @Size(max = 5000, message = "Biography must not exceed 5000 characters.")
        String biography,

        String photoUrl,
        String politicalAffiliation,

        LocalDate birthDate,
        Gender gender,

        @Size(max = 100, message = "Nationality must not exceed 100 characters.")
        String nationality,

        String placeOfBirth,
        LocalDate deathDate,
        ClearanceLevel clearanceLevel,

        VerificationStatus verificationStatus

) {

    /**
     * Request for an additional name record — alias, cover name, maiden name, etc.
     *
     * @param firstName  first name component of the alias.
     * @param lastName   last name component of the alias.
     * @param type       classification of the name (ALIAS, COVER, MAIDEN, etc.).
     * @param validFrom  the date from which this name was in use; null if unknown.
     * @param validTo    the date until which this name was in use; null if still active.
     * @param note       analyst note providing context for this alias.
     */
    public record AdditionalNameRequest(
            @NotBlank(message = "Alias first name is required.") String firstName,
            @NotBlank(message = "Alias last name is required.") String lastName,
            @NotNull(message = "Name type is required.") NameType type,
            LocalDate validFrom,
            LocalDate validTo,
            String note
    ) {}

    /**
     * Request for a single contact vector — phone number, email, social media handle,
     * encrypted channel identifier, etc.
     *
     * <p>Includes SIGINT-relevant metadata: operator name, IMEI, confidence score,
     * and clearance level for access gating.</p>
     *
     * @param contactType          the type of contact channel.
     * @param contactValueRaw      the raw contact value as provided.
     * @param operatorName         the telecoms operator or service provider.
     * @param imei                 the IMEI of the device associated with this contact.
     * @param verificationStatus   evidentiary provenance of this contact record.
     * @param confidenceScore      analyst confidence in this contact (0.0–1.0).
     * @param clearanceLevel       minimum clearance required to view this contact.
     * @param active               whether this contact is currently active.
     * @param validFrom            date from which this contact was valid.
     * @param validTo              date until which this contact was valid.
     * @param analyticalNote       free-text analyst context note.
     */
    public record ContactRequest(
            @NotNull(message = "Contact type is required.") ContactType contactType,
            @NotBlank(message = "Contact value is required.") String contactValueRaw,
            String operatorName,
            String imei,
            @NotNull(message = "Verification status is required.") VerificationStatus verificationStatus,
            Double confidenceScore,
            @NotNull(message = "Clearance level is required.") ClearanceLevel clearanceLevel,
            boolean active,
            LocalDate validFrom,
            LocalDate validTo,
            String analyticalNote
    ) {}

    /**
     * Request for a financial account relationship.
     *
     * <p>Supports two modes:</p>
     * <ul>
     *   <li>Attach an existing global account by {@code bankAccountExternalId}.</li>
     *   <li>Create a new account via {@code newBankAccountData} using the shared
     *       {@link BankAccountRequest} from the {@code financial} module.</li>
     * </ul>
     *
     * @param bankAccountExternalId public UUID of an existing account to attach.
     * @param newBankAccountData    data for creating a new account if none exists.
     * @param roleType              the person's role with respect to this account.
     * @param validFrom             date from which this relationship is valid.
     * @param validTo               date until which this relationship is valid.
     * @param active                whether this financial relationship is currently active.
     */
    public record FinancialRelationRequest(
            UUID bankAccountExternalId,
            @Valid BankAccountRequest newBankAccountData,
            @NotNull(message = "Account role type is required.") PersonAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}
}
