package com.projectfaust.repository;

import com.projectfaust.entity.IntelligenceReport;
import com.projectfaust.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing intelligence reports generated within Project Faust.
 * Provides access to analytical dossiers and historical tracking of subject evaluations.
 */
@Repository
public interface IntelligenceReportRepository extends JpaRepository<IntelligenceReport, Long> {

    /**
     * Retrieves a specific intelligence report based on its global unique identifier.
     *
     * @param externalId The UUID assigned to the report.
     * @return An Optional containing the found IntelligenceReport, or empty if not found.
     */
    Optional<IntelligenceReport> findByExternalId(UUID externalId);

    /**
     * Retrieves the most recently generated intelligence report for a specific subject.
     * Useful for obtaining the latest risk assessment or profile snapshot.
     *
     * @param person The subject entity (Person) for whom the report was generated.
     * @return An Optional containing the latest report, ordered by the generation timestamp.
     */
    Optional<IntelligenceReport> findFirstByPersonOrderByGeneratedAtDesc(Person person);

}
