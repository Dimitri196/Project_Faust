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

@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institution Management", description = "Operations for mapping the global political and state-corporate hierarchy")
public class InstitutionController {

    private final InstitutionService service;

    @PostMapping
    @Operation(summary = "Create an institution", description = "Creates a new record. Use parentExternalId to link it into the hierarchy (e.g. link ČD to Ministry of Transport).")
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create institutions", description = "Accepts an array of institutions. Useful for initial country setup.")
    public ResponseEntity<List<InstitutionResponse>> createBulk(@Valid @RequestBody List<InstitutionRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Find by UUID", description = "Returns full details of a single institution.")
    public ResponseEntity<InstitutionResponse> getById(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search with filters", description = "Dynamic search using Spring Data Specifications. All parameters are optional.")
    public ResponseEntity<List<InstitutionResponse>> search(
            @Parameter(description = "Partial name match (case-insensitive)") @RequestParam(required = false) String name,
            @Parameter(description = "2-letter ISO country code (e.g. CZ)") @RequestParam(required = false) String country,
            @Parameter(description = "Filter by state ownership") @RequestParam(required = false) Boolean isStateOwned) {

        return ResponseEntity.ok(service.search(name, country, isStateOwned));
    }

    // Od kořene k potomkům (Organigram)
    @GetMapping("/tree")
    public ResponseEntity<List<InstitutionTreeResponse>> getFullTree() {
        return ResponseEntity.ok(service.getFullTree());
    }

    // Od potomka k rodiči (Linie velení)
    @GetMapping("/{publicId}/path-to-root")
    public ResponseEntity<InstitutionAscendedResponse> getPathToRoot(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getAscendedPath(publicId));
    }

    @GetMapping("/{publicId}/sub-tree")
    @Operation(summary = "Get sub-tree downwards", description = "Returns the hierarchy starting from this node down to all its descendants.")
    public ResponseEntity<InstitutionTreeResponse> getSubTree(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getSubTree(publicId));
    }

}
