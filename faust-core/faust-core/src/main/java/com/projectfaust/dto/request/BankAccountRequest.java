package com.projectfaust.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankAccountRequest(
        @NotBlank(message = "IBAN je povinný")
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$", message = "Neplatný formát IBAN")
        String iban,

        @Size(min = 8, max = 11, message = "BIC (SWIFT) musí mít 8 až 11 znaků")
        String bic,

        @NotBlank(message = "Název bankovní instituce je povinný")
        String bankName,

        @NotBlank(message = "Měna je povinná")
        @Size(min = 3, max = 3, message = "Měna musí být ve formátu ISO (např. CZK, EUR)")
        String currency,

        boolean isMonitored,
        String analyticalNote
) {}