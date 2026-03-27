package com.projectfaust.service;

import com.projectfaust.dto.request.AppointmentRequest;
import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.entity.Appointment;
import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.Person;
import com.projectfaust.mapper.AppointmentMapper;
import com.projectfaust.repository.AppointmentRepository;
import com.projectfaust.repository.OccupationRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Service orchestrating the lifecycle of official appointments and personnel deployments.
 * Maintains the integrity of the organizational hierarchy by enforcing security clearance
 * protocols and managing vacancy states across the Faust network.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PersonRepository personRepository;
    private final OccupationRepository occupationRepository;
    private final AppointmentMapper appointmentMapper;

    /**
     * Executes a single personnel deployment.
     * Validates that the subject's security clearance meets the role's requirements
     * before establishing the link.
     *
     * @param request Metadata for the new appointment.
     */
    @Transactional
    public void appointPerson(AppointmentRequest request) {
        log.info("System_Action: Initiating appointment for Subject: {} to Role: {}",
                request.personPublicId(), request.occupationPublicId());

        Occupation occupation = occupationRepository.findByExternalId(request.occupationPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Position node not found"));
        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Subject profile not found"));

        validateSecurityClearance(person, occupation);

        Appointment appointment = mapToEntity(request, person, occupation);
        appointmentRepository.save(appointment);

        updateOccupationVacancyStatus(occupation, request.endDate());
        log.info("System_Action: Deployment confirmed for {} into '{}'", person.getLastName(), occupation.getTitle());
    }

    /**
     * HIGH-PERFORMANCE BULK DEPLOYMENT
     * Optimized for mass ingestion (e.g., historical career data or post-election shifts).
     * Employs in-memory indexing to mitigate the N+1 selection problem, reducing database
     * round-trips from thousands to just a few batched queries.
     */
    @Transactional
    public void bulkAppoint(List<AppointmentRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        log.info("System_Action: Commencing optimized bulk ingestion of {} career records.", requests.size());

        Set<UUID> personUuids = requests.stream()
                .map(AppointmentRequest::personPublicId)
                .collect(Collectors.toSet());
        Set<UUID> occupationUuids = requests.stream()
                .map(AppointmentRequest::occupationPublicId)
                .collect(Collectors.toSet());

        Map<UUID, Person> personMap = personRepository.findAllByExternalIdIn(personUuids).stream()
                .collect(Collectors.toMap(Person::getExternalId, p -> p));
        Map<UUID, Occupation> occupationMap = occupationRepository.findAllByExternalIdIn(occupationUuids).stream()
                .collect(Collectors.toMap(Occupation::getExternalId, o -> o));

        List<Appointment> entitiesToSave = new ArrayList<>();
        Set<Occupation> occupationsToUpdate = new HashSet<>();

        for (AppointmentRequest req : requests) {
            Person person = personMap.get(req.personPublicId());
            Occupation occupation = occupationMap.get(req.occupationPublicId());

            if (person == null || occupation == null) {
                log.warn("Record Discarded: Identity resolution failed for Person ({}) or Role ({})",
                        req.personPublicId(), req.occupationPublicId());
                continue;
            }

            try {
                validateSecurityClearance(person, occupation);
                entitiesToSave.add(mapToEntity(req, person, occupation));

                if (req.endDate() == null && occupation.isVacant()) {
                    occupation.setVacant(false);
                    occupationsToUpdate.add(occupation);
                }
            } catch (SecurityException e) {
                log.error("Security Breach Blocked: Subject {} lacks clearance for Role {}: {}",
                        person.getFullName(), occupation.getTitle(), e.getMessage());
            }
        }

        appointmentRepository.saveAll(entitiesToSave);
        if (!occupationsToUpdate.isEmpty()) {
            occupationRepository.saveAll(occupationsToUpdate);
        }

        log.info("Bulk_Action: Successfully committed {} deployments in a single atomic transaction.", entitiesToSave.size());
    }

    /**
     * Retrieves the chronological history of all subjects who have held a specific position.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByOccupation(UUID occupationId) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByOccupationExternalIdOrderByStartDateDesc(occupationId)
        );
    }

    /**
     * Resolves the complete career trajectory (professional dossier) for a specific subject.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByPerson(UUID personId) {
        return appointmentMapper.toResponseList(
                appointmentRepository.findByPersonExternalIdOrderByStartDateDesc(personId)
        );
    }

    private Appointment mapToEntity(AppointmentRequest request, Person person, Occupation occupation) {
        return Appointment.builder()
                .person(person)
                .occupation(occupation)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlySalary(request.monthlySalary())
                .monthlyLumpSumAllowance(request.monthlyLumpSumAllowance())
                .benefitDetails(request.benefitDetails())
                .acting(request.isActing())
                .exOffoAccess(request.isExOffoAccess())
                .appointmentNote(request.appointmentNote() != null ?
                        request.appointmentNote() : "Standard systemic deployment")
                .build();
    }

    /**
     * Enforces the project's security model by comparing the subject's clearance level
     * against the position's required weight.
     */
    private void validateSecurityClearance(Person person, Occupation occupation) {
        if (person.getClearanceLevel().getWeight() < occupation.getRequiredClearanceLevel().getWeight()) {
            throw new SecurityException(String.format(
                    "Clearance Mismatch: Subject [%s] clearance is insufficient for Role [%s]",
                    person.getClearanceLevel(), occupation.getRequiredClearanceLevel()
            ));
        }
    }

    private void updateOccupationVacancyStatus(Occupation occupation, java.time.LocalDate endDate) {
        if (endDate == null && occupation.isVacant()) {
            occupation.setVacant(false);
            occupationRepository.save(occupation);
        }
    }
}
