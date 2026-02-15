package com.projectfaust.dto.response;

import com.projectfaust.entity.Appointment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AppointmentResponse(
        UUID publicId,
        String personDisplayName,
        UUID personPublicId,
        String occupationTitle,
        String occupationPublicId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlySalary,
        BigDecimal monthlyLumpSumAllowance,
        String currency,
        boolean isActing,
        Appointment.BenefitDetails benefitDetails,
        String appointmentNote,
        String personPhotoUrl
) {}
