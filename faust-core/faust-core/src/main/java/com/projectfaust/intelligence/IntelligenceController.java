package com.projectfaust.intelligence;

import com.projectfaust.intelligence.dto.ConnectionPathResponse;
import com.projectfaust.intelligence.dto.InfluenceMapResponse;
import com.projectfaust.person.dto.PersonConnectionRequest;
import com.projectfaust.person.dto.PersonConnectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for intelligence graph operations within Project Faust.
 *
 * <p>Exposes four endpoints covering the intelligence analytical layer:</p>
 * <ul>
 *   <li>AI-driven subject brief generation.</li>
 *   <li>Influence map — full connection graph centred on a subject.</li>
 *   <li>Path finding — shortest connection between two subjects (BFS).</li>
 *   <li>Connection creation — establishing new graph edges.</li>
 * </ul>
 *
 * <p>All endpoints require at minimum {@code ANALYST} role — intelligence
 * operations are not available to {@code VIEWER} accounts.</p>
 *
 * <p>Base path: {@code /api/v1/intelligence}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/intelligence")
@RequiredArgsConstructor
@Tag(name = "Intelligence Operations",
        description = "AI analysis, influence mapping, and graph traversal")
public class IntelligenceController {

    private final AiAnalystService aiAnalystService;
    private final IntelligenceService intelligenceService;

    /**
     * Generates an AI-driven intelligence brief for a specific subject.
     *
     * @param uuid the public UUID of the person to analyse.
     * @return a natural language intelligence summary.
     */
    @GetMapping("/analyze/{uuid}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Generate AI intelligence brief",
            description = "Produces an AI-generated summary of a subject's connections, risk factors, and influence.")
    public ResponseEntity<String> analyze(@PathVariable UUID uuid) {
        return ResponseEntity.ok(aiAnalystService.getAiIntelligenceBrief(uuid));
    }

    /**
     * Returns the influence map centred on the specified subject.
     *
     * @param uuid the public UUID of the root subject.
     * @return the influence map with all connections normalised around the subject.
     */
    @GetMapping("/influence-map/{uuid}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get influence map",
            description = "Returns the full weighted connection graph for a subject. Connections are normalised so the subject is always the source node.")
    public ResponseEntity<InfluenceMapResponse> getInfluenceMap(@PathVariable UUID uuid) {
        return ResponseEntity.ok(intelligenceService.getInfluenceMap(uuid));
    }

    /**
     * Finds the shortest connection path between two persons using BFS.
     *
     * @param source the public UUID of the starting person.
     * @param target the public UUID of the destination person.
     * @return the shortest path, or {@code found = false} if no path exists within 10 hops.
     */
    @GetMapping("/path")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Find connection path",
            description = "BFS shortest-path between two subjects. Returns degrees of separation and the named path. Capped at 10 hops.")
    public ResponseEntity<ConnectionPathResponse> findPath(
            @RequestParam UUID source,
            @RequestParam UUID target) {
        return ResponseEntity.ok(intelligenceService.findPath(source, target));
    }

    /**
     * Creates a new directed connection between two persons.
     *
     * @param request the connection creation request (validated).
     * @return the created connection with HTTP 201.
     */
    @PostMapping("/connect")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Create person connection",
            description = "Establishes a new directed relationship between two persons in the influence graph.")
    public ResponseEntity<PersonConnectionResponse> connectEntities(
            @Valid @RequestBody PersonConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(intelligenceService.createConnection(request));
    }
}