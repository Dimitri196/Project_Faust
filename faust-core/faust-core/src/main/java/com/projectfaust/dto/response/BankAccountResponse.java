package com.projectfaust.dto.response;

import java.util.UUID;

public record BankAccountResponse(
        UUID publicId,
        String iban,
        String bic,
        String bankName,
        String currency,
        boolean isMonitored,
        String analyticalNote
) {}