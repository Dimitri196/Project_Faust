package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.entity.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContactOperationsResponse(
        UUID publicId, // externalId z entity PersonContact
        UUID personPublicId, // externalId z entity Person
        String subjectDisplayName, // getFullName() z Person pro kontext v UI
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
        String analyticalNote,
        LocalDateTime createdAt
) {}