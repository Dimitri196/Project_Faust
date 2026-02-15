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

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<LocationResponse> createLocation(@RequestBody LocationRequest request) {
        return new ResponseEntity<>(locationService.createLocation(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LocationResponse> getLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationByExternalId(id));
    }

    @GetMapping("/roots")
    public ResponseEntity<List<LocationResponse>> getRootLocations() {
        return ResponseEntity.ok(locationService.getRootLocations());
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<List<LocationResponse>> getLocationPath(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getLocationPath(id));
    }

    @GetMapping("/{id}/sub-locations")
    public ResponseEntity<List<LocationResponse>> getSubLocations(@PathVariable UUID id) {
        return ResponseEntity.ok(locationService.getSubLocations(id));
    }

    @PatchMapping("/{id}/parent")
    public ResponseEntity<LocationResponse> updateParent(
            @PathVariable UUID id,
            @RequestParam UUID newParentId) {
        return ResponseEntity.ok(locationService.updateParent(id, newParentId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(@PathVariable UUID id) {
        locationService.deleteLocation(id);
                return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<LocationResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(locationService.search(name, type, parentId));
    }
}
