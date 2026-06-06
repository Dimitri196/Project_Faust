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

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person Management", description = "Operations for managing human profiles (officials, employees, and subjects)")
public class PersonController {

    private final PersonService service;

    @PostMapping
    @Operation(summary = "Create a person", description = "Creates a new human profile. Automatically initializes the primary identity.")
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody PersonRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all persons")
    public ResponseEntity<List<PersonResponse>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get person by UUID")
    public ResponseEntity<PersonResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update person details", description = "Modifies biographical data and synchronizes name history.")
    public ResponseEntity<PersonResponse> update(@PathVariable UUID publicId, @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    @GetMapping("/search")
    @Operation(summary = "Search persons by primary name")
    public ResponseEntity<List<PersonResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(service.searchByName(query));
    }

    @GetMapping("/search/deep")
    @Operation(summary = "Deep identity search", description = "Searches across all historical names and aliases.")
    public ResponseEntity<List<PersonResponse>> searchDeep(@RequestParam String query) {
        return ResponseEntity.ok(service.searchByAnyIdentity(query));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk import persons")
    public ResponseEntity<List<PersonResponse>> createBulk(@Valid @RequestBody List<PersonRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }
}
