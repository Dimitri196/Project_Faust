package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.util.UUID;

/**
 * Response DTO representing a node in the institutional ancestor chain.
 *
 * <p>Used to render organisational breadcrumbs — the path from any node
 * up to the root ancestor of the hierarchy.</p>
 *
 * <p><b>Serialisation safety:</b> unlike {@link InstitutionTreeResponse}, this record
 * contains only a {@code parent} reference with no {@code children} collection.
 * The chain is strictly unidirectional (child → parent → grandparent → ... → root),
 * which terminates naturally at the root node where {@code parent} is {@code null}.
 * There is no risk of infinite recursion during JSON serialisation.</p>
 *
 * <p>The recursive {@code parent} field is populated by
 * {@link com.projectfaust.institution.InstitutionMapper#toAscendedResponse} and
 * requires the parent chain to be loaded within an active {@code @Transactional}
 * boundary.</p>
 *
 * @param publicId           the public UUID of this node.
 * @param name               official name of the institution.
 * @param level              administrative tier (NATIONAL, REGIONAL, LOCAL).
 * @param type               functional category (EXECUTIVE, INTELLIGENCE, etc.).
 * @param clearanceLevel     minimum clearance required to view this node.
 * @param description        brief description of the institution's mandate.
 * @param stateOwned         whether the institution is state-controlled or owned.
 * @param active             whether the institution is currently operational.
 * @param verificationStatus evidentiary provenance state of this record.
 * @param parent             the direct parent node; {@code null} for root nodes.
 * @author Dimitri / Project Faust
 */
public record InstitutionAscendedResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        ClearanceLevel clearanceLevel,
        String description,
        boolean stateOwned,
        boolean active,
        VerificationStatus verificationStatus,
        InstitutionAscendedResponse parent
) {}
