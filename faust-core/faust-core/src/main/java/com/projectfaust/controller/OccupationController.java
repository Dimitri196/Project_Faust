package com.projectfaust.controller;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationAscendedResponse;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.service.OccupationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing the organizational roles and reporting structures.
 * Defines the functional "slots" within institutions and maps the operational chain of command.
 */
@RestController
@RequestMapping("/api/v1/occupations")
@RequiredArgsConstructor
@Tag(name = "Occupation Management", description = "Operations for managing positions within institutions")
public class OccupationController {

    private final OccupationService service;

    /**
     * Defines a new functional role within an institution.
     *
     * @param request Data containing role title, institution link, and optional reporting line.
     * @return The created occupation metadata.
     */
    @PostMapping
    @Operation(summary = "Create an occupation",
            description = "Creates a new position slot. Link to an institution via UUID and optionally define a supervisor.")
    public ResponseEntity<OccupationResponse> create(@Valid @RequestBody OccupationRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves the technical dossier of a specific position slot.
     *
     * @param publicId The unique identifier of the occupation.
     * @return Full occupation metadata including current status.
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get occupation by UUID", description = "Returns details of a specific position slot.")
    public ResponseEntity<OccupationResponse> getOne(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * Retrieves all functional roles associated with a specific institutional department.
     *
     * @param institutionId The UUID of the target institution.
     * @return A list of all positions within the specified organization.
     */
    @GetMapping("/institution/{institutionId}")
    @Operation(summary = "Get occupations by institution",
            description = "Returns all positions belonging to a specific department or entity.")
    public ResponseEntity<List<OccupationResponse>> getByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(service.getByInstitution(institutionId));
    }

    /**
     * Resolves the entire functional chain of command across the system.
     *
     * @return A recursive tree representing the hierarchy of roles.
     */
    @GetMapping("/tree")
    @Operation(summary = "Get full reporting tree",
            description = "Returns the entire chain of command starting from top-level roles down to subordinates.")
    public ResponseEntity<List<OccupationTreeResponse>> getOccupationTree() {
        return ResponseEntity.ok(service.getCommandChain());
    }

    /**
     * Performs a batch creation of occupation records.
     * Ideal for synchronizing large organizational charts from external data sources.
     *
     * @param dtos A collection of occupation requests.
     * @return A list of successfully created occupation responses.
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk create occupations", description = "Facilitates mass ingestion of organizational roles.")
    public ResponseEntity<List<OccupationResponse>> bulkCreateOccupations(
            @RequestBody List<OccupationRequest> dtos) {
        return ResponseEntity.ok(service.createBulk(dtos));
    }

    /**
     * Retrieves all direct and indirect reports for a specific position.
     *
     * @param id The UUID of the superior position.
     * @return A list of subordinate roles linked to the target node.
     */
    @GetMapping("/{id}/subordinates")
    @Operation(summary = "Get subordinates", description = "Retrieves all reporting lines directly underneath a specific role.")
    public ResponseEntity<List<OccupationAscendedResponse>> getSubordinates(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getSubordinates(id));
    }

    /**
     * Resolves the functional chain of command starting from a specific node.
     *
     * @param id The UUID of the root position for the tree.
     * @return A recursive tree representing the hierarchy starting from the target role.
     */
    @GetMapping("/{id}/tree")
    @Operation(summary = "Get partial reporting tree",
            description = "Returns the chain of command starting from the specified role for visual mapping.")
    public ResponseEntity<OccupationTreeResponse> getOccupationTreeById(@PathVariable UUID id) {
        // Voláme service pro získání podstromu pro konkrétní ID
        return ResponseEntity.ok(service.getCommandChainById(id));
    }

}
