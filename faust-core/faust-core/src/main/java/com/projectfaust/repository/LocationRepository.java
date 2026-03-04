package com.projectfaust.repository;

import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing spatial nodes within Project Faust.
 * Provides methods for hierarchical navigation and filtering of active geographic entities.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long>, JpaSpecificationExecutor<Location> {

    /**
     * Retrieves a location based on its global unique identifier.
     *
     * @param externalId The UUID assigned to the location.
     * @return An Optional containing the found Location, or empty if not found.
     */
    Optional<Location> findByExternalId(UUID externalId);

    /**
     * Retrieves root-level elements (e.g., Countries) that are currently marked as active.
     *
     * @return A list of active top-level locations.
     */
    List<Location> findAllByParentIsNullAndActiveTrue();

    /**
     * Finds all active sub-locations belonging to a specific parent node.
     *
     * @param parentExternalId The UUID of the parent location.
     * @return A list of active child locations.
     */
    @Query("SELECT l FROM Location l WHERE l.parent.externalId = :parentExternalId AND l.active = true")
    List<Location> findAllActiveSubLocations(@Param("parentExternalId") UUID parentExternalId);

    /**
     * Searches for a specific node based on a unique combination of name and type within a parent scope.
     * Primarily used for data seeding and validation to prevent duplicates.
     *
     * @param name The name of the location.
     * @param type The categorization of the location (e.g., CITY, FACILITY).
     * @param parentExternalId The UUID of the parent, or null for root-level nodes.
     * @return An Optional containing the specific location if found.
     */
    @Query("SELECT l FROM Location l WHERE l.name = :name AND l.type = :type " +
            "AND (:parentExternalId IS NULL OR (l.parent IS NOT NULL AND l.parent.externalId = :parentExternalId))")
    Optional<Location> findByNameAndTypeAndParentExternalId(
            @Param("name") String name,
            @Param("type") LocationType type,
            @Param("parentExternalId") UUID parentExternalId);
}
