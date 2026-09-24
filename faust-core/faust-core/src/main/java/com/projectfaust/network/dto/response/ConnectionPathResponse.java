package com.projectfaust.network.dto.response;

import com.projectfaust.shared.enums.ConnectionType;

import java.util.List;
import java.util.UUID;

/**
 * Represents the shortest path between two persons in the HUMINT graph.
 *
 * <p>Each {@link PathNode} is a person in the chain; each {@link PathEdge}
 * is the direct connection between two adjacent nodes. The path is ordered
 * from source to target.</p>
 *
 * <p>Intelligence use-case: "Find the shortest chain linking Subject A
 * to Subject B through their known network." Distance &lt; 3 is considered
 * high-risk proximity in classical six-degrees analysis.</p>
 *
 * @param sourceId          public UUID of the starting person.
 * @param targetId          public UUID of the destination person.
 * @param hops              number of edges in the path (path length).
 * @param totalInfluenceScore product of edge weights along the path — a proxy
 *                          for how "direct" and "strong" the chain of influence is.
 * @param nodes             ordered list of persons in the path (source first).
 * @param edges             ordered list of edges connecting adjacent nodes.
 * @param pathFound         false when no path exists between source and target.
 *
 * @author Dimitri / Project Faust
 */
public record ConnectionPathResponse(
        UUID          sourceId,
        UUID          targetId,
        int           hops,
        double        totalInfluenceScore,
        List<PathNode> nodes,
        List<PathEdge> edges,
        boolean       pathFound
) {

    /**
     * A person node in the path chain.
     *
     * @param personId      public UUID.
     * @param fullName      display name (redacted if classified).
     * @param currentRole   active occupation title.
     * @param depth         distance from the source node (0 = source).
     */
    public record PathNode(
            UUID   personId,
            String fullName,
            String currentRole,
            int    depth
    ) {}

    /**
     * A directed edge between two adjacent path nodes.
     *
     * @param connectionId   public UUID of the PersonConnection record.
     * @param sourcePersonId source node UUID.
     * @param targetPersonId target node UUID.
     * @param connectionType the nature of the relationship.
     * @param influenceScore edge weight (0.0–1.0).
     */
    public record PathEdge(
            UUID           connectionId,
            UUID           sourcePersonId,
            UUID           targetPersonId,
            ConnectionType connectionType,
            double         influenceScore
    ) {}
}
