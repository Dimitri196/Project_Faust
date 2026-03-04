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
 * Service managing the lifecycle of official appointments within Projekt Faust.
 * Optimized for high-volume data ingestion and political accumulation tracking.
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
     * Creates a single appointment.
     * Suitable for real-time UI actions.
     */
    @Transactional
    public void appointPerson(AppointmentRequest request) {
        log.info("System_Action: Initiating appointment for Person_ID: {} to Node_ID: {}",
                request.personPublicId(), request.occupationPublicId());

        Occupation occupation = occupationRepository.findByExternalId(request.occupationPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Occupation node not found"));
        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Subject not found"));

        validateSecurityClearance(person, occupation);

        Appointment appointment = mapToEntity(request, person, occupation);
        appointmentRepository.save(appointment);

        updateOccupationVacancyStatus(occupation, request.endDate());
        log.info("System_Action: Appointment confirmed for {} in {}", person.getLastName(), occupation.getTitle());
    }

    /**
     * HIGH-PERFORMANCE BULK INGESTION (Optimized for 6000+ records)
     * Uses in-memory caching to avoid N+1 select issues during mass imports.
     */
    @Transactional
    public void bulkAppoint(List<AppointmentRequest> requests) {
        if (requests == null || requests.isEmpty()) return;

        log.info("System_Action: Initiating optimized bulk import of {} records.", requests.size());

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
                log.warn("Skipping record: Missing Person ({}) or Occupation ({})",
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
                log.error("Security validation failed during bulk import for {}: {}", person.getFullName(), e.getMessage());
            }
        }

        appointmentRepository.saveAll(entitiesToSave);
        if (!occupationsToUpdate.isEmpty()) {
            occupationRepository.saveAll(occupationsToUpdate);
        }

        log.info("Bulk_Action: Successfully ingested {} appointments in one transaction.", entitiesToSave.size());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByOccupation(UUID occupationId) {
        log.debug("Accessing chronological logs for Occupation: {}", occupationId);
        return appointmentMapper.toResponseList(
                appointmentRepository.findByOccupationExternalIdOrderByStartDateDesc(occupationId)
        );
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByPerson(UUID personId) {
        log.debug("Accessing career logs for Person: {}", personId);
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

    private void validateSecurityClearance(Person person, Occupation occupation) {
        if (person.getClearanceLevel().getWeight() < occupation.getRequiredClearanceLevel().getWeight()) {
            throw new SecurityException(String.format(
                    "Insufficient clearance! Person: %s (%s) vs Required: %s",
                    person.getFullName(), person.getClearanceLevel(), occupation.getRequiredClearanceLevel()
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
