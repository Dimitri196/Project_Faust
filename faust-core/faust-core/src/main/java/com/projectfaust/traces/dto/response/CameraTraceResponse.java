package com.projectfaust.traces.dto.response;

import java.util.UUID;

public record CameraTraceResponse(
        UUID externalId,
        BaseTraceResponse base,

        // ── Vehicle ───────────────────────────────────────────────────────────
        UUID vehicleRecordPublicId,
        String vehicleLicensePlate,
        String vehicleMakeModel,

        // ── Camera ────────────────────────────────────────────────────────────
        String cameraId,
        String cameraOperator,

        // ── Location ──────────────────────────────────────────────────────────
        String locationName,
        String streetAddress,
        String city,
        String country,
        Double latitude,
        Double longitude,

        // ── Recognition ───────────────────────────────────────────────────────
        Double facialMatchScore,
        String anprPlateRaw,
        String anprPlateCountry,
        String directionOfTravel,
        String footageReference,
        boolean footageArchived
) {}
