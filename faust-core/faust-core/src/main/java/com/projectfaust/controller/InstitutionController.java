package com.projectfaust.controller;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.service.InstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for managing the institutional hierarchy within Project Faust.
 * Provides endpoints for mapping state-corporate structures, tree traversal,
 * and advanced multi-parameter searching.
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institution Management", description = "Operations for mapping the global political and state-corporate hierarchy")
public class InstitutionController {

    private final InstitutionService service;

    /**
     * Creates a new institution record.
     *
     * @param request Data containing identity, parent links, and ownership status.
     * @return The created institution profile.
     */
    @PostMapping
    @Operation(summary = "Create an institution",
            description = "Creates a new record. Use parentExternalId to link it into the hierarchy (e.g., linking a state-owned enterprise to a Ministry).")
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Performs a batch creation of multiple institutional nodes.
     * Optimized for initial country-level registry synchronization.
     *
     * @param requests List of institutions to be registered.
     * @return List of processed institution responses.
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk create institutions",
            description = "Accepts an array of institutions. Optimized for initial system setup or migration.")
    public ResponseEntity<List<InstitutionResponse>> createBulk(@Valid @RequestBody List<InstitutionRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Retrieves the technical dossier of a single institution by its UUID.
     *
     * @param publicId The unique identifier of the institution.
     * @return Full institution metadata.
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Find by UUID", description = "Returns full details of a single institution node.")
    public ResponseEntity<InstitutionResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * Generates a complete recursive tree of all registered institutions.
     *
     * @return The full global institutional hierarchy.
     */
    @GetMapping("/tree")
    @Operation(summary = "Get global hierarchy tree", description = "Returns the entire institutional forest starting from root-level entities.")
    public ResponseEntity<List<InstitutionTreeResponse>> getFullTree() {
        return ResponseEntity.ok(service.getFullTree());
    }

    /**
     * Resolves the lineage of an institution from its current node up to the root.
     *
     * @param publicId The UUID of the starting institution.
     * @return The ascending path representing the chain of command/ownership.
     */
    @GetMapping("/{publicId}/path-to-root")
    @Operation(summary = "Get path to root", description = "Resolves the organizational lineage upwards to the top-level parent.")
    public ResponseEntity<InstitutionAscendedResponse> getPathToRoot(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getAscendedPath(publicId));
    }

    /**
     * Generates a hierarchical sub-tree starting from the specified node.
     *
     * @param publicId The UUID of the sub-tree root.
     * @return The downward recursive hierarchy of all descendants.
     */
    @GetMapping("/{publicId}/sub-tree")
    @Operation(summary = "Get sub-tree downwards", description = "Returns the hierarchy starting from this node down to all its descendants.")
    public ResponseEntity<InstitutionTreeResponse> getSubTree(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getSubTree(publicId));
    }

    /**
     * Retrieves immediate subordinate entities for a specific parent.
     *
     * @param parentPublicId The UUID of the parent institution.
     * @return A list of direct children.
     */
    @GetMapping("/parent/{parentPublicId}")
    @Operation(summary = "Get immediate children", description = "Returns a list of immediate subordinate institutions for a given parent.")
    public ResponseEntity<List<InstitutionResponse>> getChildren(@PathVariable UUID parentPublicId) {
        return ResponseEntity.ok(service.getImmediateChildren(parentPublicId));
    }

    /**
     * Performs a dynamic search using specialized filters.
     * * @param name Optional partial name match.
     * @param country Optional ISO country code.
     * @param isStateOwned Optional filter for ownership type.
     * @param locationId Optional spatial filter.
     * @param parentId Optional parent scope filter.
     * @return A list of institutions matching the specified criteria.
     */
    @GetMapping("/search")
    @Operation(summary = "Search with filters", description = "Dynamic search using Spring Data Specifications. All parameters are optional.")
    public ResponseEntity<List<InstitutionResponse>> search(
            @Parameter(description = "Partial name match (case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "2-letter ISO country code (e.g., CZ)") @RequestParam(required = false) String country,
            @Parameter(description = "Filter by state ownership") @RequestParam(required = false) Boolean isStateOwned,
            @Parameter(description = "Filter by specific location UUID") @RequestParam(required = false) UUID locationId,
            @Parameter(description = "Filter by parent institution UUID") @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(service.search(name, country, isStateOwned, locationId, parentId));
    }
}
