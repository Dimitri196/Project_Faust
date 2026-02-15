package com.projectfaust.dto.request;

import com.projectfaust.entity.Appointment;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AppointmentRequest(
        @NotNull UUID personPublicId,
        @NotNull UUID occupationPublicId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlySalary,
        BigDecimal monthlyLumpSumAllowance,
        boolean isActing,
        Appointment.BenefitDetails benefitDetails,
        String appointmentNote
) {}
