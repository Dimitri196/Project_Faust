package com.projectfaust.ingest.realestate.core;

import com.projectfaust.shared.enums.CadasterSourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for real estate ownership records.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface RealEstateOwnershipRepository extends JpaRepository<RealEstateOwnership, Long> {

    // -------------------------------------------------------------------------
    // Idempotency
    // -------------------------------------------------------------------------

    boolean existsBySourceSystemAndSourceRegistryId(
            CadasterSourceSystem sourceSystem, String sourceRegistryId);

    // -------------------------------------------------------------------------
    // Single record
    // -------------------------------------------------------------------------

    Optional<RealEstateOwnership> findByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Person-scoped — for person dossier
    // -------------------------------------------------------------------------

    @Query("SELECT r FROM RealEstateOwnership r " +
            "WHERE r.person.externalId = :personId " +
            "ORDER BY r.ingestedAt DESC")
    List<RealEstateOwnership> findAllByPersonExternalId(
            @Param("personId") UUID personId);

    long countByPersonExternalId(UUID personId);

    // -------------------------------------------------------------------------
    // Institution-scoped — for institution dossier
    // -------------------------------------------------------------------------

    @Query("SELECT r FROM RealEstateOwnership r " +
            "WHERE r.institution.externalId = :institutionId " +
            "ORDER BY r.ingestedAt DESC")
    List<RealEstateOwnership> findAllByInstitutionExternalId(
            @Param("institutionId") UUID institutionId);

    long countByInstitutionExternalId(UUID institutionId);

    // -------------------------------------------------------------------------
    // Country-scoped — for geographic intelligence view
    // -------------------------------------------------------------------------

    @Query("SELECT r FROM RealEstateOwnership r " +
            "WHERE r.countryCode = :countryCode " +
            "ORDER BY r.ingestedAt DESC")
    List<RealEstateOwnership> findAllByCountryCode(@Param("countryCode") String countryCode);

    // -------------------------------------------------------------------------
    // Encumbered properties — financial risk signal
    // -------------------------------------------------------------------------

    @Query("SELECT r FROM RealEstateOwnership r " +
            "WHERE r.person.externalId = :personId " +
            "AND r.encumbered = true")
    List<RealEstateOwnership> findEncumberedByPersonExternalId(
            @Param("personId") UUID personId);
}