package com.projectfaust.traces.dto.response;

import com.projectfaust.traces.enums.SurveillanceEventType;

import java.util.List;
import java.util.UUID;

public record SurveillanceEventResponse(
        UUID externalId,
        BaseTraceResponse base,

        // ── Vehicle ───────────────────────────────────────────────────────────
        UUID vehicleRecordPublicId,
        String vehicleLicensePlate,

        // ── Event ─────────────────────────────────────────────────────────────
        SurveillanceEventType eventType,

        // ── Location ──────────────────────────────────────────────────────────
        String locationName,
        String streetAddress,
        String city,
        String country,
        Double latitude,
        Double longitude,

        // ── Narrative ─────────────────────────────────────────────────────────
        String description,
        String assetCodename,
        String operationName,

        // ── Meeting participants ───────────────────────────────────────────────
        List<ParticipantSummary> meetingParticipants,

        // ── Evidence ──────────────────────────────────────────────────────────
        boolean photoEvidence,
        boolean avRecording,
        String evidenceReference
) {
    /** Minimal projection of a meeting participant — no PII beyond public name. */
    public record ParticipantSummary(UUID personPublicId, String fullName) {}
}
