package com.projectfaust.financial;

import com.projectfaust.shared.enums.InstitutionAccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link InstitutionAccountRelation} entities.
 *
 * @author Dimitri / Project Faust
 */
public interface InstitutionAccountRelationRepository extends JpaRepository<InstitutionAccountRelation, Long> {

    /**
     * All accounts linked to a given institution.
     */
    @Query("""
            SELECT r FROM InstitutionAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.institution.externalId = :institutionExternalId
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<InstitutionAccountRelation> findAllByInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    /**
     * Active relations only for a given institution.
     */
    @Query("""
            SELECT r FROM InstitutionAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.institution.externalId = :institutionExternalId
              AND r.active = true
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<InstitutionAccountRelation> findActiveByInstitutionExternalId(
            @Param("institutionExternalId") UUID institutionExternalId);

    /**
     * All institutions linked to a given account.
     */
    @Query("""
            SELECT r FROM InstitutionAccountRelation r
            JOIN FETCH r.institution i
            WHERE r.bankAccount.externalId = :accountExternalId
            ORDER BY r.roleType ASC
            """)
    List<InstitutionAccountRelation> findAllByAccountExternalId(
            @Param("accountExternalId") UUID accountExternalId);

    /**
     * Relations filtered by institutional role type.
     */
    @Query("""
            SELECT r FROM InstitutionAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.institution.externalId = :institutionExternalId
              AND r.roleType = :role
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<InstitutionAccountRelation> findByInstitutionAndRole(
            @Param("institutionExternalId") UUID institutionExternalId,
            @Param("role") InstitutionAccountRole role);

    /**
     * Duplicate-guard: checks whether a specific institution–account–role triple already exists.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM InstitutionAccountRelation r
            WHERE r.institution.externalId = :institutionExternalId
              AND r.bankAccount.externalId = :accountExternalId
              AND r.roleType = :role
            """)
    boolean existsByInstitutionAccountAndRole(
            @Param("institutionExternalId") UUID institutionExternalId,
            @Param("accountExternalId") UUID accountExternalId,
            @Param("role") InstitutionAccountRole role);
}
