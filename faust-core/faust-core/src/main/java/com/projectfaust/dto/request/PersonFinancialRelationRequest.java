package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.PersonAccountRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record PersonFinancialRelationRequest(
        UUID bankAccountExternalId, // Vyplní se, pokud propojujeme již existující účet v systému

        @Valid
        BankAccountRequest newBankAccountData, // Vyplní se, pokud se s osobou zakládá nový účet

        @NotNull(message = "Role osoby k účtu musí být definována")
        PersonAccountRole roleType,

        LocalDate validFrom,
        LocalDate validTo,
        boolean isActive
) {}
