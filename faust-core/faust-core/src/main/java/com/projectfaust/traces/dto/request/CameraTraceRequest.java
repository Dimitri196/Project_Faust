package com.projectfaust.traces.dto.request;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record CameraTraceRequest(

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

        // ── Camera ────────────────────────────────────────────────────────────
        @Size(max = 100) String cameraId,
        @Size(max = 200) String cameraOperator,

        // ── Location ──────────────────────────────────────────────────────────
        @Size(max = 300) String locationName,
        @Size(max = 500) String streetAddress,
        @Size(max = 100) String city,
        @Size(max = 2)   String country,
        Double latitude,
        Double longitude,

        // ── Recognition ───────────────────────────────────────────────────────
        @DecimalMin("0.0") @DecimalMax("1.0") Double facialMatchScore,
        @Size(max = 20)  String anprPlateRaw,
        @Size(max = 2)   String anprPlateCountry,
        @Size(max = 30)  String directionOfTravel,
        @Size(max = 200) String footageReference,
        boolean footageArchived
) {}
