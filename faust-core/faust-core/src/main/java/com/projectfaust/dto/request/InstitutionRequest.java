package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionAccountRole;
import com.projectfaust.entity.enums.InstitutionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InstitutionRequest(
        @NotBlank String name,
        UUID locationId,
        @NotNull HierarchicalLevel level,
        @NotNull InstitutionType type,
        ClearanceLevel clearanceLevel,
        UUID parentExternalId,
        boolean isStateOwned,
        boolean active,
        String description,
        String logoUrl,
        String websiteUrl,
        @Valid List<InstitutionFinancialRequest> financialAccounts
) {

    public record InstitutionFinancialRequest(
            UUID bankAccountExternalId,
            @Valid PersonRequest.BankAccountRequest newBankAccountData,
            @NotNull InstitutionAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean isActive
    ) {}
}
