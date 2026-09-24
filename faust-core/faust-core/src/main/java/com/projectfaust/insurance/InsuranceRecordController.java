package com.projectfaust.insurance;

import com.projectfaust.insurance.dto.InsuranceRecordRequestDto;
import com.projectfaust.insurance.dto.InsuranceRecordResponseDto;
import com.projectfaust.shared.enums.InsuranceType;
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

/**
 * REST controller for managing insurance policy records within Project Faust.
 *
 * <p>Exposes CRUD operations scoped both to individual records (by UUID) and
 * to a subject's full insurance profile (by person UUID). The
 * {@code /by-person} sub-resource is the primary access pattern for the SPA's
 * Insurance panel on a PersonDetailPage.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/insurance-records")
@RequiredArgsConstructor
@Tag(name = "Insurance Records", description = "Operations for managing insurance policy records linked to persons")
public class InsuranceRecordController {

    private final InsuranceRecordService service;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Create an insurance record",
            description = "Persists a new insurance policy record. If (sourceSystem, sourceReferenceId) already exists, the existing record is updated instead.")
    public ResponseEntity<InsuranceRecordResponseDto> create(
            @Valid @RequestBody InsuranceRecordRequestDto request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create insurance records",
            description = "Persists multiple insurance records in a single transaction. Existing records (matched by source reference) are updated.")
    public ResponseEntity<List<InsuranceRecordResponseDto>> createBulk(
            @Valid @RequestBody List<InsuranceRecordRequestDto> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    @PutMapping("/{publicId}")
    @Operation(summary = "Update an insurance record",
            description = "Applies a partial merge — only non-null fields in the request overwrite the existing record.")
    public ResponseEntity<InsuranceRecordResponseDto> update(
            @Parameter(description = "Public UUID of the insurance record")
            @PathVariable UUID publicId,
            @Valid @RequestBody InsuranceRecordRequestDto request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @Operation(summary = "Delete an insurance record")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Public UUID of the insurance record")
            @PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Read endpoints — by record UUID
    // -------------------------------------------------------------------------

    @GetMapping("/{publicId}")
    @Operation(summary = "Get insurance record by UUID",
            description = "Returns full details of a single insurance policy record.")
    public ResponseEntity<InsuranceRecordResponseDto> getByPublicId(
            @Parameter(description = "Public UUID of the insurance record")
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — by person
    // -------------------------------------------------------------------------

    @GetMapping("/by-person/{personPublicId}")
    @Operation(summary = "Get all insurance records for a person",
            description = "Returns the full insurance profile for a subject, ordered newest-first.")
    public ResponseEntity<List<InsuranceRecordResponseDto>> getByPerson(
            @Parameter(description = "Public UUID of the person")
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getByPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/active")
    @Operation(summary = "Get active insurance records for a person",
            description = "Returns only currently active policies for the specified subject.")
    public ResponseEntity<List<InsuranceRecordResponseDto>> getActiveByPerson(
            @Parameter(description = "Public UUID of the person")
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getActiveByPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/type/{insuranceType}")
    @Operation(summary = "Get insurance records for a person filtered by type",
            description = "Returns all policies of the specified type held by the subject.")
    public ResponseEntity<List<InsuranceRecordResponseDto>> getByPersonAndType(
            @Parameter(description = "Public UUID of the person")
            @PathVariable UUID personPublicId,
            @Parameter(description = "Insurance type filter (e.g. LIFE, HEALTH, VEHICLE_LIABILITY)")
            @PathVariable InsuranceType insuranceType) {
        return ResponseEntity.ok(service.getByPersonAndType(personPublicId, insuranceType));
    }
}