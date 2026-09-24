package com.projectfaust.occupation.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.OccupationCategory;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO representing a single occupational position in Project Faust.
 *
 * <p>Flattens all relationships into scalar fields — institution and
 * reporting-line references are expressed as UUID + name pairs rather than
 * nested objects, preventing unbounded graph serialisation.</p>
 *
 * <p>Includes the current occupant name and UUID so the frontend dossier
 * view can display "who holds this position now?" without a second API call.</p>
 *
 * @param publicId               the public UUID of this position.
 * @param title                  official title of the position.
 * @param code                   unique position code for cross-system mapping.
 * @param category               functional category (EXECUTIVE, MILITARY, etc.).
 * @param requiredClearanceLevel minimum clearance required to hold this position.
 * @param institutionName        display name of the owning institution.
 * @param institutionPublicId    public UUID of the owning institution.
 * @param supervisorTitle        title of the direct superior position.
 * @param reportsToPublicId      public UUID of the direct superior position.
 * @param vacant                 whether the position is currently unfilled.
 * @param active                 whether the position exists in the current org structure.
 * @param rank                   military or police rank associated with this position.
 * @param description            description of the position's mandate.
 * @param currentOccupantName    display name of the person currently holding this position.
 * @param currentOccupantId      public UUID of the current occupant, or null if vacant.
 * @param verificationStatus     evidentiary provenance state of this record.
 * @param createdAt              timestamp when this record was first persisted.
 * @param updatedAt              timestamp of the most recent modification.
 * @author Dimitri / Project Faust
 */
public record OccupationResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        ClearanceLevel requiredClearanceLevel,
        String institutionName,
        UUID institutionPublicId,
        String supervisorTitle,
        UUID reportsToPublicId,
        boolean vacant,
        boolean active,
        String rank,
        String description,
        String currentOccupantName,
        UUID currentOccupantId,
        VerificationStatus verificationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}