package com.projectfaust.institution;

import com.projectfaust.location.Location;
import com.projectfaust.location.LocationRepository;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.HierarchicalLevel;
import com.projectfaust.shared.enums.InstitutionType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

/**
 * Dynamic JPA Specifications for filtering {@link Institution} search queries.
 *
 * @author Dimitri / Project Faust
 */
public class InstitutionSpecifications {

    public static Specification<Institution> activeOnly() {
        return (root, query, cb) -> cb.isTrue(root.get("active"));
    }

    /**
     * Filters by minimum clearance level.
     *
     * <p><b>FIXED:</b> previously referenced {@code root.get("securityLevel")},
     * a field that does not exist on {@link Institution} — the actual entity
     * field is {@code clearanceLevel}. The old version threw
     * {@code IllegalArgumentException} the moment this predicate was invoked
     * with a non-null value (it just happened to never be wired into
     * {@code InstitutionService.search()}, so the bug was latent).</p>
     */
    public static Specification<Institution> hasSecurityLevel(ClearanceLevel clearanceLevel) {
        return (root, query, cb) -> clearanceLevel == null ? null :
                cb.equal(root.get("clearanceLevel"), clearanceLevel);
    }

    public static Specification<Institution> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Institution> hasCountry(String countryCode) {
        return (root, query, cb) -> {
            if (countryCode == null || countryCode.isBlank()) return null;
            return cb.equal(root.get("location").get("isoCode"), countryCode.toUpperCase());
        };
    }

    /**
     * Filters by state ownership flag.
     *
     * <p><b>FIXED:</b> previously referenced {@code root.get("isStateOwned")} —
     * the actual entity field is {@code stateOwned} (Lombok strips the "is"
     * prefix from boolean getters/setters, and JPA's {@code root.get()} uses
     * the raw field name). This threw {@code IllegalArgumentException} on
     * every search request where {@code stateOwned} was non-null — this was
     * an active bug, wired directly into {@code InstitutionService.search()}.</p>
     */
    public static Specification<Institution> isStateOwned(Boolean isStateOwned) {
        return (root, query, cb) -> isStateOwned == null ? null :
                cb.equal(root.get("stateOwned"), isStateOwned);
    }

    public static Specification<Institution> hasType(InstitutionType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Institution> hasLevel(HierarchicalLevel level) {
        return (root, query, cb) -> level == null ? null : cb.equal(root.get("level"), level);
    }

    /**
     * Filters by location, matching the entire descendant subtree of the
     * given location — not just institutions whose {@code location_id}
     * points at the exact node.
     *
     * <p><b>FIXED:</b> the previous version did
     * {@code cb.equal(root.get("location").get("externalId"), locationId)},
     * which only matched institutions attached directly to that exact
     * location row. In practice, institutions are linked to specific
     * buildings/facilities, while location pages are frequently viewed at
     * a broader level (city, region, country) — so a search for "Prague"
     * found zero institutions even when dozens exist at buildings located
     * underneath Prague in the location hierarchy. This caused the
     * "Bound_Institutional_Nodes" panel on {@code LocationDetailPage} to
     * always render empty for any non-leaf location.</p>
     *
     * <p>This method first resolves every descendant location ID under the
     * given root (including the root itself) via a recursive native query,
     * then matches institutions whose location is any one of them.</p>
     *
     * @param locationId      the public UUID of the location being viewed.
     * @param locationRepository injected to resolve the descendant ID set;
     *                        callers must supply this since static
     *                        Specification factory methods can't use
     *                        Spring-managed beans directly.
     */
    public static Specification<Institution> hasLocationOrDescendant(
            UUID locationId, LocationRepository locationRepository) {
        return (root, query, cb) -> {
            if (locationId == null) return null;

            List<Long> descendantIds = locationRepository.findDescendantIds(locationId);
            if (descendantIds.isEmpty()) {
                // No matching location found at all (invalid UUID) —
                // return a predicate that matches nothing, rather than null
                // (which would mean "no filter applied").
                return cb.disjunction();
            }

            return root.get("location").get("id").in(descendantIds);
        };
    }

    /**
     * @deprecated use {@link #hasLocationOrDescendant(UUID, LocationRepository)}
     * instead — this only matches the exact location node, not its subtree,
     * which does not match how institutions are actually distributed across
     * the location hierarchy in practice. Retained temporarily in case any
     * caller relies on exact-match semantics intentionally.
     */
    @Deprecated
    public static Specification<Institution> hasLocation(UUID locationId) {
        return (root, query, cb) -> {
            if (locationId == null) return null;
            return cb.equal(root.get("location").get("externalId"), locationId);
        };
    }

    public static Specification<Institution> hasParent(UUID parentId) {
        return (root, query, cb) -> {
            if (parentId == null) return null;
            return cb.equal(root.get("parent").get("externalId"), parentId);
        };
    }
}