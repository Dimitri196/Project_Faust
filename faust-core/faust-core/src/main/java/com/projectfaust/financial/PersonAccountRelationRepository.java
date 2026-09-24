package com.projectfaust.financial;

import com.projectfaust.shared.enums.PersonAccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PersonAccountRelation} entities.
 *
 * @author Dimitri / Project Faust
 */
public interface PersonAccountRelationRepository extends JpaRepository<PersonAccountRelation, Long> {

    Optional<PersonAccountRelation> findByExternalId(UUID externalId);

    /**
     * All relations for a person — regardless of active status.
     */
    @Query("""
            SELECT r FROM PersonAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.person.externalId = :personExternalId
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<PersonAccountRelation> findAllByPersonExternalId(@Param("personExternalId") UUID personExternalId);

    /**
     * Active relations only for a person.
     */
    @Query("""
            SELECT r FROM PersonAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.person.externalId = :personExternalId
              AND r.active = true
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<PersonAccountRelation> findActiveByPersonExternalId(@Param("personExternalId") UUID personExternalId);

    /**
     * All persons linked to a given account.
     */
    @Query("""
            SELECT r FROM PersonAccountRelation r
            JOIN FETCH r.person p
            WHERE r.bankAccount.externalId = :accountExternalId
            ORDER BY r.roleType ASC
            """)
    List<PersonAccountRelation> findAllByAccountExternalId(@Param("accountExternalId") UUID accountExternalId);

    /**
     * Relations for a person filtered by role type.
     */
    @Query("""
            SELECT r FROM PersonAccountRelation r
            JOIN FETCH r.bankAccount ba
            WHERE r.person.externalId = :personExternalId
              AND r.roleType = :role
            ORDER BY r.validFrom DESC NULLS LAST
            """)
    List<PersonAccountRelation> findByPersonAndRole(
            @Param("personExternalId") UUID personExternalId,
            @Param("role") PersonAccountRole role);

    /**
     * Checks whether a specific person–account link already exists.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM PersonAccountRelation r
            WHERE r.person.externalId = :personExternalId
              AND r.bankAccount.externalId = :accountExternalId
              AND r.roleType = :role
            """)
    boolean existsByPersonAccountAndRole(
            @Param("personExternalId") UUID personExternalId,
            @Param("accountExternalId") UUID accountExternalId,
            @Param("role") PersonAccountRole role);
}
