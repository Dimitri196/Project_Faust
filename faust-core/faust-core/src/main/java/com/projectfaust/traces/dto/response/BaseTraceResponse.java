package com.projectfaust.traces.dto.response;

import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.traces.enums.TraceConfidence;
import com.projectfaust.traces.enums.TraceSourceType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Shared projection fields present in every trace response.
 * Embedded as a nested record inside each type-specific response.
 */
public record BaseTraceResponse(
        UUID externalId,
        UUID personPublicId,
        String personFullName,
        LocalDateTime observedAt,
        TraceSourceType sourceType,
        String sourceReference,
        TraceConfidence confidence,
        VerificationStatus verificationStatus,
        boolean flagged,
        String analyticalNote,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
