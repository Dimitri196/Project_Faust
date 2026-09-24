package com.projectfaust.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing person-to-occupation assignments within Project Faust.
 *
 * <p>Provides methods for tracking career progression, monitoring current office
 * holders, and querying appointment history across both persons and positions.</p>
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long} primary key is never exposed outside the persistence layer.</p>
 *
 * <p><b>Active appointment definition:</b> an appointment is active when its
 * {@code endDate} is null (ongoing) or today or in the future, and its
 * {@code startDate} is today or in the past. The derived method
 * {@link #findByOccupationExternalIdAndEndDateIsNull} uses the simpler
 * null-endDate heuristic which is correct for most cases — use
 * {@link #findActiveByOccupationExternalId} for strict date-bounded checks.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Checks whether an appointment with the given public UUID exists.
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching appointment exists.
     */
    boolean existsByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Single entity lookups
    // -------------------------------------------------------------------------

    /**
     * Retrieves an appointment by its public UUID.
     *
     * @param externalId the public UUID of the appointment.
     * @return an {@link Optional} containing the appointment, or empty if not found.
     */
    Optional<Appointment> findByExternalId(UUID externalId);

    /**
     * Finds the current incumbent of a position — the appointment with no end date.
     *
     * <p>Uses the null-endDate heuristic: a position is considered occupied when
     * an appointment exists with {@code endDate = null}. This is correct for
     * well-maintained data. For strict date-bounded active checks, use
     * {@link #findActiveByOccupationExternalId}.</p>
     *
     * @param occupationId the public UUID of the target position.
     * @return an {@link Optional} containing the current incumbent's appointment,
     *         or empty if the position is vacant.
     */
    Optional<Appointment> findByOccupationExternalIdAndEndDateIsNull(UUID occupationId);

    /**
     * Finds the strictly active appointment for a position based on the current date.
     *
     * <p>An appointment is active when its {@code startDate} is today or in the past
     * and its {@code endDate} is null or today or in the future. More precise than
     * {@link #findByOccupationExternalIdAndEndDateIsNull} for positions where
     * future-dated appointments may exist.</p>
     *
     * @param occupationId the public UUID of the target position.
     * @param today        the current date for the active window check.
     * @return an {@link Optional} containing the active appointment, or empty if vacant.
     */
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.occupation.externalId = :occupationId " +
            "AND a.startDate <= :today " +
            "AND (a.endDate IS NULL OR a.endDate >= :today)")
    Optional<Appointment> findActiveByOccupationExternalId(
            @Param("occupationId") UUID occupationId,
            @Param("today") LocalDate today);

    // -------------------------------------------------------------------------
    // Collection queries — by occupation
    // -------------------------------------------------------------------------

    /**
     * Retrieves the full chronological history of all persons who held a specific position,
     * ordered most-recent first.
     *
     * <p>Useful for the position dossier view — "who has held this role and when?"</p>
     *
     * @param occupationId the public UUID of the target position.
     * @return chronological history of appointments for the position, newest first.
     */
    List<Appointment> findByOccupationExternalIdOrderByStartDateDesc(UUID occupationId);

    // -------------------------------------------------------------------------
    // Collection queries — by person
    // -------------------------------------------------------------------------

    /**
     * Retrieves the full career dossier of a specific person — all positions they
     * have held, ordered most-recent first.
     *
     * <p>Useful for the person dossier view — "what positions has this person held?"</p>
     *
     * @param personId the public UUID of the target person.
     * @return chronological career history of the person, newest first.
     */
    List<Appointment> findByPersonExternalIdOrderByStartDateDesc(UUID personId);

    /**
     * Retrieves all currently active appointments for a specific person.
     *
     * <p>A person can theoretically hold multiple positions simultaneously
     * (e.g. Minister and Chairman of a board). This query surfaces all of them.</p>
     *
     * @param personId the public UUID of the target person.
     * @param today    the current date for the active window check.
     * @return list of currently active appointments for the person.
     */
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.person.externalId = :personId " +
            "AND a.startDate <= :today " +
            "AND (a.endDate IS NULL OR a.endDate >= :today)")
    List<Appointment> findActiveByPersonExternalId(
            @Param("personId") UUID personId,
            @Param("today") LocalDate today);

    /**
     * Retrieves all acting appointments currently in effect across the entire system.
     *
     * <p>Acting appointments signal power transitions and structural instability.
     * This query is used by the intelligence dashboard to surface all positions
     * currently held in an acting/temporary capacity.</p>
     *
     * @param today the current date for the active window check.
     * @return list of all active acting appointments system-wide.
     */
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.acting = true " +
            "AND a.startDate <= :today " +
            "AND (a.endDate IS NULL OR a.endDate >= :today)")
    List<Appointment> findAllActiveActingAppointments(@Param("today") LocalDate today);

    // =========================================================================
// PATCH: AppointmentRepository.java
// Add institution-scoped time-filtered query for org chart timeline.
// =========================================================================

    /**
     * Returns all appointments active at a given point in time for all
     * occupations within a specific institution.
     *
     * <p>An appointment is "active at date" when:
     * startDate <= atDate AND (endDate IS NULL OR endDate >= atDate)</p>
     *
     * @param institutionExternalId the public UUID of the institution.
     * @param atDate                the point in time to query.
     * @return all appointments active at that date across all positions
     *         in the institution.
     */
    @Query("SELECT a FROM Appointment a " +
            "JOIN FETCH a.occupation o " +
            "JOIN FETCH a.person p " +
            "WHERE o.institution.externalId = :institutionId " +
            "AND a.startDate <= :atDate " +
            "AND (a.endDate IS NULL OR a.endDate >= :atDate) " +
            "ORDER BY o.title ASC")
    List<Appointment> findByInstitutionAtDate(
            @Param("institutionId") UUID institutionId,
            @Param("atDate") LocalDate atDate);

    /**
     * Returns the full date range (min startDate, max endDate) of all
     * appointments within an institution — used to set the timeline scrubber
     * min/max bounds on the frontend.
     */
    @Query("SELECT MIN(a.startDate), MAX(a.endDate) FROM Appointment a " +
            "WHERE a.occupation.institution.externalId = :institutionId")
    Object[] findDateRangeByInstitution(@Param("institutionId") UUID institutionId);

    /**
     * Returns all appointments for an institution across all time —
     * used to populate the timeline scrubber tick marks (significant dates).
     */
    @Query("SELECT DISTINCT a.startDate FROM Appointment a " +
            "WHERE a.occupation.institution.externalId = :institutionId " +
            "ORDER BY a.startDate ASC")
    List<LocalDate> findSignificantDatesByInstitution(@Param("institutionId") UUID institutionId);

}