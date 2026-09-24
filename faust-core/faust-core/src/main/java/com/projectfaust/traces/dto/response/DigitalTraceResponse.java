package com.projectfaust.traces.dto.response;

import java.util.UUID;

public record DigitalTraceResponse(
        UUID externalId,

        // ── Base fields (flattened for API convenience) ───────────────────────
        BaseTraceResponse base,

        // ── Digital-specific ──────────────────────────────────────────────────
        String serviceName,
        String accountIdentifier,
        String eventType,
        String ipAddress,
        String asn,
        String ispName,
        String geoCountry,
        String geoCity,
        String userAgent,
        String deviceFingerprint,
        String operatingSystem,
        boolean torExitNode,
        boolean vpnDetected,
        boolean datacenterIp
) {}
