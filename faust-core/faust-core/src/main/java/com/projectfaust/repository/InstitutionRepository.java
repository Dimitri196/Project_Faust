package com.projectfaust.repository;

import com.projectfaust.entity.Institution;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends
        JpaRepository<Institution, Long>,
        JpaSpecificationExecutor<Institution> {

    // PŘIDÁNO DISTINCT: Bez něj by ti ČR (root) vyskočila v Listu tolikrát, kolik má ministerstev (children)
    @Query("SELECT DISTINCT i FROM Institution i LEFT JOIN FETCH i.children WHERE i.parent IS NULL AND i.active = true")
    List<Institution> findAllRoots();

    Optional<Institution> findByExternalId(UUID externalId);

    // PŘIDÁNO DISTINCT: Pokud by jeden odbor patřil pod více entit (v budoucnu), nebo kvůli Hibernate multiplikaci
    @Query("SELECT DISTINCT i FROM Institution i LEFT JOIN FETCH i.children WHERE i.parent.externalId = :parentExternalId AND i.active = true")
    List<Institution> findByParent_ExternalId(@Param("parentExternalId") UUID parentExternalId);

    // Tady už jsi ho měl - tohle je ta klíčová metoda pro Nexus Focus
    @Query("SELECT DISTINCT i FROM Institution i WHERE i.externalId = :externalId")
    @EntityGraph(attributePaths = {"children", "children.children"})
    Optional<Institution> findWithDeepHierarchyByExternalId(@Param("externalId") UUID externalId);

    /**
     * Načte kompletní profil instituce včetně jejích finančních uzlů a bankovních účtů.
     * Prevence LazyInitializationException při rendrování finančního dossieru úřadu.
     */
    @Query("SELECT DISTINCT i FROM Institution i " +
            "LEFT JOIN FETCH i.financialAccounts fa " +   // 👈 PŘIDÁNO: Finanční relace instituce
            "LEFT JOIN FETCH fa.bankAccount ba " +        // 👈 PŘIDÁNO: Samotné bankovní detaily
            "WHERE i.externalId = :externalId")
    Optional<Institution> findFullProfileByExternalId(@Param("externalId") UUID externalId);
}