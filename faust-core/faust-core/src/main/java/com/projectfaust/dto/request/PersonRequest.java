package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Request payload for creating or updating a subject's profile within Project Faust.
 * Legacy phone and email root fields are decoupled into structured contact vectors.
 */
public record PersonRequest(
        @NotBlank(message = "Primary first name is mandatory") String firstName,
        @NotBlank(message = "Primary last name is mandatory") String lastName,
        @NotNull(message = "Primary name type classification is required") NameType primaryNameType,
        List<AdditionalNameRequest> additionalNames,

        @Valid List<ContactRequest> contacts,

        @Valid List<FinancialRelationRequest> financialAccounts,

        String titleBefore,
        String titleAfter,
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
        ClearanceLevel clearanceLevel
) {
    public record AdditionalNameRequest(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull NameType type,
            LocalDate validFrom,
            LocalDate validTo,
            String note
    ) {}

    public record ContactRequest(
            @NotNull ContactType contactType,
            @NotBlank String contactValueRaw,
            String operatorName,
            String imei,
            @NotNull VerificationStatus verificationStatus,
            Double confidenceScore,
            @NotNull ClearanceLevel clearanceLevel,
            boolean isActive,
            LocalDate validFrom,
            LocalDate validTo,
            String analyticalNote
    ) {}

    public record FinancialRelationRequest(
            UUID bankAccountExternalId, // Pokud propojujeme už existující finanční uzel
            @Valid BankAccountRequest newBankAccountData, // Pokud zakládáme nový účet s osobou
            @NotNull(message = "Role osoby k účtu musí být definována") PersonAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean isActive
    ) {}

    public record BankAccountRequest(
            @NotBlank(message = "IBAN je povinný")
            @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Neplatný formát IBAN")
            String iban,
            String bic,
            @NotBlank(message = "Název bankovní instituce je povinný") String bankName,
            @NotBlank(message = "Měna je povinná") @Size(min = 3, max = 3) String currency,
            boolean isMonitored,
            String analyticalNote
    ) {}
}