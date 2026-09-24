package com.projectfaust.occupation.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.OccupationCategory;
import com.projectfaust.shared.enums.VerificationStatus;

import java.util.UUID;

/**
 * Response DTO representing a node in the occupational reporting chain,
 * used for breadcrumb rendering of the full chain of command.
 *
 * <p>The recursive {@code parent} field builds the full ancestor chain upward
 * from any position to the top of the reporting hierarchy. This is serialisation-safe
 * because the chain is strictly unidirectional (subordinate → superior) with no
 * downward collection, terminating at the root where {@code parent} is {@code null}.</p>
 *
 * <p>Includes the current occupant name so the breadcrumb trail shows both the
 * position title and who holds it at each level.</p>
 *
 * @param publicId             the public UUID of this position.
 * @param title                official title of the position.
 * @param category             functional category of the position.
 * @param rank                 military or police rank associated with this position.
 * @param vacant               whether the position is currently unfilled.
 * @param requiredClearanceLevel minimum clearance required for this position.
 * @param currentOccupantName  display name of the current holder; null if vacant.
 * @param verificationStatus   evidentiary provenance state of this record.
 * @param parent               the direct superior position; null for top-level slots.
 * @author Dimitri / Project Faust
 */
public record OccupationAscendedResponse(
        UUID publicId,
        String title,
        OccupationCategory category,
        String rank,
        boolean vacant,
        ClearanceLevel requiredClearanceLevel,
        String currentOccupantName,
        VerificationStatus verificationStatus,
        OccupationAscendedResponse parent
) {}
