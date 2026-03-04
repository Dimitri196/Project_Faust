package com.projectfaust.repository;

import com.projectfaust.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing person-to-occupation assignments (Appointments).
 * Provides methods for tracking career progression and monitoring current office holders.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Finds the currently active appointment for a specific occupation.
     * An appointment is considered active if it has no end date defined.
     *
     * @param occupationId The unique identifier of the target occupation.
     * @return An Optional containing the current incumbent, or empty if the position is vacant.
     */
    Optional<Appointment> findByOccupationExternalIdAndEndDateIsNull(UUID occupationId);

    /**
     * Retrieves the chronological history of all subjects who held a specific occupation.
     * Results are ordered from the most recent to the oldest.
     *
     * @param occupationId The unique identifier of the target occupation node.
     * @return A list of historical and current appointments for the occupation.
     */
    List<Appointment> findByOccupationExternalIdOrderByStartDateDesc(UUID occupationId);

    /**
     * Retrieves the full career dossier of a specific person.
     * Lists all appointments the subject has held, ordered chronologically by the start date.
     *
     * @param personId The unique identifier of the subject (Person).
     * @return A list of appointments representing the person's professional history.
     */
    List<Appointment> findByPersonExternalIdOrderByStartDateDesc(UUID personId);
}
