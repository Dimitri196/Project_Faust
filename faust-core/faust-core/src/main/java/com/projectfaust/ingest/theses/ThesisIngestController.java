package com.projectfaust.ingest.theses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for academic publication lookup via Semantic Scholar.
 *
 * <p>Two endpoints:</p>
 * <ul>
 *   <li><b>GET /{personId}/publications</b> — fetches and returns the full
 *       publication list synchronously. Suitable for dossier display.</li>
 *   <li><b>POST /{personId}/trigger</b> — fires the fetch asynchronously
 *       and returns immediately with an acknowledgement. Suitable for
 *       background enrichment.</li>
 * </ul>
 *
 * <p>Base path: {@code /api/v1/ingest/theses}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/ingest/theses")
@RequiredArgsConstructor
@Tag(name = "Academic Publications",
        description = "Semantic Scholar-powered academic publication lookup for FAUST persons")
public class ThesisIngestController {

    private final ThesisIngestService ingestService;

    /**
     * Fetches and returns all academic publications found for the given person.
     *
     * <p>Calls Semantic Scholar synchronously — may take 1–3 seconds.
     * Results are not yet persisted (v1) — each call hits the external API.
     * Persistence will be added in a subsequent iteration.</p>
     *
     * @param personId the public UUID of the target FAUST Person.
     * @return list of matched publications from Semantic Scholar.
     */
    @GetMapping("/{personId}/publications")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Fetch academic publications for a person",
            description = "Searches Semantic Scholar by name and returns all matched publications. " +
                    "ANALYST access required — external API call, results not yet persisted.")
    public ResponseEntity<List<AcademicPublicationResponse>> getPublications(
            @PathVariable UUID personId) {
        return ResponseEntity.ok(ingestService.fetchAndReturnPublications(personId));
    }

    /**
     * Triggers an async academic publication fetch for the given person
     * and returns immediately.
     *
     * <p>Mirrors the original {@code ThesisIngestController} trigger pattern.
     * In a future iteration this will dispatch to Kafka so the controller
     * returns before the Semantic Scholar HTTP call completes.</p>
     *
     * @param personId the public UUID of the target FAUST Person.
     * @return acknowledgement string with the person's name.
     */
    @PostMapping("/{personId}/trigger")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Trigger async publication fetch",
            description = "Fires a Semantic Scholar lookup for this person and returns immediately. " +
                    "ANALYST access required.")
    public ResponseEntity<String> triggerFetch(@PathVariable UUID personId) {
        var result = ingestService.fetchAndReturnPublications(personId);
        return ResponseEntity.accepted()
                .body("Academic publication lookup complete — " + result.size() + " results found.");
    }
}