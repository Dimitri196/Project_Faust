package com.projectfaust.traces.dto.request;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record FinancialTraceRequest(

        UUID personPublicId,

        @NotNull(message = "Observed timestamp is required.")
        LocalDateTime observedAt,

        @NotNull(message = "Source type is required.")
        TraceSourceType sourceType,

        @Size(max = 200) String sourceReference,
        TraceConfidence confidence,
        VerificationStatus verificationStatus,
        boolean flagged,
        @Size(max = 5000) String analyticalNote,

        // ── Account linkage ───────────────────────────────────────────────────
        UUID bankAccountPublicId,                    // optional FK lookup
        @Size(max = 25)  String cardNumberMasked,    // last-4 only, never full PAN
        @Size(max = 34)  String ibanRaw,

        // ── Transaction ───────────────────────────────────────────────────────
        @DecimalMin("0.00") BigDecimal amount,
        @Size(max = 3)   String currency,
        @Size(max = 60)  String transactionType,
        @Size(max = 50)  String authorizationCode,

        // ── Merchant ──────────────────────────────────────────────────────────
        @Size(max = 300) String merchantName,
        @Size(max = 4)   String merchantCategoryCode,
        @Size(max = 50)  String terminalId,
        @Size(max = 500) String merchantAddress,
        @Size(max = 100) String merchantCity,
        @Size(max = 2)   String merchantCountry,
        Double latitude,
        Double longitude,

        // ── AML ───────────────────────────────────────────────────────────────
        boolean amlFlagged,
        @Size(max = 500) String amlReason
) {}
