package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionAccountRole;
import com.projectfaust.entity.enums.InstitutionType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InstitutionResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        ClearanceLevel clearanceLevel,
        UUID parentId,
        boolean hasChildren,
        boolean isStateOwned,
        boolean active,
        String description,
        UUID locationId,
        String locationName,
        List<LocationResponse> fullLocationPath,
        String logoUrl,
        String websiteUrl,
        List<InstitutionFinancialDto> financialAccounts

) {

    public record InstitutionFinancialDto(
            Long id,
            PersonResponse.BankAccountResponse bankAccount,
            InstitutionAccountRole roleType,
            LocalDate validFrom,
            LocalDate validTo,
            boolean active
    ) {}
}
