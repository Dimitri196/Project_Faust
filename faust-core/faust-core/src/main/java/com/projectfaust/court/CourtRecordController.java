package com.projectfaust.court;

import com.projectfaust.court.dto.CourtRecordRequestDto;
import com.projectfaust.court.dto.CourtRecordResponseDto;
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
 * REST controller for court record management within Project Faust.
 *
 * <p>Base path: {@code /api/v1/court-records}</p>
 *
 * <p>The network-analysis endpoint {@code /shared-proceedings} is exposed here
 * rather than under a separate analytics path, keeping all court intelligence
 * in one controller.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/court-records")
@RequiredArgsConstructor
@Tag(name = "Court Records",
     description = "HUMINT judicial proceedings — parties, outcomes, cross-references to criminal records.")
public class CourtRecordController {

    private final CourtRecordService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Ingest a court record",
               description = "Upserts on (sourceSystem, sourceReferenceId) if supplied. " +
                             "Party list is fully replaced on each upsert.")
    public ResponseEntity<CourtRecordResponseDto> create(
            @Valid @RequestBody CourtRecordRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk-ingest court records")
    public ResponseEntity<List<CourtRecordResponseDto>> createBulk(
            @Valid @RequestBody List<CourtRecordRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBulk(dtos));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{publicId}")
    @Operation(summary = "Update a court record",
               description = "Partial update on scalar fields. " +
                             "If `parties` is supplied in the body it fully replaces the existing list.")
    public ResponseEntity<CourtRecordResponseDto> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody CourtRecordRequestDto dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a court record (cascades to its party rows)")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by ID ──────────────────────────────────────────────────────────

    @GetMapping("/{publicId}")
    @Operation(summary = "Get a court record by public ID")
    public ResponseEntity<CourtRecordResponseDto> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // ── Read — by party (person) ──────────────────────────────────────────────

    @GetMapping("/by-party-person/{personPublicId}")
    @Operation(summary = "Get all proceedings in which a person appeared (any role)")
    public ResponseEntity<List<CourtRecordResponseDto>> getByPartyPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getByPartyPerson(personPublicId));
    }

    @GetMapping("/by-party-person/{personPublicId}/role/{role}")
    @Operation(summary = "Get proceedings in which a person appeared in a specific role",
               description = "e.g. DEFENDANT, VICTIM, WITNESS, EXPERT_WITNESS")
    public ResponseEntity<List<CourtRecordResponseDto>> getByPartyPersonAndRole(
            @PathVariable UUID personPublicId,
            @PathVariable PartyRole role) {
        return ResponseEntity.ok(service.getByPartyPersonAndRole(personPublicId, role));
    }

    // ── Read — by party (institution) ─────────────────────────────────────────

    @GetMapping("/by-party-institution/{institutionPublicId}")
    @Operation(summary = "Get all proceedings in which an institution appeared (any role)")
    public ResponseEntity<List<CourtRecordResponseDto>> getByPartyInstitution(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getByPartyInstitution(institutionPublicId));
    }

    @GetMapping("/by-party-institution/{institutionPublicId}/role/{role}")
    @Operation(summary = "Get proceedings in which an institution appeared in a specific role")
    public ResponseEntity<List<CourtRecordResponseDto>> getByPartyInstitutionAndRole(
            @PathVariable UUID institutionPublicId,
            @PathVariable PartyRole role) {
        return ResponseEntity.ok(service.getByPartyInstitutionAndRole(institutionPublicId, role));
    }

    // ── Read — by proceeding attributes ───────────────────────────────────────

    @GetMapping("/by-type/{proceedingType}")
    @Operation(summary = "Get court records by proceeding type",
               description = "e.g. CRIMINAL, CIVIL, COMMERCIAL, ADMINISTRATIVE")
    public ResponseEntity<List<CourtRecordResponseDto>> getByProceedingType(
            @PathVariable ProceedingType proceedingType) {
        return ResponseEntity.ok(service.getByProceedingType(proceedingType));
    }

    @GetMapping("/by-outcome/{outcome}")
    @Operation(summary = "Get court records by outcome")
    public ResponseEntity<List<CourtRecordResponseDto>> getByOutcome(
            @PathVariable CourtOutcome outcome) {
        return ResponseEntity.ok(service.getByOutcome(outcome));
    }

    // ── Network / intelligence query ──────────────────────────────────────────

    @GetMapping("/shared-proceedings")
    @Operation(summary = "Find proceedings shared by two persons",
               description = "Returns all court records in which both persons appeared together " +
                             "(regardless of role). Used for co-offender / network analysis.")
    public ResponseEntity<List<CourtRecordResponseDto>> getSharedProceedings(
            @RequestParam UUID personAPublicId,
            @RequestParam UUID personBPublicId) {
        return ResponseEntity.ok(service.getSharedProceedings(personAPublicId, personBPublicId));
    }
}
