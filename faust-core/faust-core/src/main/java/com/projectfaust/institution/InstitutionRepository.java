package com.projectfaust.institution;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Institution} entities within Project Faust.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support the dynamic filter
 * predicates assembled by {@link InstitutionSpecifications}.</p>
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long} primary key is never exposed outside the persistence layer.</p>
 *
 * <p><b>JOIN FETCH strategy:</b> several queries use {@code LEFT JOIN FETCH} with
 * {@code DISTINCT} to eagerly load associations in a single SQL round trip.
 * {@code DISTINCT} is required to prevent Hibernate row multiplication when
 * joining one-to-many collections.</p>
 *
 * <p><b>Hierarchy depth:</b> {@link #findWithDeepHierarchyByExternalId} loads two
 * levels of children via {@code @EntityGraph}. For hierarchies exceeding three levels,
 * replace with a recursive CTE query following the pattern in {@code LocationRepository}.</p>
 *
 * <p><b>Active children flag:</b> {@link #existsActiveChildByParentExternalId} is
 * preferred over {@link #existsByParentExternalId} when resolving the {@code hasChildren}
 * flag — it excludes inactive children so deactivated sub-agencies do not falsely
 * mark a parent as having children.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface InstitutionRepository extends
        JpaRepository<Institution, Long>,
        JpaSpecificationExecutor<Institution> {

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Checks whether an institution with the given public UUID exists.
     *
     * <p>Preferred over {@link #findByExternalId} when only existence needs to be
     * confirmed — avoids loading the full entity graph.</p>
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching institution exists.
     */
    boolean existsByExternalId(UUID externalId);

    /**
     * Checks whether any institution (active or inactive) has the given institution
     * as its parent.
     *
     * <p>Use {@link #existsActiveChildByParentExternalId} instead when resolving the
     * {@code hasChildren} display flag — inactive children should not mark a parent
     * as having children in the UI.</p>
     *
     * @param parentExternalId the public UUID of the candidate parent.
     * @return {@code true} if at least one child institution exists.
     */
    boolean existsByParentExternalId(UUID parentExternalId);

    /**
     * Checks whether any <em>active</em> institution has the given institution as
     * its parent.
     *
     * <p>Used by {@link InstitutionService#toEnrichedResponse} to resolve the
     * {@code hasChildren} flag before calling
     * {@link InstitutionMapper#toResponse(Institution, boolean)}, avoiding both a
     * lazy collection load inside the mapper and false positives from deactivated
     * child nodes.</p>
     *
     * @param parentExternalId the public UUID of the candidate parent.
     * @return {@code true} if at least one active child institution exists.
     */
    @Query("SELECT COUNT(i) > 0 FROM Institution i " +
            "WHERE i.parent.externalId = :parentExternalId AND i.active = true")
    boolean existsActiveChildByParentExternalId(
            @Param("parentExternalId") UUID parentExternalId);

    // -------------------------------------------------------------------------
    // Single entity lookups
    // -------------------------------------------------------------------------

    /**
     * Retrieves an institution by its public UUID.
     *
     * <p>Standard lookup method for all service-layer operations.
     * The internal {@code Long} primary key is not used outside the persistence layer.</p>
     *
     * @param externalId the public UUID of the institution.
     * @return an {@link Optional} containing the institution, or empty if not found.
     */
    Optional<Institution> findByExternalId(UUID externalId);

    /**
     * Retrieves a single institution with its first two levels of child hierarchy
     * eagerly loaded, for use with the Nexus Focus deep graph visualisation.
     *
     * <p>The {@code @EntityGraph} loads {@code children} and {@code children.children}
     * in a single query, preventing lazy-load exceptions when
     * {@link InstitutionMapper#toTreeResponse} is called. For hierarchies deeper than
     * two levels, this method will not load the full tree — use a recursive CTE query
     * for unlimited depth.</p>
     *
     * <p><b>Note:</b> results from this method are safe to pass to
     * {@link InstitutionMapper#toTreeResponse} (deep). Do not use results from
     * {@link #findAllRoots} or {@link #findActiveChildrenByParentExternalId} for deep
     * tree mapping — those queries only load one level of children.</p>
     *
     * @param externalId the public UUID of the root institution.
     * @return an {@link Optional} containing the institution with two levels pre-loaded.
     */
    @Query("SELECT DISTINCT i FROM Institution i WHERE i.externalId = :externalId")
    @EntityGraph(attributePaths = {"children", "children.children"})
    Optional<Institution> findWithDeepHierarchyByExternalId(@Param("externalId") UUID externalId);

    /**
     * Retrieves a complete institution profile including financial account relations
     * and their underlying bank account details.
     *
     * <p>The two-level {@code JOIN FETCH} prevents lazy-load exceptions when rendering
     * the financial dossier panel. Both the {@code InstitutionAccountRelation} join
     * table and the referenced {@code BankAccount} entity are loaded in one query.</p>
     *
     * @param externalId the public UUID of the institution.
     * @return an {@link Optional} containing the fully-loaded institution profile.
     */
    @Query("SELECT DISTINCT i FROM Institution i " +
            "LEFT JOIN FETCH i.financialAccounts fa " +
            "LEFT JOIN FETCH fa.bankAccount ba " +
            "WHERE i.externalId = :externalId")
    Optional<Institution> findFullProfileByExternalId(@Param("externalId") UUID externalId);

    // -------------------------------------------------------------------------
    // Collection queries
    // -------------------------------------------------------------------------

    /**
     * Returns all active root-level institutions (institutions with no parent).
     *
     * <p>{@code DISTINCT} prevents row multiplication from the {@code LEFT JOIN FETCH}
     * on the children collection. Root institutions are the top of the organisational
     * hierarchy — typically national-level bodies such as ministries.</p>
     *
     * <p><b>Tree mapping note:</b> results from this query have only direct children
     * pre-loaded. Pass results to {@link InstitutionMapper#toFlatTreeResponse} only —
     * never to {@link InstitutionMapper#toTreeResponse} (deep).</p>
     *
     * @return list of active root institutions with direct children pre-loaded.
     */
    @Query("SELECT DISTINCT i FROM Institution i " +
            "LEFT JOIN FETCH i.children " +
            "WHERE i.parent IS NULL AND i.active = true")
    List<Institution> findAllRoots();

    /**
     * Returns all active direct children of the specified parent institution.
     *
     * <p>{@code DISTINCT} guards against potential Hibernate row multiplication.
     * Children are fetched with their own direct children pre-loaded, enabling
     * one level of drill-down without additional queries.</p>
     *
     * <p><b>Tree mapping note:</b> results from this query have only direct children
     * pre-loaded. Pass results to {@link InstitutionMapper#toFlatTreeResponse} only —
     * never to {@link InstitutionMapper#toTreeResponse} (deep).</p>
     *
     * @param parentExternalId the public UUID of the parent institution.
     * @return list of active child institutions with their direct children pre-loaded.
     */
    @Query("SELECT DISTINCT i FROM Institution i " +
            "LEFT JOIN FETCH i.children " +
            "WHERE i.parent.externalId = :parentExternalId AND i.active = true")
    List<Institution> findActiveChildrenByParentExternalId(
            @Param("parentExternalId") UUID parentExternalId);

    // -------------------------------------------------------------------------
    // Ancestor resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves the root ancestor of any node in the hierarchy using a recursive CTE.
     *
     * <p>Traverses the parent chain upward from the given node until it reaches
     * a node with no parent — the organisational root. This replaces the N+1
     * Java-side parent walk ({@code while root.getParent() != null}) with a
     * single database round trip.</p>
     *
     * <p>Used by {@link InstitutionService#getNexusFocus(UUID)} to anchor the
     * Nexus Focus visualisation at the correct root regardless of which node
     * in the hierarchy was requested.</p>
     *
     * @param descendantExternalId the public UUID of any node in the hierarchy.
     * @return an {@link Optional} containing the root ancestor institution,
     *         or empty if no institution matches the given UUID.
     */
    @Query(value = """
            WITH RECURSIVE ancestors AS (
                SELECT * FROM institutions
                WHERE external_id = :descendantExternalId
                UNION ALL
                SELECT i.* FROM institutions i
                INNER JOIN ancestors a ON i.id = a.parent_id
            )
            SELECT * FROM ancestors
            WHERE parent_id IS NULL
            LIMIT 1
            """, nativeQuery = true)
    Optional<Institution> findRootByDescendantExternalId(
            @Param("descendantExternalId") UUID descendantExternalId);
}
