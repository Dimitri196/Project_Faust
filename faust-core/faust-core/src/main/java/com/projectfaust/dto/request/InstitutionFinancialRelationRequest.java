package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.InstitutionAccountRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record InstitutionFinancialRelationRequest(
        UUID bankAccountExternalId,

        @Valid
        BankAccountRequest newBankAccountData,

        @NotNull(message = "Role instituce k účtu musí být definována")
        InstitutionAccountRole roleType,

        LocalDate validFrom,
        LocalDate validTo,
        boolean isActive
) {}
