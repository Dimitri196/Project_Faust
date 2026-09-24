package com.projectfaust.location.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationSourceType;
import com.projectfaust.shared.enums.LocationType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating a new geographic node within the Project Faust spatial hierarchy.
 *
 * <p>All fields are validated before reaching the service layer — the controller must
 * annotate the parameter with {@code @Valid} for constraints to be enforced.</p>
 *
 * <p><b>Field responsibilities:</b></p>
 * <ul>
 *   <li>{@code name} and {@code type} are mandatory — a node cannot exist without them.</li>
 *   <li>{@code parentExternalId} is optional — omitting it creates a root-level node.</li>
 *   <li>{@code clearanceLevel} defaults to {@code LEVEL_1_PUBLIC} in the service if omitted;
 *       it is elevated automatically if below the parent's clearance (security inheritance).</li>
 *   <li>{@code verificationStatus} defaults to {@code PENDING_REVIEW} in the service if omitted,
 *       but an ADMIN operator may override it on creation (e.g. seeding OFFICIAL_REGISTRY data).</li>
 *   <li>{@code sourceType} defaults to {@code MANUAL} if omitted.</li>
 *   <li>Coordinates are optional but must be within valid WGS-84 ranges if supplied.</li>
 * </ul>
 *
 * @param name               the official name of the location (1–255 characters, required).
 * @param type               the {@link LocationType} category of the node (required).
 * @param isoCode            ISO 3166-1 alpha-2 code for countries, or internal classified code.
 * @param parentExternalId   the public UUID of the parent node; {@code null} for root nodes.
 * @param clearanceLevel     the security clearance required to access this node.
 * @param verificationStatus the evidentiary provenance state; defaults to {@code PENDING_REVIEW}.
 * @param sourceType         the ingestion channel that produced this record; defaults to {@code MANUAL}.
 * @param latitude           WGS-84 latitude in decimal degrees (−90 to 90).
 * @param longitude          WGS-84 longitude in decimal degrees (−180 to 180).
 * @author Dimitri / Project Faust
 */
public record LocationRequest(

        @NotBlank(message = "Location name is required.")
        @Size(min = 1, max = 255, message = "Location name must be between 1 and 255 characters.")
        String name,

        @Size(max = 255, message = "Local name must not exceed 255 characters.")
        String localName,

        @NotNull(message = "Location type is required.")
        LocationType type,

        @Size(max = 10, message = "ISO code must not exceed 10 characters.")
        String isoCode,

        UUID parentExternalId,

        ClearanceLevel clearanceLevel,

        VerificationStatus verificationStatus,

        LocationSourceType sourceType,

        @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90.")
        @DecimalMax(value = "90.0",   message = "Latitude must be <= 90.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.")
        @DecimalMax(value = "180.0",  message = "Longitude must be <= 180.")
        Double longitude

) {}