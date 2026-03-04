package com.projectfaust.repository;

import com.projectfaust.entity.Institution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing Institutional nodes within Project Faust.
 * Supports hierarchical tree traversal and advanced filtering via JPA Specifications.
 */
@Repository
public interface InstitutionRepository extends
        JpaRepository<Institution, Long>,
        JpaSpecificationExecutor<Institution> {

    /**
     * Retrieves all root-level institutions (those without a parent)
     * while eagerly fetching their immediate child nodes to prevent N+1 issues.
     * * @return A list of top-level institutions with initialized children.
     */
    @Query("SELECT i FROM Institution i LEFT JOIN FETCH i.children WHERE i.parent IS NULL")
    List<Institution> findAllRoots();

    /**
     * Finds a specific institution by its globally unique identifier.
     * * @param externalId The UUID assigned to the institution.
     * @return An Optional containing the found Institution, or empty if not found.
     */
    Optional<Institution> findByExternalId(UUID externalId);

    /**
     * Retrieves all child institutions linked to a specific parent UUID.
     * * @param parentExternalId The UUID of the parent institution.
     * @return A list of institutions belonging to the specified parent.
     */
    List<Institution> findByParent_ExternalId(UUID parentExternalId);

}
