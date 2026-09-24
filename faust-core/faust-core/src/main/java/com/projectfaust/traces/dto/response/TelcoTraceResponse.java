package com.projectfaust.traces.dto.response;

import java.util.UUID;

public record TelcoTraceResponse(
        UUID externalId,
        BaseTraceResponse base,

        // ── Device identifiers ────────────────────────────────────────────────
        String imsi,
        String imei,
        String msisdn,

        // ── Network ───────────────────────────────────────────────────────────
        String operatorCode,
        String operatorName,
        String ratType,
        String cellId,
        String areaCode,

        // ── Cell location ─────────────────────────────────────────────────────
        Double cellLatitude,
        Double cellLongitude,
        Integer cellRadiusMeters,
        String cellAddress,
        String country,
        String city,

        // ── Signal ────────────────────────────────────────────────────────────
        Integer rssiDbm,

        // ── Event ─────────────────────────────────────────────────────────────
        String eventType,
        boolean activeIntercept,
        Integer durationSeconds
) {}
