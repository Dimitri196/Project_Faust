package com.projectfaust.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for direct querying of {@link VehicleOwnershipHistory} records.
 *
 * <p>Most history access goes through the owning {@link VehicleRecord}
 * collection. This repository is used when querying cross-vehicle ownership
 * chains (e.g. "all vehicles ever owned by person X") or when appending
 * a single transfer event without loading the full parent entity.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface VehicleOwnershipHistoryRepository extends JpaRepository<VehicleOwnershipHistory, Long> {

    Optional<VehicleOwnershipHistory> findByExternalId(UUID externalId);

    // ── By vehicle ────────────────────────────────────────────────────────────

    /**
     * Returns the full transfer history for a vehicle, ordered most-recent first.
     */
    @Query("SELECT h FROM VehicleOwnershipHistory h " +
           "WHERE h.vehicleRecord.externalId = :vehiclePublicId " +
           "ORDER BY h.transferDate DESC")
    List<VehicleOwnershipHistory> findByVehiclePublicId(
            @Param("vehiclePublicId") UUID vehiclePublicId);

    // ── Cross-vehicle ownership traces ────────────────────────────────────────

    /**
     * Returns all history rows in which the specified person was the <em>to</em>
     * (incoming) owner — i.e. vehicles acquired by this person, across all records.
     */
    @Query("SELECT h FROM VehicleOwnershipHistory h " +
           "JOIN FETCH h.vehicleRecord v " +
           "WHERE h.toPerson.externalId = :personExternalId " +
           "ORDER BY h.transferDate DESC")
    List<VehicleOwnershipHistory> findAcquisitionsByPerson(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all history rows in which the specified person was the <em>from</em>
     * (outgoing) owner — i.e. vehicles disposed of by this person.
     */
    @Query("SELECT h FROM VehicleOwnershipHistory h " +
           "JOIN FETCH h.vehicleRecord v " +
           "WHERE h.fromPerson.externalId = :personExternalId " +
           "ORDER BY h.transferDate DESC")
    List<VehicleOwnershipHistory> findDisposalsByPerson(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all history rows in which the specified institution was the
     * incoming owner (fleet acquisition trace).
     */
    @Query("SELECT h FROM VehicleOwnershipHistory h " +
           "JOIN FETCH h.vehicleRecord v " +
           "WHERE h.toInstitution.externalId = :institutionExternalId " +
           "ORDER BY h.transferDate DESC")
    List<VehicleOwnershipHistory> findAcquisitionsByInstitution(
            @Param("institutionExternalId") UUID institutionExternalId);

    // ── Date range ────────────────────────────────────────────────────────────

    /**
     * Returns all ownership transfers within a date range — useful for
     * time-boxed intelligence reports.
     */
    @Query("SELECT h FROM VehicleOwnershipHistory h " +
           "JOIN FETCH h.vehicleRecord v " +
           "WHERE h.transferDate BETWEEN :from AND :to " +
           "ORDER BY h.transferDate DESC")
    List<VehicleOwnershipHistory> findByTransferDateBetween(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);
}
