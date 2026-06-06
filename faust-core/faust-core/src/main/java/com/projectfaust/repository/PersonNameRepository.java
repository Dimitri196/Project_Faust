package com.projectfaust.repository;

import com.projectfaust.entity.PersonName;
import com.projectfaust.entity.enums.NameType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository pro správu jednotlivých identit a jmen subjektů.
 * Umožňuje detailní operace s aliasy, rodnými jmény a historickými záznamy.
 */
@Repository
public interface PersonNameRepository extends JpaRepository<PersonName, Long> {

    /**
     * Načte všechna jména spojená s konkrétní osobou (historie + aliasy).
     */
    List<PersonName> findAllByPersonExternalId(UUID externalId);

    /**
     * Najde aktuálně používané (primární) jméno osoby.
     */
    @Query("SELECT pn FROM PersonName pn WHERE pn.isPrimary = true AND pn.person.externalId = :externalId")
    Optional<PersonName> findPrimaryNameByExternalId(@Param("externalId") UUID externalId);

    /**
     * Vyhledá všechna jména konkrétního typu (např. všechny ALIASy v systému).
     */
    List<PersonName> findAllByType(NameType type);

    /**
     * Najde jméno, které bylo platné k určitému datu (pro historické reporty).
     */
    @Query("SELECT pn FROM PersonName pn WHERE pn.person.externalId = :externalId " +
            "AND (pn.validFrom <= CURRENT_DATE OR pn.validFrom IS NULL) " +
            "AND (pn.validTo >= CURRENT_DATE OR pn.validTo IS NULL)")
    List<PersonName> findActiveNamesAtDate(@Param("externalId") UUID externalId);

    /**
     * Deaktivuje všechna primární jména pro danou osobu.
     * Používá se těsně před nastavením nového primárního jména (např. po sňatku).
     */
    @Modifying
    @Query("UPDATE PersonName pn SET pn.isPrimary = false WHERE pn.person.id = " +
            "(SELECT p.id FROM Person p WHERE p.externalId = :externalId)")
    void deactivateAllPrimaryNames(@Param("externalId") UUID externalId);
}
