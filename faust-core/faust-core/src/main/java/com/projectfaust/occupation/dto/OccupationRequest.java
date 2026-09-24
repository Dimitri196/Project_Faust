package com.projectfaust.occupation.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.OccupationCategory;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for creating or updating an occupational position in Project Faust.
 *
 * <p>All mandatory fields carry Bean Validation constraints. The controller must
 * annotate the parameter with {@code @Valid} for constraints to be enforced.</p>
 *
 * @param title                  official title of the position (required, max 255 chars).
 * @param code                   unique position code for registry mapping (required, max 50 chars).
 * @param category               functional category of the position (required).
 * @param requiredClearanceLevel minimum clearance required to hold this position (required).
 * @param institutionPublicId    public UUID of the owning institution (required).
 * @param reportsToPublicId      public UUID of the direct superior position; null for top-level.
 * @param vacant                 whether the position is currently unfilled.
 * @param active                 whether the position exists in the current org structure.
 * @param rank                   military or police rank associated with this position.
 * @param description            description of the position's mandate (max 1000 chars).
 * @param verificationStatus     evidentiary provenance; defaults to PENDING_REVIEW in service.
 * @author Dimitri / Project Faust
 */
public record OccupationRequest(

        @NotBlank(message = "Position title is required.")
        @Size(max = 255, message = "Title must not exceed 255 characters.")
        String title,

        @NotBlank(message = "Position code is required.")
        @Size(max = 50, message = "Code must not exceed 50 characters.")
        String code,

        @NotNull(message = "Occupation category is required.")
        OccupationCategory category,

        @NotNull(message = "Required clearance level is required.")
        ClearanceLevel requiredClearanceLevel,

        @NotNull(message = "Institution public ID is required.")
        UUID institutionPublicId,

        UUID reportsToPublicId,
        boolean vacant,
        boolean active,
        String rank,

        @Size(max = 1000, message = "Description must not exceed 1000 characters.")
        String description,

        VerificationStatus verificationStatus

) {}
