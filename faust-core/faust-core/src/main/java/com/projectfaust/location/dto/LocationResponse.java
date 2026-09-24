package com.projectfaust.location.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationSourceType;
import com.projectfaust.shared.enums.LocationType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO representing a geographic node in the Project Faust spatial hierarchy.
 *
 * <p>This record is the sole serialisation contract between the backend and the
 * frontend SPA. Design principles:</p>
 * <ul>
 *   <li><b>No internal IDs</b> — only {@code externalId} (UUID) is exposed; the internal
 *       {@code Long} primary key never crosses the API boundary.</li>
 *   <li><b>Flat parent representation</b> — the parent relationship is expressed as
 *       two scalar fields ({@code parentExternalId}, {@code parentName}) rather than a
 *       nested object, preventing unbounded graph serialisation.</li>
 *   <li><b>Intelligence metadata</b> — {@code verificationStatus} and {@code sourceType}
 *       are included so the frontend can render trust badges, source icons, and
 *       {@code DECEPTION_MARKER} warnings without a second API call.</li>
 *   <li><b>Timestamps</b> — {@code createdAt} and {@code updatedAt} allow the UI to
 *       display data freshness, which is critical for assessing intelligence currency.</li>
 * </ul>
 *
 * @param externalId           the public UUID of this node.
 * @param name                 the official name of the location.
 * @param type                 the {@link LocationType} category of the node.
 * @param isoCode              ISO 3166-1 alpha-2 code, or internal classified area code.
 * @param parentExternalId     the public UUID of the parent node, or {@code null} for roots.
 * @param parentName           the name of the parent node, or {@code null} for roots.
 * @param clearanceLevel       the minimum clearance required to access this node.
 * @param verificationStatus   the evidentiary provenance and trust state of this record.
 * @param sourceType           the ingestion channel that produced this record.
 * @param active               whether the node is currently operational.
 * @param latitude             WGS-84 latitude in decimal degrees.
 * @param longitude            WGS-84 longitude in decimal degrees.
 * @param hasChildren          whether this node has at least one active child node.
 * @param createdAt            the timestamp when this record was first persisted.
 * @param updatedAt            the timestamp of the most recent modification.
 * @author Dimitri / Project Faust
 */
public record LocationResponse(
        UUID externalId,
        String name,
        String localName,
        LocationType type,
        String isoCode,
        UUID parentExternalId,
        String parentName,
        ClearanceLevel clearanceLevel,
        VerificationStatus verificationStatus,
        LocationSourceType sourceType,
        boolean active,
        Double latitude,
        Double longitude,
        boolean hasChildren,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
