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

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PersonRepository personRepository;
    private final OccupationRepository occupationRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public void appointPerson(AppointmentRequest request) {
        log.info("System_Action: Initiating appointment for Person_ID: {} to Node_ID: {}",
                request.personPublicId(), request.occupationPublicId());

        Occupation occupation = occupationRepository.findByExternalId(request.occupationPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Occupation node not found"));
        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Subject not found"));

        if (request.endDate() == null) {
            appointmentRepository.findByOccupationExternalIdAndEndDateIsNull(request.occupationPublicId())
                    .ifPresent(active -> {
                        if (!request.startDate().isAfter(active.getStartDate())) {
                            throw new IllegalStateException("Chronological_Error: New active appointment must start after the current one began.");
                        }
                        log.info("Node_Maintenance: Closing active appointment for: {}", active.getPerson().getLastName());
                        active.setEndDate(request.startDate().minusDays(1));
                        appointmentRepository.save(active);
                    });
        } else {
            log.info("Archive_Entry: Registering historical record for period {} - {}",
                    request.startDate(), request.endDate());
        }

        Appointment appointment = Appointment.builder()
                .person(person)
                .occupation(occupation)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .monthlySalary(request.monthlySalary())
                .monthlyLumpSumAllowance(request.monthlyLumpSumAllowance())
                .benefitDetails(request.benefitDetails())
                .acting(request.isActing())
                .appointmentNote(request.appointmentNote() != null ?
                        request.appointmentNote() : "Standard systemic deployment")
                .build();

        appointmentRepository.save(appointment);

        if (request.endDate() == null && occupation.isVacant()) {
            occupation.setVacant(false);
            occupationRepository.save(occupation);
        }
    }

    /**
     * History of a specific chair (What people were in this office?)
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByOccupation(UUID occupationId) {
        log.debug("Accessing chronological logs for Occupation: {}", occupationId);
        List<Appointment> history = appointmentRepository.findByOccupationExternalIdOrderByStartDateDesc(occupationId);
        return appointmentMapper.toResponseList(history);
    }

    /**
     * Career history of a specific person (What offices did this person hold?)
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByPerson(UUID personId) {
        log.debug("Accessing career logs for Person: {}", personId);
        List<Appointment> history = appointmentRepository.findByPersonExternalIdOrderByStartDateDesc(personId);
        return appointmentMapper.toResponseList(history);
    }
}
