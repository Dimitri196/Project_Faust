package com.projectfaust.intelligence.dto;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing the shortest connection path between two persons
 * in the Project Faust influence graph.
 *
 * <p>Produced by the BFS algorithm in
 * {@link com.projectfaust.intelligence.IntelligenceService#findPath}.</p>
 *
 * @param degrees    number of hops between source and target (0 = same person, -1 = no path).
 * @param names      ordered list of display names along the path.
 * @param pathIds    ordered list of public UUIDs along the path.
 * @param found      whether a path was discovered.
 * @author Dimitri / Project Faust
 */
public record ConnectionPathResponse(
        int degrees,
        List<String> names,
        List<UUID> pathIds,
        boolean found
) {}