package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PersonResponse(
        UUID publicId,
        String firstName,    // Získáno z primárního záznamu v PersonName
        String lastName,     // Získáno z primárního záznamu v PersonName
        List<PersonNameDto> nameHistory, // Kompletní historie pro analytické účely
        List<PersonContactDto> contactHistory,
        List<PersonFinancialDto> financialAccounts, // 👈 NOVÉ

        String primaryEmail,
        String primaryPhone,

        String titleBefore,
        String titleAfter,
        String displayName,
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
        ClearanceLevel clearanceLevel
) {
    public record PersonNameDto(
            String firstName,
            String lastName,
            NameType type,
            boolean isPrimary,
            LocalDate validFrom,
            LocalDate validTo,
            String note
    ) {}

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
            boolean isActive,
            LocalDate validFrom,
            LocalDate validTo,
            String analyticalNote
    ) {}

    public record PersonFinancialDto(
            Long id,
            BankAccountResponse bankAccount,
            PersonAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}

    public record BankAccountResponse(
            UUID publicId,
            String iban,
            String bic,
            String bankName,
            String currency,
            boolean isMonitored,
            String analyticalNote
    ) {}
}