package com.projectfaust.financial.dto;

import com.projectfaust.shared.enums.InstitutionAccountRole;
import com.projectfaust.shared.enums.PersonAccountRole;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Unified FININT response for both person ↔ account and institution ↔ account relations.
 *
 * <p>Person-side fields are null when this DTO represents an institutional relation,
 * and institution-side fields are null when it represents a person relation.</p>
 *
 * @author Dimitri / Project Faust
 */
public record FinancialOperationsResponse(
        // Identifikátory vazby a účtu
        Long   relationId,
        UUID   bankAccountPublicId,
        String iban,
        String bic,
        String bankName,
        String currency,
        boolean monitored,

        // Kontext vlastníka — Osoba
        UUID              personPublicId,
        String            personDisplayName,
        PersonAccountRole personRoleType,

        // Kontext vlastníka — Instituce
        UUID                   institutionPublicId,
        String                 institutionName,
        InstitutionAccountRole institutionRoleType,

        // Společná časová platnost
        LocalDate validFrom,
        LocalDate validTo,
        boolean   isActive,
        String    analyticalNote
) {}
