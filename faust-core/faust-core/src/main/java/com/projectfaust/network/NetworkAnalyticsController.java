package com.projectfaust.network;

import com.projectfaust.network.dto.response.ConnectionPathResponse;
import com.projectfaust.network.dto.response.InfluenceMapResponse;
import com.projectfaust.network.dto.response.NetworkAnalysisResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Intelligence-grade network analytics REST controller for Project Faust.
 *
 * <p>Base path: {@code /api/v1/network}</p>
 *
 * <p>Exposes three classes of HUMINT graph analytics:
 * <ol>
 *   <li><b>Shortest path</b> — minimum-hop chain between two persons
 *       ({@code GET /path}).</li>
 *   <li><b>Influence map</b> — egocentric subgraph with scored, typed,
 *       directional edges for dossier view ({@code GET /{personId}/influence-map}).</li>
 *   <li><b>Network analysis</b> — degree + betweenness centrality, graph density,
 *       community detection for the N-hop neighbourhood
 *       ({@code GET /{personId}/analyse}).</li>
 * </ol>
 * </p>
 *
 * <p>CRUD operations for connection records are in
 * {@link PersonConnectionController} at {@code /api/v1/connections}.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/network")
@RequiredArgsConstructor
@Tag(name = "HUMINT Network Analytics",
     description = "Intelligence-grade graph analytics: shortest path, influence mapping, " +
                   "centrality analysis, and community detection across the HUMINT relationship graph.")
public class NetworkAnalyticsController {

    private final NetworkGraphService graphService;

    // ── Shortest path ─────────────────────────────────────────────────────────

    /**
     * Finds the shortest undirected path between two persons in the HUMINT graph.
     *
     * <p>Uses BFS with a 6-hop limit (six degrees of separation). The path is
     * undirected — connections are traversable in either direction.</p>
     *
     * <p><b>Intelligence use-case:</b> "Is Subject A connected to Subject B through
     * fewer than 3 intermediaries?" — low hop count + high influence score along
     * the path indicates a meaningful proximity link.</p>
     */
    @GetMapping("/path")
    @Operation(
            summary = "Find shortest path between two persons",
            description = "BFS shortest path (undirected), max 6 hops. " +
                          "Returns path nodes, edges, hop count, and cumulative influence score. " +
                          "pathFound=false when no path exists within 6 hops. " +
                          "A totalInfluenceScore near 1.0 indicates a strong, direct chain.")
    public ResponseEntity<ConnectionPathResponse> findShortestPath(
            @Parameter(description = "Public UUID of the source person")
            @RequestParam UUID sourcePersonId,
            @Parameter(description = "Public UUID of the target person")
            @RequestParam UUID targetPersonId) {
        return ResponseEntity.ok(graphService.findShortestPath(sourcePersonId, targetPersonId));
    }

    // ── Influence map ─────────────────────────────────────────────────────────

    /**
     * Returns the influence map (egocentric network) for a subject person.
     *
     * <p>All direct connections (1-hop) are returned with their weighted influence
     * score, direction classification, and the target's current role. Top-5
     * influencers are pre-computed for the dossier sidebar.</p>
     *
     * <p><b>Intelligence use-case:</b> Primary view for the intelligence dossier —
     * shows who this person is connected to, how strongly, and in which direction.</p>
     */
    @GetMapping("/{personId}/influence-map")
    @Operation(
            summary = "Get influence map for a person",
            description = "Egocentric network — all direct connections with weighted scores, " +
                          "direction (OUTGOING/INCOMING/BIDIRECTIONAL), and current roles. " +
                          "Sorted by weighted influence score descending. " +
                          "Top 5 influencers are pre-extracted for dossier sidebar.")
    public ResponseEntity<InfluenceMapResponse> getInfluenceMap(
            @Parameter(description = "Public UUID of the subject person")
            @PathVariable UUID personId) {
        return ResponseEntity.ok(graphService.buildInfluenceMap(personId));
    }

    // ── Network analysis ──────────────────────────────────────────────────────

    /**
     * Performs full SNA (Social Network Analysis) on the N-hop subgraph.
     *
     * <p>Computes degree centrality, betweenness centrality (Brandes' algorithm),
     * influence centrality, composite risk scores, graph density, and cluster
     * detection via Union-Find.</p>
     *
     * <p><b>Depth guidance:</b>
     * <ul>
     *   <li>{@code depth=1} — direct connections only; fast.</li>
     *   <li>{@code depth=2} — 2-hop neighbourhood; recommended for standard analysis.</li>
     *   <li>{@code depth=3} — extended network; may be slow for highly connected persons.</li>
     * </ul>
     * </p>
     *
     * <p><b>Intelligence use-case:</b> Identify key brokers (high betweenness),
     * hidden clusters, and peripheral assets (low degree) within the subject's network.</p>
     */
    @GetMapping("/{personId}/analyse")
    @Operation(
            summary = "Full network analysis — centrality, density, clusters",
            description = "Analyses the N-hop subgraph rooted at the subject. " +
                          "Returns degree + betweenness centrality, influence scores, " +
                          "composite risk scores, graph density, and community clusters. " +
                          "Key brokers (top-3 betweenness) and isolated nodes are extracted. " +
                          "Risk score > 0.7 flags the node for analyst review. " +
                          "Default depth = 2.")
    public ResponseEntity<NetworkAnalysisResponse> analyseNetwork(
            @Parameter(description = "Public UUID of the subject person")
            @PathVariable UUID personId,
            @Parameter(description = "Hop radius for subgraph expansion (1–3, default 2)")
            @RequestParam(defaultValue = "2") int depth) {

        if (depth < 1 || depth > 3) {
            throw new IllegalArgumentException("Analysis depth must be between 1 and 3.");
        }

        return ResponseEntity.ok(graphService.analyseNetwork(personId, depth));
    }
}
