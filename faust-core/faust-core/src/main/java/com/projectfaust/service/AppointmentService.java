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

    /**
     * Appoints a person to a position.
     * If the position is already occupied, the current appointment is automatically ended.
     */
    @Transactional
    public void appointPerson(AppointmentRequest request) {
        log.info("Appointing person {} to occupation {}", request.personPublicId(), request.occupationPublicId());

        // 1. Resolve Occupation
        Occupation occupation = occupationRepository.findByExternalId(request.occupationPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Occupation not found"));

        // 2. "Clean Swap" - Find and close the currently active appointment if it exists
        appointmentRepository.findByOccupationExternalIdAndEndDateIsNull(request.occupationPublicId())
                .ifPresent(existing -> {
                    log.info("Closing existing appointment for person {}", existing.getPerson().getLastName());
                    // End previous appointment the day before the new one starts
                    existing.setEndDate(request.startDate().minusDays(1));
                    appointmentRepository.save(existing);
                });

        // 3. Resolve Person
        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        // 4. Create new Appointment
        Appointment appointment = Appointment.builder()
                .person(person)
                .occupation(occupation)
                .startDate(request.startDate())
                .acting(request.isActing())
                .appointmentNote(request.appointmentNote())
                .build();

        appointmentRepository.save(appointment);

        // 5. Update Occupation status to NOT vacant
        if (occupation.isVacant()) {
            occupation.setVacant(false);
            occupationRepository.save(occupation);
        }
    }

    /**
     * Retrieves the history of a specific chair.
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getHistoryByOccupation(UUID occupationId) {
        List<Appointment> history = appointmentRepository.findByOccupationExternalIdOrderByStartDateDesc(occupationId);
        return appointmentMapper.toResponseList(history);
    }
}
