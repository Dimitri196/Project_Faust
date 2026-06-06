package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.InstitutionAccountRole;
import java.time.LocalDate;

public record InstitutionFinancialRelationResponse(
        Long id,
        BankAccountResponse bankAccount, // Tady rovněž Response objekt
        InstitutionAccountRole roleType,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {}