package com.projectfaust.person.document;

import com.projectfaust.person.document.dto.PersonDocumentRequest;
import com.projectfaust.person.document.dto.PersonDocumentResponse;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for person document intelligence records.
 *
 * <p>Base path: {@code /api/v1/persons/{personId}/documents}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/persons/{personId}/documents")
@RequiredArgsConstructor
@Tag(name = "Person Documents", description = "Identity document intelligence records")
public class PersonDocumentController {

    private final PersonDocumentService service;

    // ── Manual entry ──────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Add document record manually")
    public ResponseEntity<PersonDocumentResponse> add(
            @PathVariable UUID personId,
            @Valid @RequestBody PersonDocumentRequest request) {
        return ResponseEntity.ok(service.addDocument(request));
    }

    // ── ARES registry pull ────────────────────────────────────────────────────

    @PostMapping("/ingest/ares/{ico}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(
            summary = "Ingest IČO from ARES",
            description = "Fetches company/entity data from Czech ARES registry " +
                    "and creates IČO + DIČ document records automatically.")
    public ResponseEntity<PersonDocumentResponse> ingestFromAres(
            @PathVariable UUID personId,
            @PathVariable String ico) {
        return ResponseEntity.ok(service.ingestFromAres(ico, personId));
    }

    // ── MRZ parsing ───────────────────────────────────────────────────────────

    @PostMapping("/ingest/mrz")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(
            summary = "Parse MRZ from travel document",
            description = "Parses ICAO 9303 MRZ string (passport or ID card) " +
                    "and creates a document record with extracted fields.")
    public ResponseEntity<PersonDocumentResponse> ingestFromMrz(
            @PathVariable UUID personId,
            @RequestParam String mrz,
            @RequestParam(defaultValue = "LEVEL_2_INTERNAL") ClearanceLevel clearanceLevel) {
        return ResponseEntity.ok(service.ingestFromMrz(mrz, personId, clearanceLevel));
    }

    // ── Scan upload ───────────────────────────────────────────────────────────

    @PostMapping(value = "/{documentId}/scan",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(
            summary = "Attach document scan",
            description = "Uploads a scan (PDF/JPG/PNG) and links it to the document record.")
    public ResponseEntity<PersonDocumentResponse> attachScan(
            @PathVariable UUID personId,
            @PathVariable UUID documentId,
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(service.attachScan(documentId, file));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get all documents for a person")
    public ResponseEntity<List<PersonDocumentResponse>> getAll(
            @PathVariable UUID personId,
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(activeOnly
                ? service.getActiveByPerson(personId)
                : service.getByPerson(personId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Search by document number across all persons")
    public ResponseEntity<List<PersonDocumentResponse>> search(
            @PathVariable UUID personId,
            @RequestParam String number,
            @RequestParam DocumentType type) {
        return ResponseEntity.ok(service.findByDocumentNumber(number, type));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Deactivate a document record")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID personId,
            @PathVariable UUID documentId) {
        service.deactivate(documentId);
        return ResponseEntity.noContent().build();
    }
}
