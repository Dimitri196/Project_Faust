package com.projectfaust.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull UUID personPublicId,
        @NotNull UUID occupationPublicId,
        @NotNull LocalDate startDate,
        boolean isActing,
        String appointmentNote
) {}