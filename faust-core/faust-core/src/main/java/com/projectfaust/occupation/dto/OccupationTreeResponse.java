package com.projectfaust.occupation.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.OccupationCategory;
import com.projectfaust.shared.enums.VerificationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a node in the occupational reporting hierarchy.
 *
 * <p>Used for org-chart visualisation in the frontend. The {@code subordinates}
 * list is recursive — each node contains its direct reports, enabling the full
 * reporting chain to be rendered from a single API call when eagerly loaded.</p>
 *
 * <p><b>Serialisation safety:</b> unlike {@link com.projectfaust.institution.dto.InstitutionTreeResponse},
 * this record has no bidirectional reference risk — it contains {@code subordinates}
 * (downward) and a scalar {@code reportsToPublicId} UUID (upward), never a full
 * parent object. The recursive structure terminates at leaf nodes where
 * {@code subordinates} is empty.</p>
 *
 * @param publicId             the public UUID of this position.
 * @param title                official title of the position.
 * @param code                 unique position code.
 * @param category             functional category.
 * @param requiredClearanceLevel minimum clearance required for this position.
 * @param vacant               whether the position is currently unfilled.
 * @param active               whether the position is currently in the org structure.
 * @param rank                 military or police rank associated with this position.
 * @param currentOccupantName  display name of the current holder; null if vacant.
 * @param currentOccupantId    public UUID of the current holder; null if vacant.
 * @param reportsToPublicId    public UUID of the direct superior position; null for top-level.
 * @param subordinates         direct reports; empty for leaf nodes.
 * @param verificationStatus   evidentiary provenance state of this record.
 * @author Dimitri / Project Faust
 */
public record OccupationTreeResponse(
        UUID publicId,
        String title,
        String code,
        OccupationCategory category,
        ClearanceLevel requiredClearanceLevel,
        boolean vacant,
        boolean active,
        String rank,
        String currentOccupantName,
        UUID currentOccupantId,
        UUID reportsToPublicId,
        List<OccupationTreeResponse> subordinates,
        VerificationStatus verificationStatus
) {}
