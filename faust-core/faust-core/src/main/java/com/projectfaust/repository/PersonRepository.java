package com.projectfaust.repository;

import com.projectfaust.entity.Person;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository orchestrating data access for target identity matrices within Project Faust.
 * Provides optimized graph hydration (Deep Fetching) to eliminate N+1 queries in the SPA,
 * historical identity verification, and multi-vector telemetry search operations.
 * * @author Dimitri
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Retrieves a subject by their global structural identifier (externalId).
     */
    Optional<Person> findByExternalId(UUID externalId);

    /**
     * Resolves a collection of targets using a batch of public UUID tokens.
     */
    List<Person> findAllByExternalIdIn(Collection<UUID> externalIds);

    // =========================================================================
    // OVERHAUL: TECHNICAL FOOTPRINT & TELEMETRY SEARCH VECTORS
    // =========================================================================

    /**
     * Identifies all targets associated with any of the provided normalized contact values.
     * Critical for cross-referencing batch intelligence leaks against known subjects.
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.contacts c WHERE c.contactValueNormalized IN :values")
    List<Person> findAllByNormalizedContactsIn(@Param("values") Collection<String> values);

    /**
     * Resolves identities utilizing a specific hardware identity footprint (IMEI).
     * Used by the SPA to visualize phone-sharing anomalies across target clusters.
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.contacts c WHERE c.imei = :imei")
    List<Person> findAllByHardwareImei(@Param("imei") String imei);

    /**
     * Direct exact-match lookup over normalized communication vectors.
     * Replaces the legacy flat 'existsByEmail' constraint.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.contacts c " +
            "WHERE c.contactType = :type AND c.contactValueNormalized = :normalizedValue")
    boolean existsByContactVector(@Param("type") ContactType type, @Param("normalizedValue") String normalizedValue);

    // =========================================================================
    // IDENTITY & HISTORICAL NAME RESOLUTION
    // =========================================================================

    /**
     * Verifies identity existence through the historical sub-ledger (person_names).
     * Matches legal names, active aliases, or historical/maiden names.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.names n " +
            "WHERE LOWER(n.firstName) = LOWER(:firstName) AND LOWER(n.lastName) = LOWER(:lastName)")
    boolean existsByName(@Param("firstName") String firstName, @Param("lastName") String lastName);

    /**
     * Pre-calculated rapid text search leveraging a database-level trigger aggregation.
     * Optimized for high-speed autocomplete fields in the SPA dossier list.
     */
    @Query("SELECT p FROM Person p WHERE p.fullNameSearchNormalized LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByFullName(@Param("query") String query);

    /**
     * Deep analytical search scanning the complete un-aggregated identity history table.
     * uncovers targets using historic aliases, cover names, or compromised maiden names
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.names n " +
            "WHERE LOWER(n.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(n.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByAnyName(@Param("query") String query);

    // =========================================================================
    // OPTIMIZED GRAPH HYDRATION (SPA LAYER COUPLING)
    // =========================================================================

    /**
     * Performs a deep eager fetch of the target identity dossier, loading complete
     * identity histories and technical footprints in a unified database transaction.
     * Eliminates N+1 query performance degradation over REST endpoints.
     */
    @Query("SELECT DISTINCT p FROM Person p " +
            "LEFT JOIN FETCH p.names n " +
            "LEFT JOIN FETCH p.contacts c " +
            "LEFT JOIN FETCH p.financialAccounts fa " +
            "LEFT JOIN FETCH fa.bankAccount ba " +
            "WHERE p.externalId = :externalId")
    Optional<Person> findFullProfileByExternalId(@Param("externalId") UUID externalId);

    /**
     * Filters active profiles based on operating clearance requirements.
     */
    @Query("SELECT p FROM Person p WHERE p.clearanceLevel = :level")
    List<Person> findAllByClearanceLevel(@Param("level") ClearanceLevel level);

    /**
     * Vyhledá osoby, které jsou jakkoliv navázané na zadaný IBAN.
     * Používá se pro křížovou linkovou analýzu úniků dat (FININT).
     */
    @Query("SELECT DISTINCT p FROM Person p " +
            "JOIN p.financialAccounts fa " +
            "JOIN fa.bankAccount ba " +
            "WHERE ba.iban = :iban")
    List<Person> findAllByLinkedIban(@Param("iban") String iban);
}