package com.projectfaust.financial.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating a {@code BankAccount}.
 *
 * @param iban          IBAN — validated format, max 34 chars (ISO 13616)
 * @param bic           BIC/SWIFT code, optional, max 11 chars
 * @param bankName      Name of the holding bank
 * @param currency      ISO 4217 currency code (e.g. CZK, EUR, USD)
 * @param monitored     Whether the account is flagged for FININT monitoring
 * @param analyticalNote Free-text intelligence annotation
 */
public record BankAccountRequest(

        @NotBlank
        @Size(max = 34)
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$",
                message = "Must be a valid IBAN format")
        String iban,

        @Size(max = 11)
        String bic,

        @NotBlank
        String bankName,

        @NotBlank
        @Size(min = 3, max = 3)
        String currency,

        Boolean monitored,

        String analyticalNote
) {}
