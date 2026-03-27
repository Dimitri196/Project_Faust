package com.projectfaust.controller;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.filters.LocationFilter;
import com.projectfaust.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the spatial and geopolitical hierarchy within Project Faust.
 * Provides endpoints for creating, navigating, and managing geographic nodes such as
 * countries, regions, and buildings.
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Management", description = "Endpoints for spatial nodes and hierarchical geo-data")
public class LocationController {

    private final LocationService locationService;

    /**
     * Registers a new geographic location in the system.
     */
    @PostMapping
    @Operation(summary = "Create a new location", description = "Creates a node with security inheritance and granularity validation.")
    public ResponseEntity<LocationResponse> createLocation(@RequestBody LocationRequest request) {
        return new ResponseEntity<>(locationService.createLocation(request), HttpStatus.CREATED);
    }

    /**
     * Performs a batch creation of multiple location nodes.
     * Optimized for initial seeding of geographical datasets.
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk import locations", description = "Atomic transaction for seeding multiple hierarchical nodes.")
    public ResponseEntity<List<LocationResponse>> createLocationsBulk(@RequestBody List<LocationRequest> requests) {
        return new ResponseEntity<>(locationService.createLocationsBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Dynamically searches for locations based on complex filter criteria.
     * Supports pagination, sorting, and geospatial bounding box.
     */
    @GetMapping("/search")
    @Operation(summary = "Search locations", description = "Filter by name, type, parentId, clearance, or map coordinates.")
    public ResponseEntity<Page<LocationResponse>> search(
            LocationFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(locationService.search(filter, pageable));
    }

    /**
     * Retrieves specific location metadata via its external UUID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get location by UUID")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationByExternalId(id));
    }

    /**
     * Retrieves all top-level geographic nodes (e.g., Countries).
     */
    @GetMapping("/roots")
    @Operation(summary = "Get root locations", description = "Retrieves active nodes that have no parent assigned.")
    public ResponseEntity<List<LocationResponse>> getRootLocations() {
        return ResponseEntity.ok(locationService.getRootLocations());
    }

    /**
     * Retrieves immediate child nodes for a given location.
     */
    @GetMapping("/{id}/sub-locations")
    @Operation(summary = "Get children", description = "Useful for step-by-step drill-down navigation.")
    public ResponseEntity<List<LocationResponse>> getSubLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getSubLocations(id));
    }

    /**
     * Resolves the full breadcrumb path from a specific location up to the root.
     */
    @GetMapping("/{id}/path")
    @Operation(summary = "Get path (Breadcrumbs)", description = "Returns the full list of ancestors for the given node.")
    public ResponseEntity<List<LocationResponse>> getLocationPath(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationPath(id));
    }

    /**
     * Reassigns a location node to a different parent within the hierarchy.
     */
    @PatchMapping("/{id}/parent")
    @Operation(summary = "Update parent", description = "Moves the node in the hierarchy. Includes circular reference check.")
    public ResponseEntity<LocationResponse> updateParent(
            @PathVariable UUID id,
            @RequestParam UUID newParentId) {
        return ResponseEntity.ok(locationService.updateParent(id, newParentId));
    }

    /**
     * Deactivates a location node (Soft-delete).
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate location", description = "Marks the node as inactive to preserve historical data integrity.")
    public ResponseEntity<Void> deactivateLocation(@PathVariable UUID id) {
        locationService.deactivateLocation(id);
        return ResponseEntity.noContent().build();
    }
}
