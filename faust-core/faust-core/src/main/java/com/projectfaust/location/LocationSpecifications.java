package com.projectfaust.location;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationSourceType;
import com.projectfaust.shared.enums.LocationType;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Static factory for JPA {@link Specification} predicates used to filter
 * {@link Location} entities in Project Faust.
 *
 * <p>Two usage patterns are supported:</p>
 * <ul>
 *   <li><b>Composed predicates</b> — individual static methods (e.g.,
 *       {@link #nameContains(String)}, {@link #insideBounds}) can be combined
 *       via {@link Specification#and} / {@link Specification#or} for custom queries.</li>
 *   <li><b>Filter-based assembly</b> — {@link #build(LocationFilter)} is the
 *       primary entry point; it reads a {@link LocationFilter} and composes only
 *       the predicates for non-null fields into a single {@code AND} query.</li>
 * </ul>
 *
 * <p><b>Security note on clearance filtering:</b> the {@link #securityScope(ClearanceLevel)}
 * predicate resolves allowed clearance levels into an {@code IN} list of enum values
 * whose weight is less than or equal to the operator's maximum clearance. This avoids
 * attempting a {@code <=} comparison on a string-mapped enum column, which JPA does
 * not support.</p>
 *
 * <p>This is a static utility class and cannot be instantiated.</p>
 *
 * @author Dimitri / Project Faust
 */
public class LocationSpecifications {

    /** Utility class — no instances allowed. */
    private LocationSpecifications() {}

    // -------------------------------------------------------------------------
    // Individual predicate factories
    // -------------------------------------------------------------------------

    /**
     * Restricts results to active nodes only.
     *
     * <p>Applied as the first predicate in {@link #build(LocationFilter)} to ensure
     * inactive nodes are never accidentally surfaced to the presentation layer.</p>
     *
     * @return a specification that filters out inactive locations.
     */
    public static Specification<Location> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Case-insensitive partial match on the location name.
     *
     * @param name the search term; returns an always-true predicate if null or blank.
     * @return a {@code LIKE} specification on the name field.
     */
    public static Specification<Location> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank())
                ? null
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    /**
     * Restricts results to nodes whose type is contained in the given set.
     *
     * @param types the allowed location types; returns null predicate if null or empty.
     * @return an {@code IN} specification on the type field.
     */
    public static Specification<Location> hasTypes(Set<LocationType> types) {
        return (root, query, cb) ->
                (types == null || types.isEmpty()) ? null : root.get("type").in(types);
    }

    /**
     * Restricts results to direct children of the specified parent node.
     *
     * @param parentExternalId the public UUID of the parent; returns null predicate if null.
     * @return an equality specification on the parent's externalId.
     */
    public static Specification<Location> hasParent(UUID parentExternalId) {
        return (root, query, cb) -> parentExternalId == null
                ? null
                : cb.equal(root.get("parent").get("externalId"), parentExternalId);
    }

    /**
     * Restricts results to root-level nodes (nodes with no parent).
     *
     * @return a specification that filters for null parent.
     */
    public static Specification<Location> isRoot() {
        return (root, query, cb) -> cb.isNull(root.get("parent"));
    }

    /**
     * Restricts results to nodes whose ISO code matches exactly.
     *
     * @param isoCode the ISO 3166-1 alpha-2 code or internal classified area code.
     * @return an equality specification on the isoCode field.
     */
    public static Specification<Location> hasIsoCode(String isoCode) {
        return (root, query, cb) -> (isoCode == null || isoCode.isBlank())
                ? null
                : cb.equal(cb.lower(root.get("isoCode")), isoCode.toLowerCase());
    }

    /**
     * Restricts results to nodes whose {@link VerificationStatus} is in the given set.
     *
     * <p>Used by analysts to isolate records by evidentiary quality —
     * e.g. show only {@code PENDING_REVIEW} nodes awaiting sign-off,
     * or flag all {@code DECEPTION_MARKER} honeypots.</p>
     *
     * @param statuses the allowed verification statuses; returns null predicate if null or empty.
     * @return an {@code IN} specification on the verificationStatus field.
     */
    public static Specification<Location> hasVerificationStatus(Set<VerificationStatus> statuses) {
        return (root, query, cb) ->
                (statuses == null || statuses.isEmpty()) ? null
                        : root.get("verificationStatus").in(statuses);
    }

    /**
     * Restricts results to nodes whose {@link LocationSourceType} is in the given set.
     *
     * <p>Allows data quality audits such as "show all HUMINT-sourced locations"
     * or "show only OVERPASS-imported nodes".</p>
     *
     * @param sourceTypes the allowed source types; returns null predicate if null or empty.
     * @return an {@code IN} specification on the sourceType field.
     */
    public static Specification<Location> hasSourceTypes(Set<LocationSourceType> sourceTypes) {
        return (root, query, cb) ->
                (sourceTypes == null || sourceTypes.isEmpty()) ? null
                        : root.get("sourceType").in(sourceTypes);
    }

    /**
     * Security clearance scope filter — restricts results to nodes the operator
     * is cleared to access.
     *
     * <p><b>Implementation note:</b> JPA cannot apply {@code <=} to a string-mapped enum.
     * This predicate resolves the operator's maximum clearance into an explicit
     * {@code IN} list of all {@link ClearanceLevel} values whose weight is less than
     * or equal to the operator's level, then uses an {@code IN} predicate on the
     * string-mapped column. This is both type-safe and index-friendly.</p>
     *
     * @param maxLevel the highest clearance the operator holds; returns null predicate if null.
     * @return an {@code IN} specification containing all clearance levels the operator may see.
     */
    public static Specification<Location> securityScope(ClearanceLevel maxLevel) {
        return (root, query, cb) -> {
            if (maxLevel == null) return null;

            // Resolve all clearance levels the operator is permitted to see
            List<ClearanceLevel> allowedLevels = Arrays.stream(ClearanceLevel.values())
                    .filter(level -> level.getWeight() <= maxLevel.getWeight())
                    .collect(Collectors.toList());

            return root.get("clearanceLevel").in(allowedLevels);
        };
    }

    /**
     * Geofencing predicate — restricts results to nodes whose coordinates
     * fall within the specified map viewport bounding box.
     *
     * <p>All four bounds must be non-null for the predicate to be applied;
     * if any bound is missing, the predicate is skipped entirely.</p>
     *
     * @param north northern latitude boundary (decimal degrees).
     * @param south southern latitude boundary (decimal degrees).
     * @param east  eastern longitude boundary (decimal degrees).
     * @param west  western longitude boundary (decimal degrees).
     * @return a compound {@code BETWEEN} specification on latitude and longitude.
     */
    public static Specification<Location> insideBounds(
            Double north, Double south, Double east, Double west) {
        return (root, query, cb) -> {
            if (north == null || south == null || east == null || west == null) return null;
            return cb.and(
                    cb.between(root.get("latitude"),  south, north),
                    cb.between(root.get("longitude"), west,  east)
            );
        };
    }

    // -------------------------------------------------------------------------
    // Primary filter assembly
    // -------------------------------------------------------------------------

    /**
     * Assembles a composite {@link Specification} from a {@link LocationFilter}.
     *
     * <p>Only predicates for non-null filter fields are applied. Fields left null
     * impose no restriction on that dimension. All predicates are combined with
     * {@code AND}. Active-only enforcement is always applied regardless of the filter.</p>
     *
     * <p><b>Hierarchy drill-down logic:</b></p>
     * <ul>
     *   <li>If {@code parentId} is set → restrict to direct children of that parent.</li>
     *   <li>Else if {@code rootOnly} is true → restrict to root nodes only.</li>
     *   <li>Otherwise → no hierarchy restriction (global search).</li>
     * </ul>
     *
     * @param filter the search criteria; may be null (returns active-only specification).
     * @return a composed {@link Specification} ready for use with
     *         {@link LocationRepository} and its {@code findAll} method.
     */
    public static Specification<Location> build(LocationFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Always enforce active records — inactive nodes are never surfaced
            predicates.add(cb.isTrue(root.get("active")));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            // 2. Free-text name search (case-insensitive partial match)
            if (filter.getQuery() != null && !filter.getQuery().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("name")),
                        "%" + filter.getQuery().toLowerCase() + "%"));
            }

            // 3. Type filter — IN list replaces the former single-type field
            if (filter.getTypes() != null && !filter.getTypes().isEmpty()) {
                predicates.add(root.get("type").in(filter.getTypes()));
            }

            // 4. Hierarchy drill-down logic
            if (filter.getParentId() != null) {
                // Drill into a specific parent node
                predicates.add(cb.equal(
                        root.get("parent").get("externalId"), filter.getParentId()));
            } else if (Boolean.TRUE.equals(filter.getRootOnly())) {
                // Return only root-level nodes
                predicates.add(cb.isNull(root.get("parent")));
            }

            // 5. ISO code (exact match, case-insensitive)
            if (filter.getIsoCode() != null && !filter.getIsoCode().isBlank()) {
                predicates.add(cb.equal(
                        cb.lower(root.get("isoCode")),
                        filter.getIsoCode().toLowerCase()));
            }

            // 6. Geographic bounding box — all four bounds required
            if (filter.getNorth() != null && filter.getSouth() != null
                    && filter.getEast() != null && filter.getWest() != null) {
                predicates.add(cb.between(
                        root.get("latitude"),  filter.getSouth(), filter.getNorth()));
                predicates.add(cb.between(
                        root.get("longitude"), filter.getWest(),  filter.getEast()));
            }

            // 7. Security clearance scope — resolves allowed levels into IN list
            if (filter.getMaxClearance() != null) {
                List<ClearanceLevel> allowedLevels = Arrays.stream(ClearanceLevel.values())
                        .filter(l -> l.getWeight() <= filter.getMaxClearance().getWeight())
                        .collect(Collectors.toList());
                predicates.add(root.get("clearanceLevel").in(allowedLevels));
            }

            // 8. Verification status filter (intelligence provenance)
            if (filter.getVerificationStatuses() != null
                    && !filter.getVerificationStatuses().isEmpty()) {
                predicates.add(root.get("verificationStatus")
                        .in(filter.getVerificationStatuses()));
            }

            // 9. Source type filter (ingestion channel audit)
            if (filter.getSourceTypes() != null && !filter.getSourceTypes().isEmpty()) {
                predicates.add(root.get("sourceType").in(filter.getSourceTypes()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}