package com.projectfaust.repository;

import com.projectfaust.entity.PersonAccountRelation;
import com.projectfaust.entity.enums.PersonAccountRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PersonAccountRelationRepository extends JpaRepository<PersonAccountRelation, Long> {

    /**
     * Najde všechny aktivní vazby konkrétního člověka podle jeho publicId.
     */
    @Query("SELECT r FROM PersonAccountRelation r WHERE r.person.externalId = :personId AND r.active = true")
    List<PersonAccountRelation> findActiveRelationsByPersonId(@Param("personId") UUID personId);

    /**
     * Vyhledá lidi, kteří mají nebo měli specifickou roli (např. BENEFICIARY - skrytý vlastník)
     * k účtům v zadaném časovém rozmezí.
     */
    @Query("SELECT r FROM PersonAccountRelation r " +
            "WHERE r.roleType = :role " +
            "AND (r.validFrom <= :end AND (r.validTo >= :start OR r.validTo IS NULL))")
    List<PersonAccountRelation> findIntersectionsByRoleAndTime(
            @Param("role") PersonAccountRole role,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
