package com.projectfaust.network.dto.response;

import com.projectfaust.shared.enums.ConnectionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Influence map for a given subject person — the full egocentric subgraph
 * with scored, ranked, and typed connections.
 *
 * <p>Used in the intelligence dossier view to render the person's network.
 * Modelled after egocentric network analysis as used in OSINT platforms
 * such as Palantir and i2 Analyst's Notebook.</p>
 *
 * <p>The {@link InfluenceNode} list is pre-sorted by {@code weightedScore}
 * descending — the highest-influence contacts appear first.</p>
 *
 * @param subjectId             public UUID of the subject (ego node).
 * @param subjectFullName       display name of the subject.
 * @param totalConnections      total edge count in the map.
 * @param activeConnections     edges where endDate is null or in the future.
 * @param nodes                 all persons directly connected to the subject.
 * @param connectionTypeSummary count of connections grouped by {@link ConnectionType}.
 * @param topInfluencers        top-5 nodes by weighted influence score.
 *
 * @author Dimitri / Project Faust
 */
public record InfluenceMapResponse(
        UUID                      subjectId,
        String                    subjectFullName,
        int                       totalConnections,
        int                       activeConnections,
        List<InfluenceNode>       nodes,
        Map<ConnectionType, Long> connectionTypeSummary,
        List<InfluenceNode>       topInfluencers
) {

    /**
     * A single node in the influence map — one person directly connected to the subject.
     *
     * @param personId       public UUID of the connected person.
     * @param fullName       display name.
     * @param currentRole    current occupation title.
     * @param connectionType the type of relationship to the subject.
     * @param influenceScore raw edge weight (0.0–1.0) from the connection record.
     * @param weightedScore  {@code influenceScore × connectionType.defaultWeight} —
     *                       normalised score accounting for both recorded weight and
     *                       category risk.
     * @param direction      {@code OUTGOING} (subject is source), {@code INCOMING}
     *                       (subject is target), or {@code BIDIRECTIONAL} (both exist).
     * @param active         whether the connection is currently active.
     */
    public record InfluenceNode(
            UUID           personId,
            String         fullName,
            String         currentRole,
            ConnectionType connectionType,
            double         influenceScore,
            double         weightedScore,
            Direction      direction,
            boolean        active
    ) {}

    public enum Direction {
        OUTGOING, INCOMING, BIDIRECTIONAL
    }
}
