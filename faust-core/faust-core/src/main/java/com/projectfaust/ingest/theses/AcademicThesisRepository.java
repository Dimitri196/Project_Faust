package com.projectfaust.ingest.theses;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for academic theses linked to FAUST persons.
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID)
 * or {@code sourceSystemId} (external registry string). The internal {@code Long id}
 * is never exposed outside the persistence layer — same convention as every
 * other repository in Project Faust.</p>
 *
 * <p><b>Key query categories:</b></p>
 * <ul>
 *   <li>Idempotency — {@link #existsBySourceSystemId} prevents duplicate ingest.</li>
 *   <li>Person lookup — all theses for a given person, used by dossier display.</li>
 *   <li>Relationship graph — supervisor/opponent name lookups for network analysis.</li>
 *   <li>Unverified — analyst queue of theses awaiting human confirmation.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface AcademicThesisRepository extends JpaRepository<AcademicThesis, Long> {

    // -------------------------------------------------------------------------
    // Idempotency — checked before every ingest to prevent duplicates.
    // Same deduplication pattern as ExternalContractRepository.
    // -------------------------------------------------------------------------

    /**
     * Checks whether a thesis with the given external registry ID already exists.
     * Called before every ingest operation — if true, the record is skipped.
     *
     * @param sourceSystemId the external registry ID (e.g. "semanticscholar:abc123").
     * @return {@code true} if the thesis is already persisted.
     */
    boolean existsBySourceSystemId(String sourceSystemId);

    // -------------------------------------------------------------------------
    // Single record lookup
    // -------------------------------------------------------------------------

    /**
     * Retrieves a thesis by its public UUID.
     *
     * @param externalId the public UUID of the thesis.
     * @return the thesis if found.
     */
    Optional<AcademicThesis> findByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Person-scoped lookups — used by dossier and person detail pages.
    // -------------------------------------------------------------------------

    /**
     * Returns all theses linked to the given person, ordered most recent first.
     *
     * @param personExternalId the public UUID of the FAUST Person.
     * @return list of theses, never null.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE t.person.externalId = :personExternalId " +
            "ORDER BY t.defenseYear DESC NULLS LAST")
    List<AcademicThesis> findAllByPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Returns only verified theses for a person — used when displaying
     * confirmed data in the public-facing dossier view.
     *
     * @param personExternalId the public UUID of the FAUST Person.
     * @return list of analyst-verified theses.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE t.person.externalId = :personExternalId " +
            "AND t.verifiedByAgent = true " +
            "ORDER BY t.defenseYear DESC NULLS LAST")
    List<AcademicThesis> findVerifiedByPersonExternalId(
            @Param("personExternalId") UUID personExternalId);

    /**
     * Counts theses linked to a person — used for the hasTheses flag
     * without loading the full collection.
     *
     * @param personExternalId the public UUID of the FAUST Person.
     * @return count of linked theses.
     */
    long countByPersonExternalId(UUID personExternalId);

    // -------------------------------------------------------------------------
    // Relationship graph — supervisor and opponent name resolution.
    // These power the academic network graph: who supervised whom,
    // who examined whom. Used to suggest Person linkages to analysts.
    // -------------------------------------------------------------------------

    /**
     * Finds all theses supervised by a person with the given name.
     * Used to build the supervisor → student relationship graph and to
     * suggest a {@code Person} FK match when a supervisor name appears
     * in an ingested thesis but hasn't been manually verified yet.
     *
     * @param supervisorName partial or full supervisor name (case-insensitive).
     * @return list of matching theses.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE LOWER(t.supervisorName) LIKE LOWER(CONCAT('%', :supervisorName, '%'))")
    List<AcademicThesis> findBySupervisorNameContainingIgnoreCase(
            @Param("supervisorName") String supervisorName);

    /**
     * Finds all theses opposed by a person with the given name.
     * Same pattern as {@link #findBySupervisorNameContainingIgnoreCase} —
     * used for the opponent → student relationship graph.
     *
     * @param opponentName partial or full opponent name (case-insensitive).
     * @return list of matching theses.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE LOWER(t.opponentName) LIKE LOWER(CONCAT('%', :opponentName, '%'))")
    List<AcademicThesis> findByOpponentNameContainingIgnoreCase(
            @Param("opponentName") String opponentName);

    // -------------------------------------------------------------------------
    // Analyst queue — unverified theses awaiting human confirmation.
    // -------------------------------------------------------------------------

    /**
     * Returns all theses that have not yet been verified by an analyst.
     * Used to populate the analyst review queue — theses where the author
     * name match against a FAUST Person hasn't been manually confirmed.
     *
     * @return list of unverified theses ordered by creation date, newest first.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE t.verifiedByAgent = false " +
            "ORDER BY t.createdAt DESC")
    List<AcademicThesis> findAllUnverified();

    /**
     * Counts unverified theses — used for the analyst dashboard badge count.
     *
     * @return number of theses awaiting analyst verification.
     */
    @Query("SELECT COUNT(t) FROM AcademicThesis t WHERE t.verifiedByAgent = false")
    long countUnverified();

    // -------------------------------------------------------------------------
    // University / institution scoped lookups
    // -------------------------------------------------------------------------

    /**
     * Returns all theses from a specific university — used to map academic
     * output by institution, which feeds the institution dossier's
     * "Academic Output" panel (future feature).
     *
     * @param universityName exact university name.
     * @return list of theses from that university ordered by year desc.
     */
    @Query("SELECT t FROM AcademicThesis t " +
            "WHERE t.universityName = :universityName " +
            "ORDER BY t.defenseYear DESC NULLS LAST")
    List<AcademicThesis> findByUniversityName(
            @Param("universityName") String universityName);

    /**
     * Returns all classified theses — restricted to ANALYST+ access.
     * Used for the classified academic intelligence view.
     *
     * @return list of classified theses.
     */
    @Query("SELECT t FROM AcademicThesis t WHERE t.classified = true " +
            "ORDER BY t.defenseYear DESC NULLS LAST")
    List<AcademicThesis> findAllClassified();
}