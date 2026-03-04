package com.projectfaust.dto.request;

import com.projectfaust.entity.Appointment;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public record AppointmentRequest(
        @NotNull UUID personPublicId,
        @NotNull UUID occupationPublicId,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlySalary,
        BigDecimal monthlyLumpSumAllowance,
        boolean isActing,
        Appointment.BenefitDetails benefitDetails,
        String appointmentNote,
        boolean isExOffoAccess
) {
    @JsonCreator
    public static AppointmentRequest create(
            @JsonProperty("personPublicId") UUID personPublicId,
            @JsonProperty("occupationPublicId") UUID occupationPublicId,
            @JsonProperty("startDate") LocalDate startDate,
            @JsonProperty("endDate") LocalDate endDate,
            @JsonProperty("monthlySalary") BigDecimal monthlySalary,
            @JsonProperty("monthlyLumpSumAllowance") BigDecimal monthlyLumpSumAllowance,
            @JsonProperty("isActing") boolean isActing,
            @JsonProperty("benefitDetails") Object benefitDetailsRaw,
            @JsonProperty("appointmentNote") String appointmentNote,
            @JsonProperty("isExOffoAccess") boolean isExOffoAccess
    ) {
        Appointment.BenefitDetails details = null;

        if (benefitDetailsRaw != null) {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            try {
                if (benefitDetailsRaw instanceof String str && !str.isBlank()) {
                       String sanitizedJson = str.replace("\"\"", "\"").trim();
                    if (sanitizedJson.startsWith("\"") && sanitizedJson.endsWith("\"")) {
                        sanitizedJson = sanitizedJson.substring(1, sanitizedJson.length() - 1);
                    }
                    details = mapper.readValue(sanitizedJson, Appointment.BenefitDetails.class);
                } else {

                    details = mapper.convertValue(benefitDetailsRaw, Appointment.BenefitDetails.class);
                }
            } catch (Exception e) {

                System.err.println("Chyba při parsování benefitDetails: " + e.getMessage());
            }
        }

        return new AppointmentRequest(
                personPublicId, occupationPublicId, startDate, endDate,
                monthlySalary, monthlyLumpSumAllowance, isActing,
                details, appointmentNote, isExOffoAccess
        );
    }
}
