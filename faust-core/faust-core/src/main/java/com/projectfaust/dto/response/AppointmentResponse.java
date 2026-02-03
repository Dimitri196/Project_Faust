package com.projectfaust.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record AppointmentResponse(
        UUID publicId,
        String personDisplayName,
        UUID personPublicId,
        String occupationTitle,
        LocalDate startDate,
        LocalDate endDate,
        boolean isActing
) {}
