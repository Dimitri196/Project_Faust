package com.projectfaust.insurance;

import com.projectfaust.shared.enums.InsuranceSourceSystem;
import com.projectfaust.shared.enums.InsuranceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing insurance policy records within Project Faust.
 *
 * <p>Extends both {@link JpaRepository} for standard CRUD and
 * {@link JpaSpecificationExecutor} to support the dynamic filter predicates
 * assembled by {@code InsuranceRecordSpecifications}.</p>
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long id} primary key is never exposed outside the
 * persistence layer.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface InsuranceRecordRepository extends JpaRepository<InsuranceRecord, Long>,
        JpaSpecificationExecutor<InsuranceRecord> {

    /**
     * Retrieves a single insurance record by its public UUID.
     *
     * @param externalId the public UUID of the record.
     * @return an {@link Optional} containing the record, or empty if not found.
     */
    Optional<InsuranceRecord> findByExternalId(UUID externalId);

    /**
     * Checks whether an insurance record with the given public UUID exists.
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching record exists.
     */
    boolean existsByExternalId(UUID externalId);

    /**
     * Returns all insurance records linked to the specified person.
     *
     * <p>Joins the person to avoid N+1 queries when rendering the full
     * insurance panel on a PersonDetailPage.</p>
     *
     * @param personExternalId the public UUID of the person.
     * @return list of insurance records for that person, never {@code null}.
     */
    @Query("SELECT ir FROM InsuranceRecord ir " +
            "JOIN FETCH ir.person p " +
            "WHERE p.externalId = :personExternalId " +
            "ORDER BY ir.validFrom DESC NULLS LAST")
    List<InsuranceRecord> findAllByPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all active insurance records linked to the specified person.
     *
     * @param personExternalId the public UUID of the person.
     * @return list of currently active policies for that person.
     */
    @Query("SELECT ir FROM InsuranceRecord ir " +
            "JOIN FETCH ir.person p " +
            "WHERE p.externalId = :personExternalId " +
            "AND ir.active = true " +
            "ORDER BY ir.validFrom DESC NULLS LAST")
    List<InsuranceRecord> findActiveByPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns all insurance records of a specific type for the given person.
     *
     * @param personExternalId the public UUID of the person.
     * @param insuranceType    the policy type to filter by.
     * @return list of matching records, ordered newest-first.
     */
    @Query("SELECT ir FROM InsuranceRecord ir " +
            "JOIN ir.person p " +
            "WHERE p.externalId = :personExternalId " +
            "AND ir.insuranceType = :insuranceType " +
            "ORDER BY ir.validFrom DESC NULLS LAST")
    List<InsuranceRecord> findByPersonExternalIdAndType(
            @Param("personExternalId") UUID personExternalId,
            @Param("insuranceType") InsuranceType insuranceType);

    /**
     * Looks up a record by the unique (sourceSystem, sourceReferenceId) pair.
     *
     * <p>Used during idempotent ingest: if a matching record already exists the
     * ingest pipeline updates it rather than creating a duplicate.</p>
     *
     * @param sourceSystem      the originating system.
     * @param sourceReferenceId the record's ID within that system.
     * @return an {@link Optional} containing the existing record if present.
     */
    Optional<InsuranceRecord> findBySourceSystemAndSourceReferenceId(
            InsuranceSourceSystem sourceSystem,
            String sourceReferenceId);

    /**
     * Returns all insurance records for a given insurer (by name, case-insensitive).
     *
     * @param insurerName the insurer name to match.
     * @return list of records for that insurer.
     */
    @Query("SELECT ir FROM InsuranceRecord ir " +
            "WHERE LOWER(ir.insurerName) = LOWER(:insurerName) " +
            "ORDER BY ir.ingestedAt DESC")
    List<InsuranceRecord> findByInsurerName(@Param("insurerName") String insurerName);
}