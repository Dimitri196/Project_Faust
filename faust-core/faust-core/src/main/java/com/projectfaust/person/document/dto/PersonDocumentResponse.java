package com.projectfaust.person.document.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.DocumentType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PersonDocumentResponse(
        UUID publicId,
        UUID personPublicId,
        String subjectDisplayName,
        DocumentType documentType,
        String documentNumberRaw,
        String documentNumberNormalized,
        String issuingState,
        String issuingAuthority,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active,
        boolean authentic,
        String mrzLine,
        VerificationStatus verificationStatus,
        Double confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,
        OffsetDateTime createdAt
) {}
