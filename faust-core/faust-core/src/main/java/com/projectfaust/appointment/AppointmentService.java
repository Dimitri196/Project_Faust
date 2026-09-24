package com.projectfaust.appointment;

import com.projectfaust.appointment.dto.AppointmentRequest;
import com.projectfaust.appointment.dto.AppointmentResponse;
import com.projectfaust.occupation.Occupation;
import com.projectfaust.occupation.OccupationRepository;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.shared.exception.ClearanceInsufficientException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service orchestrating the lifecycle of official appointments within Project Faust.
 *
 * <p>Maintains organisational hierarchy integrity by enforcing two invariants
 * on every appointment operation:</p>
 * <ul>
 *   <li><b>Security clearance validation</b> — a person's clearance must meet
 *       or exceed the position's {@code requiredClearanceLevel}.</li>
 *   <li><b>Vacancy state management</b> — a position is automatically marked
 *       as filled ({@code vacant = false}) when an ongoing appointment is created.</li>
 * </ul>
 *
 * <p><b>Bulk performance:</b> {@link #bulkAppoint} uses in-memory indexing to
 * pre-fetch all required persons and occupations in two batched queries,
 * reducing database round-trips from N to 2 regardless of batch size.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PersonRepository personRepository;
    private final OccupationRepository occupationRepository;
    private final AppointmentMapper appointmentMapper;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a single personnel appointment.
     *
     * <p>Validates that the person's security clearance meets the position's
     * requirements before establishing the assignment. The position's vacancy
     * status is updated if the appointment is ongoing (no end date).</p>
     *
     * @param request the appointment creation DTO.
     * @return the persisted appointment as an {@link AppointmentResponse}.
     * @throws EntityNotFoundException       if the person or position is not found.
     * @throws ClearanceInsufficientException if the person lacks sufficient clearance.
     */
    @Transactional
    public AppointmentResponse appointPerson(AppointmentRequest request) {
        log.info("FAUST_APPT: Initiating appointment for person {} to position {}.",
                request.personPublicId(), request.occupationPublicId());

        Occupation occupation = occupationRepository.findByExternalId(request.occupationPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "POSITION_NOT_FOUND: " + request.occupationPublicId()));
        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NOT_FOUND: " + request.personPublicId()));

        validateSecurityClearance(person, occupation);

        Appointment appointment = appointmentMapper.toEntity(request);
        appointment.setPerson(person);
        appointment.setOccupation(occupation);

        Appointment saved = appointmentRepository.save(appointment);
        updateOccupancyStatus(occupation, request.endDate());

        log.info("FAUST_APPT: Appointment confirmed — {} → '{}'.",
                person.getLastName(), occupation.getTitle());

        return appointmentMapper.toResponse(saved);
    }

    /**
     * Creates multiple appointments within a single database transaction.
     *
     * <p>Uses in-memory indexing to pre-fetch all required persons and occupations
     * in two batched queries, avoiding the N+1 problem for large batches.
     * Records that fail clearance validation are skipped and logged — the remaining
     * valid records are still committed.</p>
     *
     * @param requests list of appointment creation requests.
     */
    @Transactional
    public void bulkAppoint(List<AppointmentRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        log.info("FAUST_APPT_BULK: Processing {} appointment records.", requests.size());

        // Pre-fetch all required entities in 2 queries — avoids N+1
        Set<UUID> personUuids = requests.stream()
                .map(AppointmentRequest::personPublicId)
                .collect(Collectors.toSet());
        Set<UUID> occupationUuids = requests.stream()
                .map(AppointmentRequest::occupationPublicId)
                .collect(Collectors.toSet());

        Map<UUID, Person> personMap = personRepository.findAllByExternalIdIn(personUuids)
                .stream().collect(Collectors.toMap(Person::getExternalId, p -> p));
        Map<UUID, Occupation> occupationMap = occupationRepository.findAllByExternalIdIn(occupationUuids)
                .stream().collect(Collectors.toMap(Occupation::getExternalId, o -> o));

        List<Appointment> toSave = new ArrayList<>();
        Set<Occupation> toUpdate = new HashSet<>();

        for (AppointmentRequest req : requests) {
            Person person = personMap.get(req.personPublicId());
            Occupation occupation = occupationMap.get(req.occupationPublicId());

            if (person == null || occupation == null) {
                log.warn("FAUST_APPT_BULK: Skipping record — entity not found " +
                                "(person={}, occupation={}).",
                        req.personPublicId(), req.occupationPublicId());
                continue;
            }

            try {
                validateSecurityClearance(person, occupation);

                Appointment appt = appointmentMapper.toEntity(req);
                appt.setPerson(person);
                appt.setOccupation(occupation);
                toSave.add(appt);

                if (req.endDate() == null && occupation.isVacant()) {
                    occupation.setVacant(false);
                    toUpdate.add(occupation);
                }
            } catch (ClearanceInsufficientException e) {
                log.error("FAUST_APPT_BULK: Clearance violation — person {} / position {}: {}",
                        person.getFullName(), occupation.getTitle(), e.getMessage());
            }
        }

        appointmentRepository.saveAll(toSave);
        if (!toUpdate.isEmpty()) {
            occupationRepository.saveAll(toUpdate);
        }

        log.info("FAUST_APPT_BULK: Committed {} appointments.", toSave.size());
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Returns the chronological history of all persons who held a specific position.
     *
     * @param occupationId the public UUID of the target position.
     * @return chronological appointment history, newest first.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByOccupation(UUID occupationId) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByOccupationExternalIdOrderByStartDateDesc(occupationId));
    }

    /**
     * Returns the full career dossier of a specific person — all positions they have held.
     *
     * @param personId the public UUID of the target person.
     * @return chronological career history, newest first.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByPerson(UUID personId) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByPersonExternalIdOrderByStartDateDesc(personId));
    }

    /**
     * Returns all appointments currently active system-wide where the holder
     * is in an acting (temporary) capacity.
     *
     * <p>Acting appointments signal power transitions and structural instability.
     * Used by the intelligence dashboard to surface positions currently held
     * without a permanent appointment.</p>
     *
     * @return list of all active acting appointments.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getActiveActingAppointments() {
        log.info("FAUST_APPT: Querying all active acting appointments.");
        return appointmentMapper.toResponseList(
                appointmentRepository.findAllActiveActingAppointments(LocalDate.now()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that a person's security clearance meets the position's requirement.
     *
     * @param person     the person being assigned.
     * @param occupation the position being filled.
     * @throws ClearanceInsufficientException if the person's clearance weight is below
     *                                        the position's required clearance weight.
     */
    private void validateSecurityClearance(Person person, Occupation occupation) {
        if (person.getClearanceLevel().getWeight() < occupation.getRequiredClearanceLevel().getWeight()) {
            throw new ClearanceInsufficientException(String.format(
                    "CLEARANCE_INSUFFICIENT: Person [%s] clearance %s is below required %s for position [%s].",
                    person.getFullName(),
                    person.getClearanceLevel(),
                    occupation.getRequiredClearanceLevel(),
                    occupation.getTitle()
            ));
        }
    }

    /**
     * Marks a position as filled when an ongoing appointment is created.
     *
     * @param occupation the position being filled.
     * @param endDate    the appointment end date; null indicates an ongoing appointment.
     */
    private void updateOccupancyStatus(Occupation occupation, LocalDate endDate) {
        if (endDate == null && occupation.isVacant()) {
            occupation.setVacant(false);
            occupationRepository.save(occupation);
            log.info("FAUST_APPT: Position '{}' marked as filled.", occupation.getTitle());
        }
    }

    // =========================================================================
