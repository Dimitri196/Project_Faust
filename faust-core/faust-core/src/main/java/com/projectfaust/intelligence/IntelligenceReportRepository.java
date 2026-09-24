package com.projectfaust.intelligence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing AI-generated intelligence reports within Project Faust.
 *
 * <p>All person-based lookups use {@code person.externalId} rather than the
 * full {@link com.projectfaust.person.Person} entity to avoid requiring an
 * entity load before querying reports.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface IntelligenceReportRepository extends JpaRepository<IntelligenceReport, Long> {

    /**
     * Retrieves a report by its public UUID.
     *
     * @param externalId the public UUID of the report.
     * @return an {@link Optional} containing the report, or empty if not found.
     */
    Optional<IntelligenceReport> findByExternalId(UUID externalId);

    /**
     * Retrieves the most recently generated report for a specific person.
     *
     * @param personExternalId the public UUID of the subject person.
     * @return an {@link Optional} containing the latest report, or empty if none exists.
     */
    Optional<IntelligenceReport> findFirstByPersonExternalIdOrderByGeneratedAtDesc(
            UUID personExternalId);

    /**
     * Retrieves all reports generated for a specific person, newest first.
     *
     * @param personExternalId the public UUID of the subject person.
     * @return list of all reports for the person.
     */
    List<IntelligenceReport> findAllByPersonExternalIdOrderByGeneratedAtDesc(
            UUID personExternalId);
}