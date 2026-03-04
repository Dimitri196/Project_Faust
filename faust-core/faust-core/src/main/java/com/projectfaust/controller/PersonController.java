package com.projectfaust.controller;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
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
 * REST controller for managing human assets and biographical profiles.
 * Provides endpoints for identity registration, profile synchronization,
 * and advanced subject discovery within the intelligence database.
 */
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person Management", description = "Operations for managing human profiles (officials, employees, and subjects)")
public class PersonController {

    private final PersonService service;

    /**
     * Registers a new individual in the registry.
     * Capture includes academic titles, education levels, and normalized identity data.
     *
     * @param request Data containing names, titles, and biographical attributes.
     * @return The created person response with its global identifier (UUID).
     */
    @PostMapping
    @Operation(summary = "Create a person",
            description = "Creates a new human profile including academic titles and education level.")
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Retrieves a list of all subjects currently indexed in the registry.
     *
     * @return A collection of abbreviated person profiles.
     */
    @GetMapping
    @Operation(summary = "List all persons", description = "Retrieves the complete catalog of individuals in the system.")
    public ResponseEntity<List<PersonResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    /**
     * Retrieves the full biographical record of a specific subject by their UUID.
     *
     * @param publicId The unique identifier of the person.
     * @return The detailed profile response.
     */
    @GetMapping("/{publicId}")
    @Operation(summary = "Get person by UUID", description = "Fetches the full identity record for a specific subject.")
    public ResponseEntity<PersonResponse> getOne(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * Updates an existing subject's record.
     * Used for synchronizing biographical changes or correcting identity data.
     *
     * @param publicId The unique identifier of the person to be modified.
     * @param request The updated set of person attributes.
     * @return The updated person response.
     */
    @PutMapping("/{publicId}")
    @Operation(summary = "Update person details", description = "Modifies existing biographical data for a subject.")
    public ResponseEntity<PersonResponse> update(@PathVariable UUID publicId, @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    /**
     * Performs a partial name search across the subject registry.
     * Utilizes the normalized name fields for high-accuracy matching.
     *
     * @param query The search string (first name, last name, or combined).
     * @return A list of subjects matching the search criteria.
     */
    @GetMapping("/search")
    @Operation(summary = "Search persons by name", description = "Returns matches based on first name, last name, or combined full name.")
    public ResponseEntity<List<PersonResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(service.searchByName(query));
    }

    /**
     * Processes a mass ingestion of human profiles.
     * Designed for bulk synchronization from external civil registries or social datasets.
     *
     * @param requests List of person profiles to be ingested.
     * @return The list of newly created person responses.
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk import persons",
            description = "Ingest a list of multiple human profiles in a single atomic transaction.")
    public ResponseEntity<List<PersonResponse>> createBulk(@Valid @RequestBody List<PersonRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }
}
