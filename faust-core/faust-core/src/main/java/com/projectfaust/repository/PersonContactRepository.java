package com.projectfaust.repository;

import com.projectfaust.entity.PersonContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonContactRepository extends JpaRepository<PersonContact, Long> {

    Optional<PersonContact> findByExternalId(UUID externalId);

    List<PersonContact> findAllByPersonExternalId(UUID personExternalId);

    /**
     * Vyhledá sdílenou infrastrukturu. Najde všechny kontakty se stejnou normalizovanou
     * hodnotou, které ale patří JINÝM osobám, než je zadané ID.
     * Klíčové pro odhalení skrytých vazeb ve SPA grafu.
     */
    @Query("SELECT c FROM PersonContact c WHERE c.contactValueNormalized = :normalizedValue AND c.person.externalId <> :personPublicId")
    List<PersonContact> findSharedInfrastructureLeaks(
            @Param("normalizedValue") String normalizedValue,
            @Param("personPublicId") UUID personPublicId
    );
}