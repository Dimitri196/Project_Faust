package com.projectfaust.institution;

import com.projectfaust.institution.dto.InstitutionalEvolutionRequest;
import com.projectfaust.institution.dto.InstitutionalEvolutionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller pro správu historické kontinuity a nástupnictví institucí v projektu Faust.
 * Umožňuje sledovat "životní cyklus" úřadů, jejich transformace, slučování a zánik.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/institutional-evolutions")
@RequiredArgsConstructor
@Tag(name = "Institutional Evolution", description = "Operations for tracking institutional lineage and historical transformations")
public class InstitutionalEvolutionController {

    private final InstitutionalEvolutionService service;

    @PostMapping
    @Operation(summary = "Record an evolution event",
            description = "Creates a link between a predecessor and a successor institution. Automatically handles deactivation of the predecessor if applicable.")
    public ResponseEntity<InstitutionalEvolutionResponse> create(@Valid @RequestBody InstitutionalEvolutionRequest request) {
        log.info("FAUST_API: Recording single evolution link for predecessor: {}", request.predecessorExternalId());
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk record evolution events",
            description = "Useful for historical migrations or large-scale administrative reorganizations (e.g., 1993 transition).")
    public ResponseEntity<List<InstitutionalEvolutionResponse>> createBulk(@Valid @RequestBody List<InstitutionalEvolutionRequest> requests) {
        log.info("FAUST_API: Processing bulk evolution import ({} items)", requests.size());
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Vrací kompletní časovou osu pro danou instituci.
     * V grafu Network Hud se používá k vykreslení šipek "před" a "po".
     */
    @GetMapping("/timeline/{institutionId}")
    @Operation(summary = "Get institution timeline",
            description = "Returns a chronological list of all evolutionary events associated with the given institution UUID.")
    public ResponseEntity<List<InstitutionalEvolutionResponse>> getTimeline(@PathVariable UUID institutionId) {
        log.info("FAUST_API: Fetching evolution timeline for institution: {}", institutionId);
        return ResponseEntity.ok(service.getTimeline(institutionId));
    }

    /**
     * Vyhledá přímé předchůdce dané instituce.
     * Pomáhá identifikovat "původ" úřadu při zkoumání historických spisů v MEPHISTO_OS.
     */
    @GetMapping("/predecessors/{successorId}")
    @Operation(summary = "Get direct predecessors",
            description = "Returns a list of institutions that were directly succeeded by the specified institution.")
    public ResponseEntity<List<InstitutionalEvolutionResponse>> getPredecessors(@PathVariable UUID successorId) {
        log.info("FAUST_API: Resolving predecessors for successor: {}", successorId);

        // Filtrujeme timeline pouze na záznamy, kde je instituce v roli nástupce

        List<InstitutionalEvolutionResponse> predecessors = service.getTimeline(successorId).stream()
                .filter(e -> e.successorExternalId() != null && e.successorExternalId().equals(successorId))
                .toList();
        return ResponseEntity.ok(predecessors);
    }
}