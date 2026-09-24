package com.projectfaust.person.dto;

import com.projectfaust.shared.enums.ConnectionType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO representing a directed connection in the HUMINT relationship graph.
 *
 * <p>Flattens the target person's identity and current professional role into
 * scalar fields for graph visualisation without a second API call.</p>
 *
 * <p>{@code active} is derived from {@code endDate} — a connection with no end
 * date or a future end date is considered active.</p>
 *
 * @param connectionId           public UUID of the connection record.
 * @param targetId               public UUID of the target person.
 * @param targetFullName         formatted display name of the target person.
 * @param connectionType                   the nature of the relationship.
 * @param influenceScore         edge weight (0.0–1.0).
 * @param description            description of the connection context.
 * @param targetCurrentPosition  the target's current occupation title.
 * @param startDate              date from which the relationship is known.
 * @param active                 whether the connection is currently active.
 * @param verificationStatus     evidentiary provenance of this connection record.
 * @author Dimitri / Project Faust
 */
public record PersonConnectionResponse(
        UUID connectionId,
        UUID targetId,
        String targetFullName,
        ConnectionType connectionType,
        Double influenceScore,
        String description,
        String targetCurrentPosition,
        LocalDate startDate,
        boolean active,
        VerificationStatus verificationStatus
) {}