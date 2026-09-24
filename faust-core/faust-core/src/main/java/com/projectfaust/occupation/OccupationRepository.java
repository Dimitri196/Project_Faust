package com.projectfaust.occupation;

import com.projectfaust.shared.enums.OccupationCategory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing occupational position nodes within Project Faust.
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long} primary key is never exposed outside the persistence layer.</p>
 *
 * <p><b>Fetch strategy:</b> methods suffixed {@code Fetched} or using
 * {@code @EntityGraph} eagerly load associations to prevent N+1 queries.
 * Use the plain derived methods only when the associated data is not needed.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface OccupationRepository extends JpaRepository<Occupation, Long> {

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Checks whether a position with the given public UUID exists.
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching position exists.
     */
    boolean existsByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Single entity lookups
    // -------------------------------------------------------------------------

    /**
     * Retrieves a position by its public UUID.
     *
     * @param externalId the public UUID of the position.
     * @return an {@link Optional} containing the position, or empty if not found.
     */
    Optional<Occupation> findByExternalId(UUID externalId);

    /**
     * Retrieves a position with its direct subordinates eagerly loaded,
     * for use with recursive tree mapping in {@link OccupationMapper#toTreeResponse}.
     *
     * <p>Prevents lazy-load exceptions when the mapper recursively builds
     * the subordinate hierarchy. Use this method instead of
     * {@link #findByExternalId} when calling {@link OccupationMapper#toTreeResponse}.</p>
     *
     * @param externalId the public UUID of the root position.
     * @return an {@link Optional} containing the position with subordinates pre-loaded.
     */
    @Query("SELECT DISTINCT o FROM Occupation o WHERE o.externalId = :externalId")
    @EntityGraph(attributePaths = {"subordinates", "subordinates.subordinates"})
    Optional<Occupation> findWithSubordinatesByExternalId(@Param("externalId") UUID externalId);

    // -------------------------------------------------------------------------
    // Collection queries
    // -------------------------------------------------------------------------

    /**
     * Returns all positions that directly report to the specified superior.
     *
     * @param reportsToExternalId the public UUID of the superior position.
     * @return list of direct subordinate positions.
     */
    List<Occupation> findByReportsToExternalId(UUID reportsToExternalId);

    /**
     * Returns all positions associated with a specific institution.
     *
     * <p>Use {@link #findByInstitutionExternalIdFetched} instead when rendering
     * full position lists to avoid N+1 queries.</p>
     *
     * @param institutionId the public UUID of the institution.
     * @return list of positions within the institution.
     */
    List<Occupation> findByInstitutionExternalId(UUID institutionId);

    /**
     * Returns all positions for an institution with institution, location,
     * and reporting-line associations eagerly loaded.
     *
     * <p>Prevents N+1 overhead when rendering the full position list
     * for an institution dossier.</p>
     *
     * @param instId the public UUID of the institution.
     * @return fully-loaded list of positions for the institution.
     */
    @Query("SELECT o FROM Occupation o " +
            "LEFT JOIN FETCH o.institution i " +
            "LEFT JOIN FETCH i.location " +
            "LEFT JOIN FETCH o.reportsTo " +
            "WHERE i.externalId = :instId")
    List<Occupation> findByInstitutionExternalIdFetched(@Param("instId") UUID instId);

    /**
     * Returns all top-level positions (those with no superior) with their
     * direct subordinates eagerly loaded.
     *
     * <p>Used by {@link OccupationService#getCommandChain()} to build
     * the full command chain tree from the top down.</p>
     *
     * @return list of root positions with direct subordinates pre-loaded.
     */
    @Query("SELECT o FROM Occupation o " +
            "LEFT JOIN FETCH o.subordinates " +
            "WHERE o.reportsTo IS NULL")
    List<Occupation> findTopLevelRoles();

    /**
     * Returns all direct subordinates of the specified superior.
     *
     * @param parentId the public UUID of the superior position.
     * @return list of direct subordinate positions.
     */
    @Query("SELECT o FROM Occupation o WHERE o.reportsTo.externalId = :parentId")
    List<Occupation> findAllSubordinates(@Param("parentId") UUID parentId);

    /**
     * Cross-entity OSINT query — finds positions by functional category
     * within a specific country.
     *
     * <p>Traverses: {@code Occupation → Institution → Location → isoCode}.
     * Useful for identifying all EXECUTIVE roles in a given country.</p>
     *
     * @param category    the functional category (e.g. EXECUTIVE, MILITARY).
     * @param countryCode the ISO 3166-1 alpha-2 country code.
     * @return list of positions matching both category and country.
     */
    @Query("SELECT o FROM Occupation o " +
            "JOIN o.institution i " +
            "JOIN i.location l " +
            "WHERE o.category = :category AND l.isoCode = :countryCode")
    List<Occupation> findByCategoryAndCountryCode(
            @Param("category") OccupationCategory category,
            @Param("countryCode") String countryCode);

    /**
     * Bulk fetch by a collection of public UUIDs.
     *
     * @param externalIds collection of public UUIDs to fetch.
     * @return list of found positions.
     */
    List<Occupation> findAllByExternalIdIn(Collection<UUID> externalIds);
}