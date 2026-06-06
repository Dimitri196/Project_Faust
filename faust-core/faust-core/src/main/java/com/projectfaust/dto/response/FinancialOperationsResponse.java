package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.InstitutionAccountRole;
import com.projectfaust.entity.enums.PersonAccountRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FinancialOperationsResponse(
        // Identifikátory vazby a účtu
        Long relationId,
        UUID bankAccountPublicId, // externalId z BankAccount
        String iban,
        String bic,
        String bankName,
        String currency,
        boolean isMonitored,

        // Kontext vlastníka - Osoba (vyplní se, pokud jde o personální vazbu)
        UUID personPublicId,
        String personDisplayName,
        PersonAccountRole personRoleType,

        // Kontext vlastníka - Instituce (vyplní se, pokud jde o institucionální vazbu)
        UUID institutionPublicId,
        String institutionName,
        InstitutionAccountRole institutionRoleType,

        // Společná časová platnost vazby
        LocalDate validFrom,
        LocalDate validTo,
        boolean isActive,
        String analyticalNote
) {}
