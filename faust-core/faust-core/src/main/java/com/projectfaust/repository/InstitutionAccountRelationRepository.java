package com.projectfaust.repository;

import com.projectfaust.entity.InstitutionAccountRelation;
import com.projectfaust.entity.enums.InstitutionAccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InstitutionAccountRelationRepository extends JpaRepository<InstitutionAccountRelation, Long> {

    /**
     * Najde všechny účty určitého typu (např. DISCRETIONARY_FUND - tajné fondy) napříč všemi institucemi.
     */
    List<InstitutionAccountRelation> findByRoleTypeAndActiveTrue(InstitutionAccountRole roleType);

    /**
     * Najde všechny finanční uzly navázané na konkrétní úřad/složku podle publicId.
     */
    @Query("SELECT r FROM InstitutionAccountRelation r WHERE r.institution.externalId = :institutionId")
    List<InstitutionAccountRelation> findAllByInstitutionId(@Param("institutionId") UUID institutionId);
}