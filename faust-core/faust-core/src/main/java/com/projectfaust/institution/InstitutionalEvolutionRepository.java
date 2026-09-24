package com.projectfaust.institution;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionalEvolutionRepository extends JpaRepository<InstitutionalEvolution, Long> {

    Optional<InstitutionalEvolution> findByExternalId(UUID externalId);

    /**
     * Vyhledá evoluční událost, kde daná instituce figurovala jako předchůdce.
     */
    List<InstitutionalEvolution> findByPredecessor_ExternalId(UUID predecessorExternalId);

    /**
     * Vyhledá evoluční událost, kde daná instituce figurovala jako nástupce.
     */
    List<InstitutionalEvolution> findBySuccessor_ExternalId(UUID successorExternalId);

    /**
     * Komplexní dotaz pro Network Hud: najde celou časovou osu (všechny záznamy, kde ID figuruje).
     */
    @Query("SELECT e FROM InstitutionalEvolution e " +
            "WHERE e.predecessor.externalId = :id OR e.successor.externalId = :id " +
            "ORDER BY e.effectiveDate DESC")
    List<InstitutionalEvolution> findAllByInstitutionExternalId(@Param("id") UUID id);
}