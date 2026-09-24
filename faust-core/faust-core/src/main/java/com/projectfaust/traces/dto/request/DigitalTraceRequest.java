package com.projectfaust.traces.dto.request;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record DigitalTraceRequest(

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

        // ── Digital-specific ──────────────────────────────────────────────────
        @Size(max = 200) String serviceName,
        @Size(max = 500) String accountIdentifier,
        @Size(max = 100) String eventType,
        @Size(max = 45)  String ipAddress,
        @Size(max = 20)  String asn,
        @Size(max = 200) String ispName,
        @Size(max = 2)   String geoCountry,
        @Size(max = 100) String geoCity,
        String userAgent,
        @Size(max = 128) String deviceFingerprint,
        @Size(max = 100) String operatingSystem,
        boolean torExitNode,
        boolean vpnDetected,
        boolean datacenterIp
) {}
