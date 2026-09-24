package com.projectfaust.ingest.core;

import com.projectfaust.shared.enums.SourceSystem;
import com.projectfaust.shared.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link ExternalContract} persistence.
 *
 * <p>FIXED: Removed ORDER BY clauses from JPQL queries that are called with
 * {@code Pageable.unpaged()} — mixing JPQL ORDER BY with Pageable.unpaged()
 * causes HibernateJpaDialect conflicts in Spring Boot 3+. Sorting is now
 * handled in the service layer after fetching all results.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface ExternalContractRepository extends JpaRepository<ExternalContract, UUID> {

    // -------------------------------------------------------------------------
    // Deduplication
    // -------------------------------------------------------------------------

    boolean existsBySourceSystemAndExternalId(SourceSystem sourceSystem, String externalId);

    Optional<ExternalContract> findBySourceSystemAndExternalId(
            SourceSystem sourceSystem, String externalId);

    // -------------------------------------------------------------------------
    // Institution-scoped queries
    // -------------------------------------------------------------------------

    /**
     * Returns all contracts where the registration number appears as buyer OR supplier.
     * Called with {@code Pageable.unpaged()} from the service — no ORDER BY here.
     */
    @Query("SELECT c FROM ExternalContract c " +
            "WHERE c.buyerIco = :registrationNumber " +
            "OR c.supplierIco = :registrationNumber2")
    Page<ExternalContract> findByBuyerIcoOrSupplierIco(
            @Param("registrationNumber")  String registrationNumber,
            @Param("registrationNumber2") String registrationNumber2,
            Pageable pageable);

    /**
     * Returns contracts where the buyer matches AND the supplier matches the
     * search term. Called with {@code Pageable.unpaged()} — no ORDER BY.
     */
    @Query("SELECT c FROM ExternalContract c " +
            "WHERE c.buyerIco = :buyerRegistrationNumber " +
            "AND (LOWER(c.supplierName) LIKE LOWER(CONCAT('%', :supplierQuery, '%')) " +
            "     OR c.supplierIco = :supplierQuery " +
            "     OR c.supplierDic = :supplierQuery)")
    Page<ExternalContract> findByBuyerAndSupplierSearch(
            @Param("buyerRegistrationNumber") String buyerRegistrationNumber,
            @Param("supplierQuery")           String supplierQuery,
            Pageable pageable);

    /**
     * Count contracts for a registration number — used for fetch-on-demand check.
     */
    @Query("SELECT COUNT(c) FROM ExternalContract c " +
            "WHERE c.buyerIco = :registrationNumber OR c.supplierIco = :registrationNumber")
    long countByRegistrationNumber(@Param("registrationNumber") String registrationNumber);

    Page<ExternalContract> findAllByBuyerIcoOrderByContractDateDesc(
            String registrationNumber, Pageable pageable);

    Page<ExternalContract> findAllBySupplierIcoOrderByContractDateDesc(
            String registrationNumber, Pageable pageable);

    // -------------------------------------------------------------------------
    // Cross-institution supplier search
    // -------------------------------------------------------------------------

    @Query("SELECT c FROM ExternalContract c " +
            "WHERE LOWER(c.supplierName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR c.supplierIco = :query " +
            "OR c.supplierDic = :query " +
            "ORDER BY c.contractDate DESC NULLS LAST")
    Page<ExternalContract> searchBySupplier(
            @Param("query") String query, Pageable pageable);

    @Query("SELECT c FROM ExternalContract c " +
            "WHERE LOWER(c.supplierName) LIKE LOWER(CONCAT('%', :supplierName, '%')) " +
            "ORDER BY c.contractDate DESC NULLS LAST")
    Page<ExternalContract> findBySupplierNameContaining(
            @Param("supplierName") String supplierName, Pageable pageable);

    // -------------------------------------------------------------------------
    // Analyst queue
    // -------------------------------------------------------------------------

    Page<ExternalContract> findByVerificationStatus(VerificationStatus status, Pageable pageable);
}