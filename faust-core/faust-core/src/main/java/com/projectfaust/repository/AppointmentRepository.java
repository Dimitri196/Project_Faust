package com.projectfaust.repository;

import com.projectfaust.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Used for the "Clean Swap" logic
    Optional<Appointment> findByOccupationExternalIdAndEndDateIsNull(UUID occupationId);

    // Used for the history view
    List<Appointment> findByOccupationExternalIdOrderByStartDateDesc(UUID occupationId);
}
