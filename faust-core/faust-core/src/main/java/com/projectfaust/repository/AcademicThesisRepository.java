package com.projectfaust.repository;

import com.projectfaust.entity.AcademicThesis;
import com.projectfaust.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pro správu záznamů o akademických pracích získaných z externích registrů (Theses.cz atd.).
 * Zajišťuje integritu dat v rámci Kafka pipeline a umožňuje analytické dotazy na vazby osob.
 */
@Repository
public interface AcademicThesisRepository extends JpaRepository<AcademicThesis, Long> {

    /**
     * Kontrola existence podle ID v externím systému (např. "theses:12345").
     * Klíčové pro zamezení duplicit při opakovaném scrapování nebo re-processingu Kafky.
     */
    boolean existsBySourceSystemId(String sourceSystemId);

    /**
     * Vyhledání práce podle jejího veřejného UUID v rámci systému Faust.
     */
    Optional<AcademicThesis> findByExternalId(UUID externalId);

    /**
     * Vrátí všechny práce navázané na konkrétní osobu.
     * Používá se v profilu osoby v Dashboardu.
     */
    List<AcademicThesis> findAllByPerson(Person person);

    /**
     * Najde práce, které ještě nebyly ověřeny agentem.
     * Slouží pro "In-box" analytika k manuálnímu potvrzení shody.
     */
    List<AcademicThesis> findAllByIsVerifiedByAgentFalse();

    /**
     * Analytický dotaz: Najde všechny lidi, kteří psali práci u stejného vedoucího.
     * To je ten moment, kdy v Project Faust odhalujeme "akademické kliky".
     */
    List<AcademicThesis> findAllBySupervisorNameContainingIgnoreCase(String supervisorName);
}
