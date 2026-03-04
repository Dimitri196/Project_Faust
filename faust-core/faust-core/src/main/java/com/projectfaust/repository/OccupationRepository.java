package com.projectfaust.repository;

import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.enums.OccupationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing Occupation nodes (functional roles) within Project Faust.
 * Provides advanced querying for organizational structures, reporting lines, and cross-border role analysis.
 */
@Repository
public interface OccupationRepository extends JpaRepository<Occupation, Long> {

    /**
     * Retrieves an occupation based on its global unique identifier.
     *
     * @param externalId The UUID assigned to the occupation node.
     * @return An Optional containing the found Occupation, or empty if not found.
     */
    Optional<Occupation> findByExternalId(UUID externalId);

    /**
     * Finds all occupations associated with a specific institution.
     *
     * @param institutionId The UUID of the parent institution.
     * @return A list of occupations within the specified institution.
     */
    List<Occupation> findByInstitutionExternalId(UUID institutionId);

    /**
     * Performs a cross-entity search to find roles by category and geographical location.
     * Traverses the path: Occupation -> Institution -> Location -> ISO Code.
     *
     * @param category The functional category (e.g., EXECUTIVE, GOVERNANCE).
     * @param countryCode The ISO 3166-1 alpha-2 country code.
     * @return A list of occupations matching both the category and country.
     */
    @Query("SELECT o FROM Occupation o " +
            "JOIN o.institution i " +
            "JOIN i.location l " +
            "WHERE o.category = :category AND l.isoCode = :countryCode")
    List<Occupation> findByCategoryAndCountryCode(
            @Param("category") OccupationCategory category,
            @Param("countryCode") String countryCode);

    /**
     * Retrieves all occupations that report to a specific supervisor role by its UUID.
     *
     * @param reportsToExternalId The UUID of the superior occupation.
     * @return A list of direct subordinates.
     */
    List<Occupation> findByReportsToExternalId(UUID reportsToExternalId);

    /**
     * Retrieves all occupations that report to a specific supervisor role by its internal ID.
     *
     * @param reportsToId The primary key of the superior occupation.
     * @return A list of direct subordinates.
     */
    List<Occupation> findByReportsToId(Long reportsToId);

    /**
     * Optimized fetch for all occupations in an institution, including reporting lines and geography.
     * Eliminates N+1 query overhead by eagerly loading associated metadata.
     *
     * @param instId The UUID of the institution.
     * @return A fully initialized list of occupations for the institution.
     */
    @Query("SELECT o FROM Occupation o " +
            "LEFT JOIN FETCH o.institution i " +
            "LEFT JOIN FETCH i.location " +
            "LEFT JOIN FETCH o.reportsTo " +
            "WHERE i.externalId = :instId")
    List<Occupation> findByInstitutionExternalIdFetched(@Param("instId") UUID instId);

    /**
     * Retrieves a collection of occupations for bulk processing.
     *
     * @param externalIds A collection of UUIDs to fetch.
     * @return A list of found occupations.
     */
    List<Occupation> findAllByExternalIdIn(Collection<UUID> externalIds);

    /**
     * Identifies top-level roles within the organizational hierarchy (those without a supervisor).
     * Eagerly fetches subordinates to allow for efficient tree traversal.
     *
     * @return A list of root-level roles with initialized subordinate nodes.
     */
    @Query("SELECT o FROM Occupation o " +
            "LEFT JOIN FETCH o.subordinates " +
            "WHERE o.reportsTo IS NULL")
    List<Occupation> findTopLevelRoles();

    /**
     * Finds all subordinate positions for a given superior's external identifier.
     *
     * @param parentId The UUID of the superior role.
     * @return A list of subordinate occupations.
     */
    @Query("SELECT o FROM Occupation o WHERE o.reportsTo.externalId = :parentId")
    List<Occupation> findAllSubordinates(@Param("parentId") UUID parentId);

}
