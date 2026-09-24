package com.projectfaust.location;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for batch geocoding operations.
 *
 * <p>Base path: {@code /api/v1/admin/geocode}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/admin/geocode")
@RequiredArgsConstructor
@Tag(name = "Geocoding", description = "Batch GPS coordinate resolution via Nominatim")
public class BatchGeocodingController {

    private final BatchGeocodingService geocodingService;

    /**
     * Geocodes all locations without GPS under a given parent.
     *
     * @param parentId internal Long ID of the parent location.
     * @return result summary.
     */
    @PostMapping("/parent/{parentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Geocode locations under a parent",
            description = "Resolves GPS coordinates for all child locations " +
                    "without coordinates using Nominatim. " +
                    "Rate limited to 1 req/sec — large districts take time. ADMIN only."
    )
    public ResponseEntity<BatchGeocodingService.GeocodingResult> geocodeByParent(
            @PathVariable Long parentId) {
        return ResponseEntity.ok(geocodingService.geocodeByParent(parentId));
    }
}