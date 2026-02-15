package com.projectfaust.repository;

import com.projectfaust.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    // Pro "Clean Swap" logiku (hledá aktivního držitele pozice)
    Optional<Appointment> findByOccupationExternalIdAndEndDateIsNull(UUID occupationId);

    // Pro historii konkrétní pozice (např. kdo všechno byl premiérem)
    List<Appointment> findByOccupationExternalIdOrderByStartDateDesc(UUID occupationId);

    // NOVÉ: Pro kariérní historii konkrétní osoby (např. co všechno dělal Babiš)
    // Spring se "zanoří" přes person -> externalId
    List<Appointment> findByPersonExternalIdOrderByStartDateDesc(UUID personId);
}