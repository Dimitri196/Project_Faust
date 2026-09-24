package com.projectfaust.medical;

import com.projectfaust.medical.dto.MedicalRecordRequestDto;
import com.projectfaust.medical.dto.MedicalRecordResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for medical intelligence records within Project Faust.
 *
 * <p>Base path: {@code /api/v1/medical-records}</p>
 *
 * <p><b>Clearance note:</b> all endpoints in this controller require at minimum
 * {@code LEVEL_4_SECRET} access. Clearance enforcement is handled by the
 * Spring Security layer; this controller performs no access-control checks.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/medical-records")
@RequiredArgsConstructor
@Tag(name = "Medical Records",
     description = "HUMINT medical intelligence — clinical profiles, forensic reports, " +
                   "fitness assessments. Highest clearance module (LEVEL_4_SECRET default).")
public class MedicalRecordController {

    private final MedicalRecordService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Ingest a medical record",
               description = "Upserts on (sourceSystem, sourceReferenceId) if supplied. " +
                             "Clearance defaults to LEVEL_4_SECRET unless overridden in body.")
    public ResponseEntity<MedicalRecordResponseDto> create(
            @Valid @RequestBody MedicalRecordRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk-ingest medical records")
    public ResponseEntity<List<MedicalRecordResponseDto>> createBulk(
            @Valid @RequestBody List<MedicalRecordRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBulk(dtos));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{publicId}")
    @Operation(summary = "Partially update a medical record")
    public ResponseEntity<MedicalRecordResponseDto> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody MedicalRecordRequestDto dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a medical record")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by ID ──────────────────────────────────────────────────────────

    @GetMapping("/{publicId}")
    @Operation(summary = "Get a medical record by public ID")
    public ResponseEntity<MedicalRecordResponseDto> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // ── Read — by person ──────────────────────────────────────────────────────

    @GetMapping("/by-person/{personPublicId}")
    @Operation(summary = "Get all medical records for a person (all types)")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getBySubjectPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/type/{recordType}")
    @Operation(summary = "Get medical records for a person filtered by record type",
               description = "e.g. CLINICAL_PROFILE, FORENSIC_EXAMINATION, TOXICOLOGY_REPORT")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByPersonAndType(
            @PathVariable UUID personPublicId,
            @PathVariable MedicalRecordType recordType) {
        return ResponseEntity.ok(service.getBySubjectPersonAndType(personPublicId, recordType));
    }

    @GetMapping("/by-person/{personPublicId}/fitness/active")
    @Operation(summary = "Get non-expired FITNESS_FOR_DUTY assessments for a person",
               description = "Returns assessments where expiryDate is null or in the future.")
    public ResponseEntity<List<MedicalRecordResponseDto>> getActiveFitnessAssessments(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getActiveFitnessAssessments(personPublicId));
    }

    // ── Read — by institution ─────────────────────────────────────────────────

    @GetMapping("/by-institution/{institutionPublicId}")
    @Operation(summary = "Get all medical records for an institution",
               description = "Facility inspections, quarantine records, hospital accreditations.")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByInstitution(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getBySubjectInstitution(institutionPublicId));
    }

    // ── Read — by type / category ─────────────────────────────────────────────

    @GetMapping("/by-type/{recordType}")
    @Operation(summary = "Get medical records by type")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByRecordType(
            @PathVariable MedicalRecordType recordType) {
        return ResponseEntity.ok(service.getByRecordType(recordType));
    }

    @GetMapping("/by-condition-category/{category}")
    @Operation(summary = "Get medical records by condition category",
               description = "e.g. SUBSTANCE_ABUSE, PSYCHIATRIC, TRAUMA")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByConditionCategory(
            @PathVariable MedicalConditionCategory category) {
        return ResponseEntity.ok(service.getByConditionCategory(category));
    }

    // ── Read — ICD code search ────────────────────────────────────────────────

    @GetMapping("/by-icd")
    @Operation(summary = "Get medical records by ICD-10/11 code prefix",
               description = "Prefix search — e.g. 'F20' returns all F20.x codes.")
    public ResponseEntity<List<MedicalRecordResponseDto>> getByIcdCodePrefix(
            @RequestParam String icdPrefix) {
        return ResponseEntity.ok(service.getByIcdCodePrefix(icdPrefix));
    }
}
