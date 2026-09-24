package com.projectfaust.criminal;

import com.projectfaust.criminal.dto.CriminalRecordRequestDto;
import com.projectfaust.criminal.dto.CriminalRecordResponseDto;
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
 * REST controller for criminal record management within Project Faust.
 *
 * <p>Base path: {@code /api/v1/criminal-records}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/criminal-records")
@RequiredArgsConstructor
@Tag(name = "Criminal Records",
     description = "HUMINT criminal intelligence — convictions, sentences, expiry, Interpol notices.")
public class CriminalRecordController {

    private final CriminalRecordService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Ingest a criminal record",
               description = "Upserts on (sourceSystem, sourceReferenceId) if supplied.")
    public ResponseEntity<CriminalRecordResponseDto> create(
            @Valid @RequestBody CriminalRecordRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk-ingest criminal records")
    public ResponseEntity<List<CriminalRecordResponseDto>> createBulk(
            @Valid @RequestBody List<CriminalRecordRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBulk(dtos));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{publicId}")
    @Operation(summary = "Partially update a criminal record")
    public ResponseEntity<CriminalRecordResponseDto> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody CriminalRecordRequestDto dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a criminal record")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by ID ──────────────────────────────────────────────────────────

    @GetMapping("/{publicId}")
    @Operation(summary = "Get a criminal record by public ID")
    public ResponseEntity<CriminalRecordResponseDto> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // ── Read — by person ──────────────────────────────────────────────────────

    @GetMapping("/by-person/{personPublicId}")
    @Operation(summary = "Get all criminal records for a person",
               description = "Returns all entries (including expunged) for auditing purposes.")
    public ResponseEntity<List<CriminalRecordResponseDto>> getByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getBySubjectPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/active")
    @Operation(summary = "Get active (non-expunged) criminal records for a person")
    public ResponseEntity<List<CriminalRecordResponseDto>> getActiveByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getActiveBySubjectPerson(personPublicId));
    }

    // ── Read — by institution ─────────────────────────────────────────────────

    @GetMapping("/by-institution/{institutionPublicId}")
    @Operation(summary = "Get all criminal records for an institution",
               description = "Corporate criminal liability entries.")
    public ResponseEntity<List<CriminalRecordResponseDto>> getByInstitution(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getBySubjectInstitution(institutionPublicId));
    }

    // ── Read — by offense / status ────────────────────────────────────────────

    @GetMapping("/by-category/{category}")
    @Operation(summary = "Get criminal records by offense category")
    public ResponseEntity<List<CriminalRecordResponseDto>> getByCategory(
            @PathVariable OffenseCategory category) {
        return ResponseEntity.ok(service.getByOffenseCategory(category));
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "Get criminal records by status")
    public ResponseEntity<List<CriminalRecordResponseDto>> getByStatus(
            @PathVariable CriminalRecordStatus status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @GetMapping("/wanted")
    @Operation(summary = "Get all WANTED entries",
               description = "Returns all records with status = WANTED across all source systems.")
    public ResponseEntity<List<CriminalRecordResponseDto>> getWanted() {
        return ResponseEntity.ok(service.getAllWanted());
    }
}
