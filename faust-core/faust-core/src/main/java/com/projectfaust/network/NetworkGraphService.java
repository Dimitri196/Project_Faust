package com.projectfaust.network;

import com.projectfaust.network.dto.response.ConnectionPathResponse;
import com.projectfaust.network.dto.response.InfluenceMapResponse;
import com.projectfaust.network.dto.response.InfluenceMapResponse.Direction;
import com.projectfaust.network.dto.response.InfluenceMapResponse.InfluenceNode;
import com.projectfaust.network.dto.response.NetworkAnalysisResponse;
import com.projectfaust.network.dto.response.NetworkAnalysisResponse.CentralityScore;
import com.projectfaust.network.dto.response.NetworkAnalysisResponse.NetworkCluster;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonConnection;
import com.projectfaust.person.PersonConnectionRepository;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.shared.enums.ConnectionType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Intelligence-grade graph analytics engine for Project Faust.
 *
 * <p>Implements the core algorithms of HUMINT network analysis:
 * <ul>
 *   <li><b>BFS shortest-path</b> — finds the minimum-hop chain between two persons,
 *       treating the graph as undirected (a → b or b → a both count as adjacency).</li>
 *   <li><b>Influence map</b> — egocentric subgraph with weighted, typed, directional
 *       edges sorted by composite influence score.</li>
 *   <li><b>Network analysis</b> — degree + betweenness centrality, graph density,
 *       community/cluster detection via Union-Find on the N-hop neighbourhood.</li>
 * </ul>
 * </p>
 *
 * <p>All graph operations are performed in-memory after loading the relevant
 * subgraph from the database. For graphs larger than ~10 000 nodes, replace
 * the in-memory adjacency map with a dedicated graph DB (Neo4j) — the service
 * interface remains the same.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NetworkGraphService {

    private static final int DEFAULT_ANALYSIS_DEPTH = 2;
    private static final int MAX_BFS_DEPTH          = 6; // six degrees of separation

    private final PersonConnectionRepository connectionRepository;
    private final PersonRepository           personRepository;

    // =========================================================================
    // Shortest path — BFS
    // =========================================================================

    /**
     * Finds the shortest undirected path between two persons using BFS.
     *
     * <p>The graph is treated as undirected: if a connection exists in either
     * direction between A and B, both can traverse it. This mirrors real HUMINT
     * analysis where a "knows" relationship is typically bidirectional regardless
     * of who initiated it.</p>
     *
     * <p>Loads the relevant portion of the graph lazily — starts from the source
     * and expands layer by layer, stopping as soon as the target is reached or
     * {@value #MAX_BFS_DEPTH} hops are exhausted.</p>
     *
     * @param sourcePersonId public UUID of the starting person.
     * @param targetPersonId public UUID of the destination person.
     * @return a {@link ConnectionPathResponse} with path details, or {@code pathFound=false}.
     */
    public ConnectionPathResponse findShortestPath(UUID sourcePersonId, UUID targetPersonId) {
        log.info("FAUST_NETWORK_PATH: source={} target={}", sourcePersonId, targetPersonId);

        verifyPersonExists(sourcePersonId);
        verifyPersonExists(targetPersonId);

        if (sourcePersonId.equals(targetPersonId)) {
            return buildNoPathResponse(sourcePersonId, targetPersonId);
        }

        // BFS state
        Map<UUID, UUID>              parent     = new LinkedHashMap<>(); // child → parent
        Map<UUID, PersonConnection>  edgeUsed   = new HashMap<>();        // child → edge that reached it
        Queue<UUID>                  queue      = new LinkedList<>();
        Set<UUID>                    visited    = new HashSet<>();

        queue.add(sourcePersonId);
        visited.add(sourcePersonId);
        parent.put(sourcePersonId, null);

        boolean found = false;
        int     depth = 0;

        outer:
        while (!queue.isEmpty() && depth < MAX_BFS_DEPTH) {
            int layerSize = queue.size();
            depth++;
            for (int i = 0; i < layerSize; i++) {
                UUID current = queue.poll();
                List<PersonConnection> edges = connectionRepository.findAllByPersonExternalId(current);
                for (PersonConnection edge : edges) {
                    UUID neighbour = getNeighbour(edge, current);
                    if (!visited.contains(neighbour)) {
                        visited.add(neighbour);
                        parent.put(neighbour, current);
                        edgeUsed.put(neighbour, edge);
                        if (neighbour.equals(targetPersonId)) {
                            found = true;
                            break outer;
                        }
                        queue.add(neighbour);
                    }
                }
            }
        }

        if (!found) {
            log.info("FAUST_NETWORK_PATH_NOT_FOUND: source={} target={}", sourcePersonId, targetPersonId);
            return buildNoPathResponse(sourcePersonId, targetPersonId);
        }

        return reconstructPath(sourcePersonId, targetPersonId, parent, edgeUsed);
    }

    // =========================================================================
    // Influence map — egocentric subgraph
    // =========================================================================

    /**
     * Builds the influence map (egocentric network) for a given subject.
     *
     * <p>Returns all directly-connected persons (1-hop neighbours) with:
     * <ul>
     *   <li>directional classification (OUTGOING / INCOMING / BIDIRECTIONAL)</li>
     *   <li>weighted score = {@code influenceScore × connectionType.defaultWeight}</li>
     *   <li>connection type summary for the dossier sidebar</li>
     *   <li>top-5 influencers pre-computed</li>
     * </ul>
     * </p>
     *
     * @param subjectPersonId public UUID of the ego node.
     * @return fully populated {@link InfluenceMapResponse}.
     */
    public InfluenceMapResponse buildInfluenceMap(UUID subjectPersonId) {
        log.info("FAUST_NETWORK_INFLUENCE_MAP: subject={}", subjectPersonId);

        Person subject = personRepository.findByExternalId(subjectPersonId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + subjectPersonId));

        List<PersonConnection> allEdges = connectionRepository.findAllByPersonExternalId(subjectPersonId);

        // Build directional edge index: neighbour UUID → {outgoing edges, incoming edges}
        Map<UUID, List<PersonConnection>> outgoing = allEdges.stream()
                .filter(e -> e.getSourcePerson().getExternalId().equals(subjectPersonId))
                .collect(Collectors.groupingBy(e -> e.getTargetPerson().getExternalId()));

        Map<UUID, List<PersonConnection>> incoming = allEdges.stream()
                .filter(e -> e.getTargetPerson().getExternalId().equals(subjectPersonId))
                .collect(Collectors.groupingBy(e -> e.getSourcePerson().getExternalId()));

        Set<UUID> allNeighbours = new HashSet<>();
        allNeighbours.addAll(outgoing.keySet());
        allNeighbours.addAll(incoming.keySet());

        List<InfluenceNode> nodes = allNeighbours.stream()
                .map(neighbourId -> buildInfluenceNode(neighbourId, outgoing, incoming))
                .sorted(Comparator.comparingDouble(InfluenceNode::weightedScore).reversed())
                .toList();

        long activeCount = allEdges.stream()
                .filter(e -> e.getEndDate() == null || e.getEndDate().isAfter(LocalDate.now()))
                .count();

        Map<ConnectionType, Long> typeSummary = allEdges.stream()
                .collect(Collectors.groupingBy(PersonConnection::getConnectionType, Collectors.counting()));

        List<InfluenceNode> topInfluencers = nodes.stream().limit(5).toList();

        return new InfluenceMapResponse(
                subjectPersonId,
                subject.getFullName(),
                allEdges.size(),
                (int) activeCount,
                nodes,
                typeSummary,
                topInfluencers
        );
    }

    // =========================================================================
    // Network analysis — centrality + clusters
    // =========================================================================

    /**
     * Performs full network analysis on the N-hop subgraph rooted at the subject.
     *
     * <p>Algorithm outline:
     * <ol>
     *   <li>Expand the subgraph via BFS up to {@code depth} hops from the subject.</li>
     *   <li>Compute degree centrality (normalised by n-1).</li>
     *   <li>Compute betweenness centrality via Brandes' algorithm (O(VE) — acceptable
     *       for subgraphs up to ~1 000 nodes; replace with approximate Kadabra for larger).</li>
     *   <li>Compute influence centrality: sum of weighted incoming edge scores per node.</li>
     *   <li>Composite risk score: 0.4×degree + 0.4×betweenness + 0.2×influence (normalised).</li>
     *   <li>Cluster detection via Union-Find on connected components.</li>
     * </ol>
     * </p>
     *
     * @param subjectPersonId public UUID of the ego node.
     * @param depth           hop radius for subgraph expansion (default {@value #DEFAULT_ANALYSIS_DEPTH}).
     * @return {@link NetworkAnalysisResponse} with full metrics.
     */
    public NetworkAnalysisResponse analyseNetwork(UUID subjectPersonId, int depth) {
        log.info("FAUST_NETWORK_ANALYSE: subject={} depth={}", subjectPersonId, depth);
        verifyPersonExists(subjectPersonId);

        // 1. Build N-hop subgraph
        Map<UUID, Set<UUID>>            adjacency  = new LinkedHashMap<>();
        Map<String, PersonConnection>   edgeIndex  = new HashMap<>();   // "src:tgt" → edge
        Map<UUID, Person>               personIndex = new HashMap<>();

        expandSubgraph(subjectPersonId, depth, adjacency, edgeIndex, personIndex);

        int n = adjacency.size();
        int e = edgeIndex.size() / 2; // undirected count (each edge stored twice)

        double density = n <= 1 ? 0.0 : (2.0 * e) / (n * (n - 1));

        // 2. Degree centrality
        Map<UUID, Double> degreeCentrality = new HashMap<>();
        for (Map.Entry<UUID, Set<UUID>> entry : adjacency.entrySet()) {
            degreeCentrality.put(entry.getKey(),
                    n <= 1 ? 0.0 : (double) entry.getValue().size() / (n - 1));
        }

        // 3. Betweenness centrality — Brandes' algorithm
        Map<UUID, Double> betweenness = computeBetweenness(adjacency);

        // 4. Influence centrality — sum of weighted incoming scores
        Map<UUID, Double> influenceCentrality = computeInfluenceCentrality(edgeIndex, adjacency.keySet());

        // Normalise influence
        double maxInfluence = influenceCentrality.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1.0);
        if (maxInfluence == 0) maxInfluence = 1.0;
        final double normFactor = maxInfluence;

        // 5. Build CentralityScore list
        List<UUID> nodeIds = new ArrayList<>(adjacency.keySet());
        List<CentralityScore> scores = nodeIds.stream().map(id -> {
            double deg  = degreeCentrality.getOrDefault(id, 0.0);
            double bet  = betweenness.getOrDefault(id, 0.0);
            double inf  = influenceCentrality.getOrDefault(id, 0.0) / normFactor;
            double risk = 0.4 * deg + 0.4 * bet + 0.2 * inf;
            Person p    = personIndex.get(id);
            return new CentralityScore(id,
                    p != null ? p.getFullName() : "UNKNOWN",
                    deg, bet, inf, risk);
        }).sorted(Comparator.comparingDouble(CentralityScore::betweennessCentrality).reversed())
          .toList();

        List<CentralityScore> keyBrokers = scores.stream().limit(3).toList();

        List<UUID> isolatedNodes = adjacency.entrySet().stream()
                .filter(e2 -> e2.getValue().size() <= 1)
                .map(Map.Entry::getKey)
                .toList();

        // 6. Cluster detection — Union-Find
        List<NetworkCluster> clusters = detectClusters(adjacency, edgeIndex, personIndex);

        return new NetworkAnalysisResponse(
                subjectPersonId, depth, n, e,
                density, scores, clusters, keyBrokers, isolatedNodes
        );
    }

    // =========================================================================
    // Private — graph expansion
    // =========================================================================

    /**
     * BFS subgraph expansion up to {@code maxDepth} hops.
     * Populates adjacency map, edge index, and person lookup.
     */
    private void expandSubgraph(
            UUID rootId, int maxDepth,
            Map<UUID, Set<UUID>> adjacency,
            Map<String, PersonConnection> edgeIndex,
            Map<UUID, Person> personIndex) {

        Queue<UUID> queue   = new LinkedList<>();
        Map<UUID, Integer> depthMap = new HashMap<>();

        queue.add(rootId);
        depthMap.put(rootId, 0);
        adjacency.put(rootId, new HashSet<>());

        personRepository.findByExternalId(rootId).ifPresent(p -> personIndex.put(rootId, p));

        while (!queue.isEmpty()) {
            UUID current      = queue.poll();
            int  currentDepth = depthMap.get(current);
            if (currentDepth >= maxDepth) continue;

            List<PersonConnection> edges = connectionRepository.findAllByPersonExternalId(current);
            for (PersonConnection edge : edges) {
                UUID neighbour = getNeighbour(edge, current);

                adjacency.computeIfAbsent(current,   k -> new HashSet<>()).add(neighbour);
                adjacency.computeIfAbsent(neighbour, k -> new HashSet<>()).add(current);

                // Store both directions in edge index for lookups
                edgeIndex.put(current + ":" + neighbour, edge);
                edgeIndex.put(neighbour + ":" + current, edge);

                if (!depthMap.containsKey(neighbour)) {
                    depthMap.put(neighbour, currentDepth + 1);
                    queue.add(neighbour);
                    personRepository.findByExternalId(neighbour)
                            .ifPresent(p -> personIndex.put(neighbour, p));
                }
            }
        }
    }

    // =========================================================================
    // Private — Brandes betweenness centrality
    // =========================================================================

    /**
     * Brandes' algorithm for betweenness centrality on undirected graphs.
     * Time complexity: O(VE). Returns normalised scores in [0, 1].
     *
     * <p>Reference: Brandes, U. (2001). A faster algorithm for betweenness centrality.
     * Journal of Mathematical Sociology, 25(2), 163–177.</p>
     */
    private Map<UUID, Double> computeBetweenness(Map<UUID, Set<UUID>> adjacency) {
        Map<UUID, Double> betweenness = new HashMap<>();
        List<UUID> nodes = new ArrayList<>(adjacency.keySet());
        for (UUID node : nodes) betweenness.put(node, 0.0);

        for (UUID source : nodes) {
            // BFS from source
            Stack<UUID>              stack   = new Stack<>();
            Map<UUID, List<UUID>>    pred    = new HashMap<>();
            Map<UUID, Double>        sigma   = new HashMap<>();
            Map<UUID, Integer>       dist    = new HashMap<>();

            for (UUID n : nodes) { pred.put(n, new ArrayList<>()); sigma.put(n, 0.0); dist.put(n, -1); }
            sigma.put(source, 1.0);
            dist.put(source, 0);

            Queue<UUID> q = new LinkedList<>();
            q.add(source);

            while (!q.isEmpty()) {
                UUID v = q.poll();
                stack.push(v);
                for (UUID w : adjacency.getOrDefault(v, Collections.emptySet())) {
                    if (dist.get(w) < 0) {
                        q.add(w);
                        dist.put(w, dist.get(v) + 1);
                    }
                    if (dist.get(w) == dist.get(v) + 1) {
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        pred.get(w).add(v);
                    }
                }
            }

            // Accumulation
            Map<UUID, Double> delta = new HashMap<>();
            for (UUID n : nodes) delta.put(n, 0.0);

            while (!stack.isEmpty()) {
                UUID w = stack.pop();
                for (UUID v : pred.get(w)) {
                    double c = (sigma.get(v) / sigma.get(w)) * (1.0 + delta.get(w));
                    delta.put(v, delta.get(v) + c);
                }
                if (!w.equals(source)) {
                    betweenness.put(w, betweenness.get(w) + delta.get(w));
                }
            }
        }

        // Normalise: divide by (n-1)(n-2) for undirected
        int n = nodes.size();
        if (n > 2) {
            double norm = (n - 1.0) * (n - 2.0);
            betweenness.replaceAll((k, v) -> v / norm);
        }

        return betweenness;
    }

    // =========================================================================
    // Private — influence centrality
    // =========================================================================

    private Map<UUID, Double> computeInfluenceCentrality(
            Map<String, PersonConnection> edgeIndex, Set<UUID> nodeIds) {

        Map<UUID, Double> influence = new HashMap<>();
        for (UUID id : nodeIds) influence.put(id, 0.0);

        // Sum weighted scores of incoming edges (edge points TO this node)
        for (Map.Entry<String, PersonConnection> entry : edgeIndex.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            UUID target = UUID.fromString(parts[1]);
            PersonConnection edge = entry.getValue();
            double score = edge.getInfluenceScore() != null
                    ? edge.getInfluenceScore() * edge.getConnectionType().getDefaultWeight()
                    : edge.getConnectionType().getDefaultWeight();
            influence.merge(target, score, Double::sum);
        }

        return influence;
    }

    // =========================================================================
    // Private — Union-Find cluster detection
    // =========================================================================

    private List<NetworkCluster> detectClusters(
            Map<UUID, Set<UUID>> adjacency,
            Map<String, PersonConnection> edgeIndex,
            Map<UUID, Person> personIndex) {

        // Union-Find
        Map<UUID, UUID> parent = new HashMap<>();
        for (UUID id : adjacency.keySet()) parent.put(id, id);

        for (Map.Entry<UUID, Set<UUID>> entry : adjacency.entrySet()) {
            UUID u = entry.getKey();
            for (UUID v : entry.getValue()) {
                union(parent, u, v);
            }
        }

        // Group by root
        Map<UUID, List<UUID>> groups = new LinkedHashMap<>();
        for (UUID id : adjacency.keySet()) {
            UUID root = find(parent, id);
            groups.computeIfAbsent(root, k -> new ArrayList<>()).add(id);
        }

        // Build cluster responses
        int clusterId = 1;
        List<NetworkCluster> clusters = new ArrayList<>();
        for (List<UUID> members : groups.values()) {
            if (members.size() < 2) continue; // skip isolates

            // Cohesion = average internal edge weight within cluster
            double cohesionSum = 0.0;
            int    internalEdges = 0;
            Set<UUID> memberSet = new HashSet<>(members);
            for (UUID u : members) {
                for (UUID v : adjacency.getOrDefault(u, Collections.emptySet())) {
                    if (memberSet.contains(v)) {
                        PersonConnection edge = edgeIndex.get(u + ":" + v);
                        if (edge != null) {
                            cohesionSum += edge.getInfluenceScore() != null
                                    ? edge.getInfluenceScore()
                                    : edge.getConnectionType().getDefaultWeight();
                            internalEdges++;
                        }
                    }
                }
            }
            double cohesion = internalEdges > 0 ? cohesionSum / internalEdges : 0.0;

            List<String> names = members.stream()
                    .map(id -> personIndex.containsKey(id)
                            ? personIndex.get(id).getFullName() : "UNKNOWN")
                    .toList();

            clusters.add(new NetworkCluster(clusterId++, members, names, cohesion));
        }

        return clusters;
    }

    // =========================================================================
    // Private — path reconstruction
    // =========================================================================

    private ConnectionPathResponse reconstructPath(
            UUID sourceId, UUID targetId,
            Map<UUID, UUID> parent,
            Map<UUID, PersonConnection> edgeUsed) {

        // Walk backwards from target to source
        List<UUID> pathIds = new ArrayList<>();
        UUID cur = targetId;
        while (cur != null) {
            pathIds.add(cur);
            cur = parent.get(cur);
        }
        Collections.reverse(pathIds);

        // Build nodes
        List<ConnectionPathResponse.PathNode> nodes = new ArrayList<>();
        for (int i = 0; i < pathIds.size(); i++) {
            UUID id = pathIds.get(i);
            Person p = personRepository.findByExternalId(id).orElse(null);
            nodes.add(new ConnectionPathResponse.PathNode(
                    id,
                    p != null ? p.getFullName() : "UNKNOWN",
                    extractCurrentPosition(p),
                    i
            ));
        }

        // Build edges
        List<ConnectionPathResponse.PathEdge> edges = new ArrayList<>();
        double scoreProduct = 1.0;
        for (int i = 1; i < pathIds.size(); i++) {
            UUID child = pathIds.get(i);
            PersonConnection edge = edgeUsed.get(child);
            if (edge != null) {
                double w = edge.getInfluenceScore() != null
                        ? edge.getInfluenceScore()
                        : edge.getConnectionType().getDefaultWeight();
                scoreProduct *= w;
                edges.add(new ConnectionPathResponse.PathEdge(
                        edge.getExternalId(),
                        edge.getSourcePerson().getExternalId(),
                        edge.getTargetPerson().getExternalId(),
                        edge.getConnectionType(),
                        w
                ));
            }
        }

        return new ConnectionPathResponse(
                sourceId, targetId,
                edges.size(),
                scoreProduct,
                nodes, edges,
                true
        );
    }

    // =========================================================================
    // Private — influence map helpers
    // =========================================================================

    private InfluenceNode buildInfluenceNode(
            UUID neighbourId,
            Map<UUID, List<PersonConnection>> outgoing,
            Map<UUID, List<PersonConnection>> incoming) {

        List<PersonConnection> out = outgoing.getOrDefault(neighbourId, Collections.emptyList());
        List<PersonConnection> in  = incoming.getOrDefault(neighbourId, Collections.emptyList());

        // Pick the "strongest" edge (highest influence score) for display
        PersonConnection bestEdge = Stream.concat(out.stream(), in.stream())
                .max(Comparator.comparingDouble(e ->
                        e.getInfluenceScore() != null
                                ? e.getInfluenceScore()
                                : e.getConnectionType().getDefaultWeight()))
                .orElseThrow();

        double rawScore = bestEdge.getInfluenceScore() != null
                ? bestEdge.getInfluenceScore()
                : bestEdge.getConnectionType().getDefaultWeight();

        double weighted = rawScore * bestEdge.getConnectionType().getDefaultWeight();

        Direction direction;
        if (!out.isEmpty() && !in.isEmpty()) direction = Direction.BIDIRECTIONAL;
        else if (!out.isEmpty())             direction = Direction.OUTGOING;
        else                                 direction = Direction.INCOMING;

        boolean active = bestEdge.getEndDate() == null
                || bestEdge.getEndDate().isAfter(LocalDate.now());

        Person neighbour = personRepository.findByExternalId(neighbourId).orElse(null);

        return new InfluenceNode(
                neighbourId,
                neighbour != null ? neighbour.getFullName() : "UNKNOWN",
                extractCurrentPosition(neighbour),
                bestEdge.getConnectionType(),
                rawScore,
                weighted,
                direction,
                active
        );
    }

    // =========================================================================
    // Private — utilities
    // =========================================================================

    /** Returns the "other end" of an edge relative to a known node. */
    private UUID getNeighbour(PersonConnection edge, UUID knownSide) {
        UUID src = edge.getSourcePerson().getExternalId();
        UUID tgt = edge.getTargetPerson().getExternalId();
        return src.equals(knownSide) ? tgt : src;
    }

    private String extractCurrentPosition(Person person) {
        if (person == null || person.getAppointments() == null) return "UNKNOWN";
        return person.getAppointments().stream()
                .filter(a -> a.getEndDate() == null)
                .map(a -> a.getOccupation().getTitle())
                .findFirst()
                .orElse("INACTIVE");
    }

    private void verifyPersonExists(UUID publicId) {
        if (!personRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Person not found: " + publicId);
    }

    // Union-Find helpers
    private UUID find(Map<UUID, UUID> parent, UUID x) {
        if (!parent.get(x).equals(x)) parent.put(x, find(parent, parent.get(x)));
        return parent.get(x);
    }

    private void union(Map<UUID, UUID> parent, UUID x, UUID y) {
        UUID rx = find(parent, x);
        UUID ry = find(parent, y);
        if (!rx.equals(ry)) parent.put(rx, ry);
    }

    private ConnectionPathResponse buildNoPathResponse(UUID sourceId, UUID targetId) {
        return new ConnectionPathResponse(sourceId, targetId, 0, 0.0,
                Collections.emptyList(), Collections.emptyList(), false);
    }
}
