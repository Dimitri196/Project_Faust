package com.projectfaust.institution.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a node in the institutional hierarchy tree.
 *
 * <p>Used by two distinct rendering modes in the frontend:</p>
 * <ul>
 *   <li><b>Flat mode</b> — {@code children} is null or empty; the node is a
 *       lazy-load placeholder. The frontend requests children via
 *       {@code GET /parent/{id}} when the node is expanded.</li>
 *   <li><b>Deep mode</b> — {@code children} is populated recursively for the
 *       Nexus Focus graph visualisation. Only used when the entity was loaded
 *       with an eager {@code JOIN FETCH} or {@code @EntityGraph}.</li>
 * </ul>
 *
 * <p><b>Serialisation safety:</b> this record intentionally uses {@code UUID parentId}
 * rather than a nested {@code InstitutionTreeResponse parent} object. Including a full
 * parent reference alongside a children list creates a bidirectional recursive structure
 * that causes an infinite loop during JSON serialisation. The {@code parentId} scalar
 * gives the frontend sufficient context for breadcrumb rendering without the risk.</p>
 *
 * <p>For full ancestor chain rendering, use {@link InstitutionAscendedResponse} instead,
 * which is unidirectional (parent only, no children) and therefore serialisation-safe.</p>
 *
 * @param publicId           the public UUID of this node.
 * @param name               official name of the institution.
 * @param level              administrative tier (NATIONAL, REGIONAL, LOCAL).
 * @param type               functional category (EXECUTIVE, INTELLIGENCE, etc.).
 * @param clearanceLevel     minimum clearance required to view this node.
 * @param description        brief description of the institution's mandate.
 * @param stateOwned         whether the institution is state-controlled or owned.
 * @param active             whether the institution is currently operational.
 * @param hasChildren        whether this node has at least one active child node.
 * @param verificationStatus evidentiary provenance state — used to render trust badges
 *                           and flag DECEPTION_MARKER nodes in the graph.
 * @param parentId           public UUID of the parent node; {@code null} for root nodes.
 * @param children           subordinate nodes; null in flat mode, populated in deep mode.
 * @param logoUrl            URL of the institution's logo for graph node rendering.
 * @author Dimitri / Project Faust
 */
public record InstitutionTreeResponse(
        UUID publicId,
        String name,
        HierarchicalLevel level,
        InstitutionType type,
        ClearanceLevel clearanceLevel,
        String description,
        boolean stateOwned,
        boolean active,
        boolean hasChildren,
        VerificationStatus verificationStatus,
        UUID parentId,
        List<InstitutionTreeResponse> children,
        String logoUrl
) {}