package com.projectfaust.occupation;

import com.projectfaust.occupation.dto.OccupationAscendedResponse;
import com.projectfaust.occupation.dto.OccupationRequest;
import com.projectfaust.occupation.dto.OccupationResponse;
import com.projectfaust.occupation.dto.OccupationTreeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing occupational positions and reporting structures
 * within Project Faust.
 *
 * <p>An {@link Occupation} defines a functional slot — a named position that
 * exists independently of whoever holds it. This controller exposes endpoints
 * for the full lifecycle of positions, including tree navigation for the
 * chain-of-command visualisation.</p>
 *
 * <p>Security tiers: {@code VIEWER} for all reads, {@code ANALYST} for create,
 * {@code ADMIN} for bulk import.</p>
 *
 * <p>Base path: {@code /api/v1/occupations}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/occupations")
@RequiredArgsConstructor
@Tag(name = "Occupation Management",
        description = "Operations for managing positions and the chain of command within institutions")
public class OccupationController {

    private final OccupationService service;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Creates a new functional position slot within an institution.
     *
     * <p>Links the position to an institution via {@code institutionPublicId}
     * and optionally defines a reporting line via {@code reportsToPublicId}.
     * Circular reference validation is applied if a supervisor is specified.</p>
     *
     * @param request the creation DTO (validated).
     * @return the persisted position with HTTP 201.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Create an occupation",
            description = "Creates a new position slot. Link to an institution and optionally define a supervisor.")
    public ResponseEntity<OccupationResponse> create(
            @Valid @RequestBody OccupationRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Atomically creates multiple positions within a single transaction.
     *
     * <p>Suitable for bulk ingestion of organisational charts from external
     * data sources. Maximum 500 positions per request.</p>
     *
     * @param requests list of position creation requests (max 500, each validated).
     * @return list of persisted positions with HTTP 201.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk create occupations",
            description = "Atomic transaction for mass ingestion of organisational roles. Max 500 per request.")
    public ResponseEntity<List<OccupationResponse>> createBulk(
            @Valid @RequestBody
            @Size(max = 500, message = "Bulk import is limited to 500 positions per request.")
            List<@Valid OccupationRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    // -------------------------------------------------------------------------
    // Read endpoints — single node
    // -------------------------------------------------------------------------

    /**
     * Retrieves the full dossier of a specific position slot.
     *
     * @param publicId the public UUID of the position.
     * @return full position metadata including current occupant and institution.
     */
    @GetMapping("/{publicId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get occupation by UUID",
            description = "Returns full details of a specific position including current holder.")
    public ResponseEntity<OccupationResponse> getOne(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * Returns all positions belonging to the specified institution.
     *
     * @param institutionId the public UUID of the institution.
     * @return list of all positions within the institution.
     */
    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get occupations by institution",
            description = "Returns all position slots belonging to the specified institution.")
    public ResponseEntity<List<OccupationResponse>> getByInstitution(
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(service.getByInstitution(institutionId));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — tree navigation
    // -------------------------------------------------------------------------

    /**
     * Returns the full chain of command — all top-level positions with their
     * subordinate hierarchy.
     *
     * @return recursive tree of all positions from top-level down.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get full command chain",
            description = "Returns the entire chain of command from top-level roles downward.")
    public ResponseEntity<List<OccupationTreeResponse>> getCommandChain() {
        return ResponseEntity.ok(service.getCommandChain());
    }

    /**
     * Returns the reporting hierarchy subtree rooted at the specified position.
     *
     * @param publicId the public UUID of the root position.
     * @return recursive tree response rooted at the specified position.
     */
    @GetMapping("/{publicId}/tree")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get command chain from node",
            description = "Returns the chain of command starting from the specified role.")
    public ResponseEntity<OccupationTreeResponse> getCommandChainById(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getCommandChainById(publicId));
    }

    /**
     * Returns all direct subordinate positions of the specified superior.
     *
     * @param publicId the public UUID of the superior position.
     * @return list of direct subordinate positions.
     */
    @GetMapping("/{publicId}/subordinates")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get direct subordinates",
            description = "Returns all positions that directly report to the specified role.")
    public ResponseEntity<List<OccupationResponse>> getSubordinates(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getSubordinates(publicId));
    }

    /**
     * Resolves the full chain of command upward from the specified position
     * for breadcrumb rendering.
     *
     * @param publicId the public UUID of the starting position.
     * @return the ascended chain of command.
     */
    @GetMapping("/{publicId}/chain-of-command")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get chain of command upward",
            description = "Resolves the reporting lineage from the specified role up to the top.")
    public ResponseEntity<OccupationAscendedResponse> getAscendedChain(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getAscendedChain(publicId));
    }
}