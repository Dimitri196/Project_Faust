package com.projectfaust.financial.dto;

import java.util.UUID;

/**
 * Response DTO for a {@code BankAccount}.
 *
 * <p>Relation sets (holders, institutional owners) are NOT embedded here —
 * they are fetched via dedicated endpoints to avoid lazy-load issues and
 * to support independent pagination.</p>
 *
 * @param publicId       External UUID (never exposes internal Long id)
 * @param iban           IBAN
 * @param bic            BIC/SWIFT, nullable
 * @param bankName       Name of the holding bank
 * @param currency       ISO 4217 currency code
 * @param monitored      Whether flagged for FININT monitoring
 * @param analyticalNote Intelligence annotation, nullable
 */
public record BankAccountResponse(
        UUID    publicId,
        String  iban,
        String  bic,
        String  bankName,
        String  currency,
        boolean monitored,
        String  analyticalNote
) {}
