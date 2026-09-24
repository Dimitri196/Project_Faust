package com.projectfaust.person.document.dto;


import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.DocumentType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record PersonDocumentRequest(

        @NotNull(message = "Person public ID is required.")
        UUID personPublicId,

        @NotNull(message = "Document type is required.")
        DocumentType documentType,

        @NotBlank(message = "Document number is required.")
        @Size(max = 100, message = "Document number must not exceed 100 characters.")
        String documentNumberRaw,

        @Size(max = 10, message = "Issuing state must be ISO 3166-1 alpha-2.")
        String issuingState,

        @Size(max = 300, message = "Issuing authority must not exceed 300 characters.")
        String issuingAuthority,

        LocalDate validFrom,
        LocalDate validTo,

        boolean active,
        boolean authentic,

        @Size(max = 200, message = "MRZ line must not exceed 200 characters.")
        String mrzLine,

        @NotNull(message = "Verification status is required.")
        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double confidenceScore,

        @NotNull(message = "Clearance level is required.")
        ClearanceLevel clearanceLevel,

        @Size(max = 2000)
        String analyticalNote
) {}
