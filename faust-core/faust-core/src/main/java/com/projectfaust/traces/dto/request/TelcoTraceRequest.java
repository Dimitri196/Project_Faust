package com.projectfaust.traces.dto.request;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record TelcoTraceRequest(

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

        // ── Device identifiers ────────────────────────────────────────────────
        @Size(max = 15) String imsi,
        @Size(max = 15) String imei,
        @Size(max = 20) String msisdn,

        // ── Network node ──────────────────────────────────────────────────────
        @Size(max = 6)   String operatorCode,
        @Size(max = 100) String operatorName,
        @Size(max = 20)  String ratType,
        @Size(max = 50)  String cellId,
        @Size(max = 10)  String areaCode,

        // ── Cell location ─────────────────────────────────────────────────────
        Double cellLatitude,
        Double cellLongitude,
        Integer cellRadiusMeters,
        @Size(max = 300) String cellAddress,
        @Size(max = 2)   String country,
        @Size(max = 100) String city,

        // ── Signal ────────────────────────────────────────────────────────────
        Integer rssiDbm,

        // ── Event ─────────────────────────────────────────────────────────────
        @Size(max = 50) String eventType,
        boolean activeIntercept,
        Integer durationSeconds
) {}
