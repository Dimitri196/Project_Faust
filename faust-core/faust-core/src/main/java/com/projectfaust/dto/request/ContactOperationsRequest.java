package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.entity.enums.VerificationStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ContactOperationsRequest(
        @NotNull(message = "Target subject reference is mandatory") UUID personPublicId,
        @NotNull(message = "Contact type classification is required") ContactType contactType,
        @NotBlank(message = "Raw contact value cannot be empty") String contactValueRaw,
        String operatorName,
        String imei,
        @NotNull VerificationStatus verificationStatus,
        @Min(0) @Max(1) Double confidenceScore,
        @NotNull ClearanceLevel clearanceLevel,
        boolean isActive,
        LocalDate validFrom,
        LocalDate validTo,
        String analyticalNote
) {}