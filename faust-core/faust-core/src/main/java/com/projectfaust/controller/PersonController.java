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

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person Management", description = "Operations for managing human profiles (officials, employees)")
public class PersonController {

    private final PersonService service;

    @PostMapping
    @Operation(summary = "Create a person", description = "Creates a new human profile with academic titles and education level.")
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
    public ResponseEntity<PersonResponse> getOne(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update person details")
    public ResponseEntity<PersonResponse> update(@PathVariable UUID publicId, @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    @GetMapping("/search")
    @Operation(summary = "Search persons by name or last name")
    public ResponseEntity<List<PersonResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(service.searchByName(query));
    }
}
