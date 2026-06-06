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
import org.springframework.web.bind.annotation.*;



/**
 * REST controller for managing the institutional hierarchy within Project Faust.
 * Orchestrates flat navigation for tree views and recursive retrieval for nexus mapping.
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institution Management", description = "Operations for mapping the global political and state-corporate hierarchy")
public class InstitutionController {

    private final InstitutionService service;

    @PostMapping
    @Operation(summary = "Create an institution")
    public ResponseEntity<InstitutionResponse> create(@Valid @RequestBody InstitutionRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create institutions")
    public ResponseEntity<List<InstitutionResponse>> createBulk(@Valid @RequestBody List<InstitutionRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Find by UUID", description = "Returns full details of a single institution node.")
    public ResponseEntity<InstitutionResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * OPRAVA: Původní getFullTree nyní volá getRootNodes.
     * Vrací pouze top-level uzly pro počáteční vykreslení stromu.
     */
    @GetMapping("/tree")
    @Operation(summary = "Get root hierarchy nodes", description = "Returns top-level institutions. Use /parent/{id} to fetch deeper levels.")
    public ResponseEntity<List<InstitutionTreeResponse>> getFullTree() {
        return ResponseEntity.ok(service.getRootNodes());
    }

    /**
     * OPRAVA: Endpoint pro Lazy Loading na frontendu.
     * Musí vracet List<InstitutionTreeResponse>, ne InstitutionResponse, aby React mohl rekurzivně stavět strom.
     */
    @GetMapping("/parent/{parentPublicId}")
    @Operation(summary = "Get immediate children", description = "Returns immediate subordinate nodes for lazy loading in tree views.")
    public ResponseEntity<List<InstitutionTreeResponse>> getChildren(@PathVariable UUID parentPublicId) {
        return ResponseEntity.ok(service.getImmediateChildren(parentPublicId));
    }

    @GetMapping("/{publicId}/sub-tree")
    @Operation(summary = "Get sub-tree downwards", description = "Returns deep recursive hierarchy starting from this node. Primarily for Nexus/Graph visualization.")
    public ResponseEntity<InstitutionTreeResponse> getSubTree(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getSubTree(publicId));
    }

    @GetMapping("/{publicId}/path-to-root")
    @Operation(summary = "Get path to root", description = "Resolves the organizational lineage upwards to the top-level parent.")
    public ResponseEntity<InstitutionAscendedResponse> getPathToRoot(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getAscendedPath(publicId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search with filters", description = "Dynamic search using Spring Data Specifications.")
    public ResponseEntity<List<InstitutionResponse>> search(
            @Parameter(description = "Partial name match") @RequestParam(required = false) String name,
            @Parameter(description = "ISO country code") @RequestParam(required = false) String country,
            @Parameter(description = "Filter by state ownership") @RequestParam(required = false) Boolean isStateOwned,
            @Parameter(description = "Filter by location UUID") @RequestParam(required = false) UUID locationId,
            @Parameter(description = "Filter by parent UUID") @RequestParam(required = false) UUID parentId) {
        return ResponseEntity.ok(service.search(name, country, isStateOwned, locationId, parentId));
    }
}