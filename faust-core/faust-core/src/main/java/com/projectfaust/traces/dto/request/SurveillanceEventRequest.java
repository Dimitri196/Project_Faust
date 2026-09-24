package com.projectfaust.traces.dto.request;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.SurveillanceEventType;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record SurveillanceEventRequest(

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

        // ── Vehicle ───────────────────────────────────────────────────────────
        UUID vehicleRecordPublicId,

        // ── Event ─────────────────────────────────────────────────────────────
        @NotNull(message = "Event type is required.")
        SurveillanceEventType eventType,

        // ── Location ──────────────────────────────────────────────────────────
        @Size(max = 300) String locationName,
        @Size(max = 500) String streetAddress,
        @Size(max = 100) String city,
        @Size(max = 2)   String country,
        Double latitude,
        Double longitude,

        // ── Narrative ─────────────────────────────────────────────────────────
        String description,

        /** Operative codename only — never a real name. */
        @Size(max = 100) String assetCodename,

        @Size(max = 200) String operationName,

        // ── Meeting participants ───────────────────────────────────────────────
        /** External IDs of other persons present at this event. */
        Set<UUID> meetingParticipantPublicIds,

        // ── Evidence ──────────────────────────────────────────────────────────
        boolean photoEvidence,
        boolean avRecording,

        /** Reference to external evidence store — never raw files here. */
        @Size(max = 300) String evidenceReference
) {}
