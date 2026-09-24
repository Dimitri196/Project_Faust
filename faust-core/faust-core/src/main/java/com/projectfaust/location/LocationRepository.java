package com.projectfaust.location;

import com.projectfaust.shared.enums.LocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing spatial nodes within Project Faust.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long>,
        JpaSpecificationExecutor<Location> {

    // ── Basic lookups ─────────────────────────────────────────────────────────

    Optional<Location> findByExternalId(UUID externalId);

    boolean existsByExternalId(UUID externalId);

    List<Location> findAllByParentIsNullAndActiveTrue();

    // ── Sub-locations ─────────────────────────────────────────────────────────

    /**
     * Returns all active direct children by parent external UUID.
     * Used by LocationService.getSubLocations().
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.parent.externalId = :parentExternalId " +
            "AND l.active = true")
    List<Location> findAllActiveSubLocations(
            @Param("parentExternalId") UUID parentExternalId);

    /**
     * Returns all active direct children by parent internal Long id.
     * Used internally after Long parentId migration.
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.parent.id = :parentId " +
            "AND l.active = true")
    List<Location> findAllActiveSubLocationsByParentId(
            @Param("parentId") Long parentId);

    // ── Recursive CTEs ────────────────────────────────────────────────────────

    @Query(value = """
            WITH RECURSIVE path AS (
                SELECT * FROM locations
                WHERE external_id = :externalId
                UNION ALL
                SELECT l.* FROM locations l
                INNER JOIN path p ON l.id = p.parent_id
            )
            SELECT * FROM path
            """, nativeQuery = true)
    List<Location> findAncestorPath(@Param("externalId") UUID externalId);

    @Query(value = """
            WITH RECURSIVE descendants AS (
                SELECT id FROM locations WHERE external_id = :externalId
                UNION ALL
                SELECT l.id FROM locations l
                JOIN descendants d ON l.parent_id = d.id
            )
            SELECT id FROM descendants
            """, nativeQuery = true)
    List<Long> findDescendantIds(@Param("externalId") UUID externalId);

    @Modifying
    @Query(value = """
            WITH RECURSIVE subtree AS (
                SELECT id FROM locations
                WHERE external_id = :ancestorExternalId
                UNION ALL
                SELECT l.id FROM locations l
                INNER JOIN subtree s ON l.parent_id = s.id
            )
            UPDATE locations SET active = false
            WHERE id IN (SELECT id FROM subtree)
            AND external_id != :ancestorExternalId
            """, nativeQuery = true)
    int deactivateAllDescendants(@Param("ancestorExternalId") UUID ancestorExternalId);

    // ── ISO code lookups — used by OSM/GeoNames import ────────────────────────

    @Query("SELECT l FROM Location l WHERE l.isoCode = :isoCode ORDER BY l.id ASC")
    List<Location> findAllByIsoCode(@Param("isoCode") String isoCode);

    @Query("SELECT l FROM Location l WHERE l.isoCode = :isoCode AND l.type = :type ORDER BY l.id ASC")
    List<Location> findAllByIsoCodeAndType(
            @Param("isoCode") String isoCode,
            @Param("type")    LocationType type);

    // ── Name + type + parent lookups ──────────────────────────────────────────

    /**
     * Finds by name, type and parent external UUID.
     * Used by OSM import for deduplication.
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.name = :name " +
            "AND l.type = :type " +
            "AND (:parentExternalId IS NULL OR " +
            "    (l.parent IS NOT NULL AND l.parent.externalId = :parentExternalId))")
    Optional<Location> findByNameAndTypeAndParentExternalId(
            @Param("name")             String name,
            @Param("type")             LocationType type,
            @Param("parentExternalId") UUID parentExternalId);

    /**
     * Finds by name, type and parent internal Long id.
     * Used by OSM import after Long parentId migration.
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.name = :name " +
            "AND l.type = :type " +
            "AND (:parentId IS NULL OR l.parent.id = :parentId)")
    Optional<Location> findByNameTypeAndParentId(
            @Param("name")     String name,
            @Param("type")     LocationType type,
            @Param("parentId") Long parentId);

    /**
     * Finds by name and type only — no parent constraint.
     */
    @Query("SELECT l FROM Location l WHERE l.name = :name AND l.type = :type ORDER BY l.id ASC")
    Optional<Location> findByNameAndType(
            @Param("name") String name,
            @Param("type") LocationType type);

    // ── Type + parent lookups — used by OSM import ────────────────────────────

    @Query("SELECT l FROM Location l " +
            "WHERE l.type = :type " +
            "AND l.parent.externalId = :parentExternalId " +
            "AND l.active = true")
    List<Location> findAllByTypeAndParentExternalId(
            @Param("type")             LocationType type,
            @Param("parentExternalId") UUID parentExternalId);

    @Query("SELECT l FROM Location l " +
            "WHERE l.type = :type " +
            "AND l.parent.id = :parentId " +
            "AND l.active = true")
    List<Location> findAllByTypeAndParentId(
            @Param("type")     LocationType type,
            @Param("parentId") Long parentId);

    @Query("SELECT l FROM Location l " +
            "WHERE l.type = :type " +
            "AND l.parent.parent.id = :countryDbId " +
            "AND l.active = true")
    List<Location> findAllByTypeAndParentCountryDbId(
            @Param("type")        LocationType type,
            @Param("countryDbId") Long countryDbId);

    // ── Search — used by LocationSpecifications ───────────────────────────────

    /**
     * Main search query — filters by query, type, parentId, countryCode.
     * Called with Pageable.unpaged() for full result set.
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.active = true " +
            "AND (:query IS NULL OR LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "     OR LOWER(l.localName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "AND (:type IS NULL OR l.type = :type) " +
            "AND (:parentId IS NULL OR l.parent.id = :parentId) " +
            "AND (:countryCode IS NULL OR l.isoCode = :countryCode " +
            "     OR l.isoCode LIKE CONCAT(:countryCode, '-%') " +
            "     OR (l.parent IS NOT NULL AND (l.parent.isoCode = :countryCode " +
            "         OR l.parent.isoCode LIKE CONCAT(:countryCode, '-%'))))")
    Page<Location> searchWithCountry(
            @Param("query")       String query,
            @Param("type")        LocationType type,
            @Param("parentId")    Long parentId,
            @Param("countryCode") String countryCode,
            Pageable pageable);

    // ── Geocoding ─────────────────────────────────────────────────────────────

    /**
     * Returns all active locations under a given parent with no GPS coordinates.
     * Used by BatchGeocodingService.
     */
    @Query("SELECT l FROM Location l " +
            "WHERE l.parent.id = :parentId " +
            "AND l.latitude IS NULL " +
            "AND l.active = true")
    List<Location> findAllByParentIdAndLatitudeIsNull(@Param("parentId") Long parentId);
}