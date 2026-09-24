package com.projectfaust.network.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Full network-level analysis result for a subgraph rooted at a given subject.
 *
 * <p>Implements classical Social Network Analysis (SNA) metrics as used in
 * intelligence platforms — degree centrality, betweenness centrality, and
 * community/cluster detection. The analysis covers the subject's 2-hop
 * neighbourhood (configurable via {@code depth} parameter).</p>
 *
 * <p><b>Centrality interpretation:</b>
 * <ul>
 *   <li><b>Degree centrality</b> — raw edge count; high degree = well-connected broker.</li>
 *   <li><b>Betweenness centrality</b> — how often a node lies on the shortest path between
 *       other nodes; high betweenness = critical intermediary / information chokepoint.</li>
 *   <li><b>Influence centrality</b> — sum of weighted incoming influence scores;
 *       high influence = target of strong directed connections (e.g. INTELLIGENCE_SOURCE).</li>
 * </ul>
 * </p>
 *
 * @param subjectId         public UUID of the ego node.
 * @param analysisDepth     how many hops were traversed from the subject.
 * @param totalNodes        total persons in the analysed subgraph.
 * @param totalEdges        total connections in the subgraph.
 * @param graphDensity      ratio of actual to maximum possible edges — 1.0 = fully connected.
 * @param centralityScores  per-person centrality metrics, sorted by betweenness descending.
 * @param clusters          detected communities / clusters within the subgraph.
 * @param keyBrokers        persons with top-3 betweenness centrality — likely chokepoints.
 * @param isolatedNodes     persons with only one connection — potential peripheral assets.
 *
 * @author Dimitri / Project Faust
 */
public record NetworkAnalysisResponse(
        UUID                    subjectId,
        int                     analysisDepth,
        int                     totalNodes,
        int                     totalEdges,
        double                  graphDensity,
        List<CentralityScore>   centralityScores,
        List<NetworkCluster>    clusters,
        List<CentralityScore>   keyBrokers,
        List<UUID>              isolatedNodes
) {

    /**
     * Centrality metrics for a single person in the analysed subgraph.
     *
     * @param personId              public UUID.
     * @param fullName              display name.
     * @param degreeCentrality      normalised degree (0.0–1.0).
     * @param betweennessCentrality normalised betweenness (0.0–1.0).
     * @param influenceCentrality   sum of weighted incoming scores (not normalised).
     * @param riskScore             composite: {@code 0.4×degree + 0.4×betweenness + 0.2×influence}.
     *                              Values above 0.7 flag this node for analyst review.
     */
    public record CentralityScore(
            UUID   personId,
            String fullName,
            double degreeCentrality,
            double betweennessCentrality,
            double influenceCentrality,
            double riskScore
    ) {}

    /**
     * A detected community cluster within the subgraph.
     *
     * <p>Clusters are detected using greedy modularity maximisation (Union-Find
     * on strongly-connected components). Each cluster is assigned a sequential
     * integer ID.</p>
     *
     * @param clusterId     sequential identifier (1-based).
     * @param memberIds     public UUIDs of persons in this cluster.
     * @param memberNames   display names (parallel to {@code memberIds}).
     * @param cohesion      average internal edge weight — how tightly connected
     *                      the cluster members are to each other.
     */
    public record NetworkCluster(
            int          clusterId,
            List<UUID>   memberIds,
            List<String> memberNames,
            double       cohesion
    ) {}
}
