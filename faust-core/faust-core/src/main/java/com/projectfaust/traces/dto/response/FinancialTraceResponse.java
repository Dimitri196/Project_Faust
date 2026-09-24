package com.projectfaust.traces.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record FinancialTraceResponse(
        UUID externalId,
        BaseTraceResponse base,

        // ── Account linkage ───────────────────────────────────────────────────
        UUID bankAccountPublicId,
        String cardNumberMasked,
        String ibanRaw,

        // ── Transaction ───────────────────────────────────────────────────────
        BigDecimal amount,
        String currency,
        String transactionType,
        String authorizationCode,

        // ── Merchant ──────────────────────────────────────────────────────────
        String merchantName,
        String merchantCategoryCode,
        String terminalId,
        String merchantAddress,
        String merchantCity,
        String merchantCountry,
        Double latitude,
        Double longitude,

        // ── AML ───────────────────────────────────────────────────────────────
        boolean amlFlagged,
        String amlReason
) {}
