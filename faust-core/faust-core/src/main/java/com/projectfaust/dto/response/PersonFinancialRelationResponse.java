package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.PersonAccountRole;
import java.time.LocalDate;

public record PersonFinancialRelationResponse(
        Long id,
        BankAccountResponse bankAccount, // Tady je teď správně Response objekt
        PersonAccountRole roleType,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {}