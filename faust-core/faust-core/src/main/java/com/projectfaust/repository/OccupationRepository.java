package com.projectfaust.repository;

import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.enums.OccupationCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OccupationRepository extends JpaRepository<Occupation, Long> {

    Optional<Occupation> findByExternalId(UUID externalId);

    List<Occupation> findByInstitutionExternalId(UUID institutionId);

    List<Occupation> findByCategoryAndInstitutionCountryCode(OccupationCategory category, String countryCode);

    List<Occupation> findByReportsToExternalId(UUID reportsToExternalId);

    List<Occupation> findByReportsToId(Long reportsToId);

    @Query("SELECT o FROM Occupation o " +
            "LEFT JOIN FETCH o.institution " +
            "LEFT JOIN FETCH o.reportsTo " +
            "WHERE o.institution.externalId = :instId")
    List<Occupation> findByInstitutionExternalIdFetched(@Param("instId") UUID instId);

    // Find top-level roles (e.g., Prime Minister, Ministers)
    @Query("SELECT o FROM Occupation o LEFT JOIN FETCH o.subordinates WHERE o.reportsTo IS NULL")
    List<Occupation> findTopLevelRoles();
}