// PATCH: AppointmentService.java
// Add institution-scoped timeline query methods.
// =========================================================================

    /**
     * Returns all appointments active at a specific point in time for an institution.
     * Used by the org chart timeline component.
     *
     * @param institutionId the public UUID of the institution.
     * @param atDate        the point in time to query (defaults to today if null).
     * @return appointments active at that date.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getByInstitutionAtDate(UUID institutionId, LocalDate atDate) {
        LocalDate queryDate = atDate != null ? atDate : LocalDate.now();
        log.info("FAUST_APPT: Querying institution {} at date {}.", institutionId, queryDate);
        return appointmentMapper.toResponseList(
                appointmentRepository.findByInstitutionAtDate(institutionId, queryDate));
    }

    /**
     * Returns significant dates (all appointment start dates) for an institution.
     * Used to populate timeline scrubber tick marks.
     *
     * @param institutionId the public UUID of the institution.
     * @return sorted list of significant dates.
     */
    @Transactional(readOnly = true)
    public List<java.time.LocalDate> getSignificantDates(UUID institutionId) {
        return appointmentRepository.findSignificantDatesByInstitution(institutionId);
    }

    /**
     * Returns the date range (earliest appointment start, latest appointment end)
     * for an institution — used to set timeline scrubber bounds.
     *
     * @param institutionId the public UUID of the institution.
     * @return array of [minDate, maxDate], maxDate may be null if ongoing.
     */
    @Transactional(readOnly = true)
    public Object[] getDateRange(UUID institutionId) {
        return appointmentRepository.findDateRangeByInstitution(institutionId);
    }

}