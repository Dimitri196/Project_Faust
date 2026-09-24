package com.projectfaust.location;

import com.projectfaust.location.dto.LocationRequest;
import com.projectfaust.location.dto.LocationResponse;
import com.projectfaust.location.dto.UpdateParentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing the spatial and geopolitical hierarchy within Project Faust.
 *
 * <p>Exposes endpoints for the full lifecycle of geographic nodes — from sovereign states
 * down to individual rooms within classified facilities. All write operations require
 * at minimum {@code ROLE_ANALYST} authority. Deactivation and bulk import are restricted
 * to {@code ROLE_ADMIN}.</p>
 *
 * <p>The controller is intentionally thin — all business logic, hierarchy validation,
 * and security inheritance enforcement live in {@link LocationService}.</p>
 *
 * <p>Base path: {@code /api/v1/locations}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Management",
        description = "Endpoints for spatial nodes and hierarchical geo-data")
public class LocationController {

    private final LocationService locationService;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Registers a new geographic node in the spatial hierarchy.
     *
     * <p>Enforces granularity validation (e.g., ROOM cannot be placed under COUNTRY)
     * and security inheritance (child clearance is elevated to match parent if lower).
     * Request body is validated via Bean Validation before reaching the service layer.</p>
     *
     * @param request the creation request containing node attributes and optional parent UUID.
     * @return the persisted node as a {@link LocationResponse} with HTTP 201.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Create a new location",
            description = "Creates a node with security inheritance and granularity validation.")
    public ResponseEntity<LocationResponse> createLocation(
            @Valid @RequestBody LocationRequest request) {
        return new ResponseEntity<>(locationService.createLocation(request), HttpStatus.CREATED);
    }

    /**
     * Atomically imports multiple geographic nodes within a single transaction.
     *
     * <p>Intended for initial seeding of geographical datasets. The entire batch
     * is rolled back if any single node fails validation. Maximum batch size is
     * 500 nodes to prevent memory exhaustion.</p>
     *
     * @param requests list of location creation requests (max 500).
     * @return list of persisted nodes as {@link LocationResponse} objects with HTTP 201.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import locations",
            description = "Atomic transaction for seeding multiple hierarchical nodes. Max 500 per request.")
    public ResponseEntity<List<LocationResponse>> createLocationsBulk(
            @Valid @RequestBody @Size(max = 500, message = "Bulk import is limited to 500 nodes per request.")
            List<@Valid LocationRequest> requests) {
        return new ResponseEntity<>(locationService.createLocationsBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Relocates a node to a new parent within the spatial hierarchy.
     *
     * <p>The new parent UUID is passed in the request body rather than as a query
     * parameter to avoid logging sensitive node identifiers in proxy access logs.
     * Includes circular reference detection and re-applies security inheritance.</p>
     *
     * @param id      the public UUID of the node to relocate.
     * @param request a DTO containing the {@code newParentId}.
     * @return the updated node as a {@link LocationResponse}.
     */
    @PatchMapping("/{id}/parent")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Update parent",
            description = "Moves the node in the hierarchy. Includes circular reference check and clearance re-evaluation.")
    public ResponseEntity<LocationResponse> updateParent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParentRequest request) {
        return ResponseEntity.ok(locationService.updateParent(id, request.newParentId()));
    }

    /**
     * Soft-deactivates a geographic node and all its descendants.
     *
     * <p>No data is physically deleted. The node and its entire subtree are marked
     * inactive, preserving historical data integrity and audit trail continuity.</p>
     *
     * @param id the public UUID of the node to deactivate.
     * @return HTTP 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate location",
            description = "Marks the node and all descendants as inactive. Preserves historical data integrity.")
    public ResponseEntity<Void> deactivateLocation(@PathVariable UUID id) {
        locationService.deactivateLocation(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    /**
     * Executes a paginated, filtered search over all location nodes.
     *
     * <p>Supports filtering by name, type set, clearance level, verification status,
     * source type, parent scope, and geographic bounding box. Only non-null filter
     * fields are applied — omitting a field means "no restriction on that dimension".</p>
     *
     * @param filter   the search criteria; all fields are optional.
     * @param pageable pagination and sorting parameters (default: 20 per page, sorted by name).
     * @return a page of matching locations.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Search locations",
            description = "Filter by name, type, parentId, clearance, verificationStatus, sourceType, or map bounding box.")
    public ResponseEntity<Page<LocationResponse>> search(
            LocationFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(locationService.search(filter, pageable));
    }

    /**
     * Retrieves a single location by its public UUID.
     *
     * @param id the public UUID of the target location.
     * @return the matched location as a {@link LocationResponse}.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get location by UUID")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationByExternalId(id));
    }

    /**
     * Returns all active root-level nodes (nodes with no parent).
     *
     * <p>In a standard Faust deployment, root nodes represent sovereign states
     * or top-level classified geographic regions such as continents.</p>
     *
     * @return list of active root locations.
     */
    @GetMapping("/roots")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get root locations",
            description = "Returns active nodes that have no parent assigned.")
    public ResponseEntity<List<LocationResponse>> getRootLocations() {
        return ResponseEntity.ok(locationService.getRootLocations());
    }

    /**
     * Returns all active direct children of the specified parent node.
     *
     * <p>Intended for step-by-step drill-down navigation in the frontend map panel.</p>
     *
     * @param id the public UUID of the parent node.
     * @return list of active child locations.
     */
    @GetMapping("/{id}/sub-locations")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get children",
            description = "Returns immediate active child nodes for drill-down navigation.")
    public ResponseEntity<List<LocationResponse>> getSubLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getSubLocations(id));
    }

    /**
     * Resolves the full ancestor chain of a node as an ordered breadcrumb path.
     *
     * <p>Returns nodes ordered from root down to the requested node
     * (e.g., Europe → Czech Republic → Prague → Strakova Academy).</p>
     *
     * @param id the public UUID of the target node.
     * @return ordered list from root to node, inclusive.
     */
    @GetMapping("/{id}/path")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get path (breadcrumbs)",
            description = "Returns the full ordered ancestor chain from root to the given node.")
    public ResponseEntity<List<LocationResponse>> getLocationPath(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationPath(id));
    }
}