package com.projectfaust.controller;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing the spatial and geopolitical hierarchy within Project Faust.
 * Provides endpoints for creating, navigating, and managing geographic nodes such as
 * countries, regions, and cities.
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    /**
     * Registers a new geographic location in the system.
     *
     * @param request Data containing location name, type, and optional parent link.
     * @return The created location metadata.
     */
    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@RequestBody LocationRequest request) {
        return new ResponseEntity<>(locationService.createLocation(request), HttpStatus.CREATED);
    }

    /**
     * Performs a batch creation of multiple location nodes.
     * Optimized for initial seeding of geographical datasets.
     *
     * @param requests List of location data to be processed.
     * @return List of successfully created location responses.
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<LocationResponse>> createLocationsBulk(@RequestBody List<LocationRequest> requests) {
        List<LocationResponse> responses = locationService.createLocationsBulk(requests);
        return new ResponseEntity<>(responses, HttpStatus.CREATED);
    }

    /**
     * Retrieves specific location metadata via its external UUID.
     *
     * @param id The global unique identifier of the location.
     * @return The found location details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationByExternalId(id));
    }

    /**
     * Retrieves all top-level geographic nodes (e.g., Countries).
     *
     * @return A list of root locations that have no parent assigned.
     */
    @GetMapping("/roots")
    public ResponseEntity<List<LocationResponse>> getRootLocations() {
        return ResponseEntity.ok(locationService.getRootLocations());
    }

    /**
     * Retrieves immediate child nodes for a given location.
     * Useful for step-by-step drill-down navigation (e.g., Country -> Regions).
     *
     * @param id The UUID of the parent location.
     * @return A list of active sub-locations.
     */
    @GetMapping("/{id}/sub-locations")
    public ResponseEntity<List<LocationResponse>> getSubLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getSubLocations(id));
    }

    /**
     * Resolves the full breadcrumb path from a specific location up to the root.
     *
     * @param id The UUID of the starting location node.
     * @return A list of locations representing the hierarchical path.
     */
    @GetMapping("/{id}/path")
    public ResponseEntity<List<LocationResponse>> getLocationPath(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationPath(id));
    }

    /**
     * Reassigns a location node to a different parent within the hierarchy.
     *
     * @param id The UUID of the location to be moved.
     * @param newParentId The UUID of the new target parent node.
     * @return The updated location metadata.
     */
    @PatchMapping("/{id}/parent")
    public ResponseEntity<LocationResponse> updateParent(
            @PathVariable UUID id,
            @RequestParam UUID newParentId) {
        return ResponseEntity.ok(locationService.updateParent(id, newParentId));
    }

    /**
     * Deactivates a location node.
     * Soft-deletes the entity to maintain historical integrity of linked data.
     *
     * @param id The UUID of the location to be deactivated.
     * @return 204 No Content on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateLocation(@PathVariable UUID id) {
        locationService.deactivateLocation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Dynamically searches for locations based on name, type, or parent affiliation.
     *
     * @param name Optional partial name filter.
     * @param type Optional filter for location type (e.g., 'CITY', 'COUNTRY').
     * @param parentId Optional filter to search within a specific branch.
     * @return A list of matching location nodes.
     */
    @GetMapping("/search")
    public ResponseEntity<List<LocationResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(locationService.search(name, type, parentId));
    }
}
