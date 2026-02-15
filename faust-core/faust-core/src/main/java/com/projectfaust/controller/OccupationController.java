package com.projectfaust.controller;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.service.OccupationService;
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
@RequestMapping("/api/v1/occupations")
@RequiredArgsConstructor
@Tag(name = "Occupation Management", description = "Operations for managing positions within institutions")
public class OccupationController {

    private final OccupationService service;

    @PostMapping
    @Operation(summary = "Create an occupation", description = "Creates a new position slot. Link to an institution via UUID and optionally a supervisor.")
    public ResponseEntity<OccupationResponse> create(@Valid @RequestBody OccupationRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @GetMapping("/{publicId}")
    @Operation(summary = "Get occupation by UUID", description = "Returns details of a specific position slot.")
    public ResponseEntity<OccupationResponse> getOne(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @GetMapping("/institution/{institutionId}")
    @Operation(summary = "Get occupations by institution", description = "Returns all positions belonging to a specific department.")
    public ResponseEntity<List<OccupationResponse>> getByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(service.getByInstitution(institutionId));
    }

    @GetMapping("/tree")
    @Operation(summary = "Get full reporting tree", description = "Returns the entire chain of command starting from top-level roles.")
    public ResponseEntity<List<OccupationTreeResponse>> getOccupationTree() {
        return ResponseEntity.ok(service.getCommandChain());
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<OccupationResponse>> bulkCreateOccupations(
            @RequestBody List<OccupationRequest> dtos) {
        return ResponseEntity.ok(service.createBulk(dtos));
    }
}
