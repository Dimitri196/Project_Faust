package com.projectfaust.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing vehicle records within Project Faust.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD and
 * {@link JpaSpecificationExecutor} to support dynamic filter predicates.</p>
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long id} primary key is never exposed outside the
 * persistence layer.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface VehicleRecordRepository extends JpaRepository<VehicleRecord, Long>,
        JpaSpecificationExecutor<VehicleRecord> {

    Optional<VehicleRecord> findByExternalId(UUID externalId);

    boolean existsByExternalId(UUID externalId);

    // ── Owner — person ────────────────────────────────────────────────────────

    /**
     * Returns all vehicles currently owned by the specified person.
     *
     * @param personExternalId public UUID of the person.
     * @return list of vehicle records, ordered by registration date descending.
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "JOIN FETCH v.ownerPerson p " +
           "WHERE p.externalId = :personExternalId " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findAllByOwnerPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all active vehicles owned by the specified person.
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "JOIN FETCH v.ownerPerson p " +
           "WHERE p.externalId = :personExternalId " +
           "AND v.active = true " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findActiveByOwnerPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    // ── Operator — person ─────────────────────────────────────────────────────

    /**
     * Returns all vehicles operated (but not necessarily owned) by the specified person.
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "JOIN FETCH v.operatorPerson p " +
           "WHERE p.externalId = :personExternalId " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findAllByOperatorPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    // ── Owner — institution ───────────────────────────────────────────────────

    /**
     * Returns all vehicles owned by the specified institution (fleet view).
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "JOIN FETCH v.ownerInstitution i " +
           "WHERE i.externalId = :institutionExternalId " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findAllByOwnerInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    /**
     * Returns all active vehicles owned by the specified institution.
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "JOIN FETCH v.ownerInstitution i " +
           "WHERE i.externalId = :institutionExternalId " +
           "AND v.active = true " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findActiveByOwnerInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    // ── Deduplication ─────────────────────────────────────────────────────────

    /**
     * Looks up a vehicle by the unique {@code (sourceSystem, sourceReferenceId)} pair.
     * Used during idempotent ingest.
     */
    Optional<VehicleRecord> findBySourceSystemAndSourceReferenceId(
            VehicleSourceSystem sourceSystem,
            String sourceReferenceId);

    /**
     * Looks up a vehicle by VIN (globally unique identifier).
     * Multiple results are theoretically impossible but returned as a list
     * to handle data quality issues.
     */
    @Query("SELECT v FROM VehicleRecord v WHERE v.vin = :vin ORDER BY v.ingestedAt DESC")
    List<VehicleRecord> findByVin(@Param("vin") String vin);

    /**
     * Looks up all vehicles by licence plate within a country.
     * A plate can be reissued over time, so multiple results are expected.
     */
    @Query("SELECT v FROM VehicleRecord v " +
           "WHERE v.licensePlate = :licensePlate " +
           "AND v.countryCode = :countryCode " +
           "ORDER BY v.registeredSince DESC NULLS LAST")
    List<VehicleRecord> findByLicensePlateAndCountry(
            @Param("licensePlate") String licensePlate,
            @Param("countryCode") String countryCode);
}
