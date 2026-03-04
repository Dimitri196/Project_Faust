package com.projectfaust.controller;

import com.projectfaust.dto.request.PersonConnectionRequest;
import com.projectfaust.dto.response.ConnectionPathResponse;
import com.projectfaust.dto.response.InfluenceMapResponse;
import com.projectfaust.dto.response.PersonConnectionResponse;
import com.projectfaust.service.AiAnalystService;
import com.projectfaust.service.IntelligenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for high-level intelligence operations and network analysis.
 * Provides endpoints for AI-generated dossiers, influence mapping, and shortest-path
 * discovery between entities in the Faust network.
 */
@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
public class IntelligenceController {

    private final AiAnalystService aiAnalystService;
    private final IntelligenceService intelligenceService;

    /**
     * Generates an AI-driven intelligence brief for a specific subject.
     * Summarizes known connections, risk factors, and institutional influence.
     *
     * @param uuid The unique identifier of the person to analyze.
     * @return A natural language intelligence summary.
     */
    @GetMapping("/analyze/{uuid}")
    public ResponseEntity<String> analyze(@PathVariable UUID uuid) {
        return ResponseEntity.ok(aiAnalystService.getAiIntelligenceBrief(uuid));
    }

    /**
     * Retrieves the visual and statistical influence map of a subject.
     * Maps the weight of connections and institutional reach.
     *
     * @param uuid The unique identifier of the central subject.
     * @return A map structure representing the subject's network of influence.
     */
    @GetMapping("/influence-map/{uuid}")
    public ResponseEntity<InfluenceMapResponse> getInfluenceMap(@PathVariable UUID uuid) {
        return ResponseEntity.ok(intelligenceService.getInfluenceMap(uuid));
    }

    /**
     * Discovers the connection path (degrees of separation) between two entities.
     * Utilizes graph traversal to find how two individuals are linked through
     * shared institutions or mutual connections.
     *
     * @param source The UUID of the starting subject.
     * @param target The UUID of the destination subject.
     * @return The shortest path or degrees of separation between the two subjects.
     */
    @GetMapping("/path")
    public ResponseEntity<ConnectionPathResponse> findPath(
            @RequestParam UUID source,
            @RequestParam UUID target) {
        return ResponseEntity.ok(intelligenceService.findPath(source, target));
    }

    /**
     * Establishes a new formal or informal link between two subjects in the database.
     *
     * @param request Data containing source, target, relationship type, and weight.
     * @return The created connection metadata.
     */
    @PostMapping("/connect")
    public ResponseEntity<PersonConnectionResponse> connectEntities(@Valid @RequestBody PersonConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(intelligenceService.createConnection(request));
    }
}
