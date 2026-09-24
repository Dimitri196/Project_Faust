package com.projectfaust.person.contact.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.ContactType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating or updating a contact vector on a person record.
 *
 * @param personPublicId    public UUID of the subject person (required).
 * @param contactType       contact channel classification (required).
 * @param contactValueRaw   raw contact value as ingested (required).
 * @param operatorName      telecom operator or service provider.
 * @param imei              device IMEI for hardware fingerprinting.
 * @param verificationStatus evidentiary provenance of this contact (required).
 * @param confidenceScore   analyst confidence 0.0–1.0.
 * @param clearanceLevel    minimum clearance to view this contact (required).
 * @param active            whether this contact is currently active.
 * @param validFrom         date from which this contact was valid.
 * @param validTo           date until which this contact was valid.
 * @param analyticalNote    free-text analyst context note.
 * @author Dimitri / Project Faust
 */
public record ContactOperationsRequest(

        @NotNull(message = "Person public ID is required.")
        UUID personPublicId,

        @NotNull(message = "Contact type is required.")
        ContactType contactType,

        @NotBlank(message = "Raw contact value is required.")
        @Size(max = 500, message = "Contact value must not exceed 500 characters.")
        String contactValueRaw,

        @Size(max = 150, message = "Operator name must not exceed 150 characters.")
        String operatorName,

        @Size(max = 50, message = "IMEI must not exceed 50 characters.")
        String imei,

        @NotNull(message = "Verification status is required.")
        VerificationStatus verificationStatus,

        @DecimalMin(value = "0.0", message = "Confidence score must be >= 0.0.")
        @DecimalMax(value = "1.0", message = "Confidence score must be <= 1.0.")
        Double confidenceScore,

        @NotNull(message = "Clearance level is required.")
        ClearanceLevel clearanceLevel,

        boolean active,
        LocalDate validFrom,
        LocalDate validTo,

        @Size(max = 2000, message = "Analytical note must not exceed 2000 characters.")
        String analyticalNote

) {}