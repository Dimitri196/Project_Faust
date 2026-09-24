package com.projectfaust.location;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationSourceType;
import com.projectfaust.shared.enums.LocationType;
import com.projectfaust.shared.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Query filter object for the {@link LocationService#search(LocationFilter, Pageable)} operation.
 *
 * <p>Every field is optional — a {@code null} value means "no restriction on this dimension".
 * The filter is translated into a JPA {@link org.springframework.data.jpa.domain.Specification}
 * by {@link LocationSpecifications#build(LocationFilter)}, which composes only the predicates
 * for non-null fields.</p>
 *
 * <p><b>Design notes:</b></p>
 * <ul>
 *   <li>A single {@code types} set replaces the former dual {@code type}/{@code types} fields
 *       to eliminate ambiguity when both were populated. To filter by a single type,
 *       pass a one-element set.</li>
 *   <li>The bounding box fields ({@code north}/{@code south}/{@code east}/{@code west})
 *       support map viewport queries — only nodes whose coordinates fall within the
 *       box are returned.</li>
 *   <li>Intelligence-specific fields ({@code verificationStatuses}, {@code sourceTypes})
 *       allow analysts to isolate data by provenance and evidentiary quality.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationFilter {

    /**
     * Free-text search against the location name (case-insensitive, partial match).
     */
    private String query;

    /**
     * Restricts results to nodes whose {@link LocationType} is contained in this set.
     * Pass a single-element set to filter by one type. {@code null} = all types.
     */
    private Set<LocationType> types;

    /**
     * Restricts results to direct children of the specified parent UUID.
     */
    private UUID parentId;

    /**
     * When {@code true}, restricts results to root-level nodes only (no parent).
     */
    private Boolean rootOnly;

    /**
     * Filters by ISO 3166-1 alpha-2 country code, or internal classified area code.
     */
    private String isoCode;

    /**
     * Maximum clearance level visible to the requesting operator.
     * Nodes with a higher clearance weight than this value are excluded.
     */
    private ClearanceLevel maxClearance;

    /**
     * Filters by active status. {@code null} = return both active and inactive.
     */
    private Boolean active;

    // --- Bounding box (GEOINT map viewport) ---

    /** Northern boundary latitude in decimal degrees. */
    private Double north;

    /** Southern boundary latitude in decimal degrees. */
    private Double south;

    /** Eastern boundary longitude in decimal degrees. */
    private Double east;

    /** Western boundary longitude in decimal degrees. */
    private Double west;

    /**
     * When {@code true}, includes all descendants of matched nodes in the results,
     * not just the direct matches.
     */
    @Builder.Default
    private boolean includeSubtree = false;

    // --- Hierarchical scope ---

    /**
     * Scopes the search to nodes within the specified country UUID.
     * Combines with {@code provinceId} for finer scoping.
     */
    private UUID countryId;

    /**
     * Scopes the search to nodes within the specified province UUID.
     */
    private UUID provinceId;

    // --- Intelligence metadata filters ---

    /**
     * Filters by evidentiary provenance status.
     * Allows analysts to isolate e.g. only {@code PENDING_REVIEW} or {@code DECEPTION_MARKER} nodes.
     * {@code null} = all statuses.
     */
    private Set<VerificationStatus> verificationStatuses;

    /**
     * Filters by the ingestion channel that produced the record.
     * Allows data quality audits such as "show all HUMINT-sourced locations".
     * {@code null} = all source types.
     */
    private Set<LocationSourceType> sourceTypes;
}
