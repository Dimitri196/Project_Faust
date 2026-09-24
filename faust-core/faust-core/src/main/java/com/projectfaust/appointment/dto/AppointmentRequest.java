package com.projectfaust.appointment.dto;

import com.projectfaust.appointment.Appointment;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating or updating an {@link Appointment} record.
 *
 * <p>All mandatory fields carry Bean Validation constraints. The controller
 * must annotate the parameter with {@code @Valid} for constraints to be enforced.</p>
 *
 * <p><b>benefitDetails serialisation:</b> the {@link Appointment.BenefitDetails}
 * field is a typed nested object. Spring Boot's Jackson autoconfiguration handles
 * deserialisation automatically when the frontend sends a correctly structured
 * JSON object. Example:</p>
 * <pre>{@code
 * {
 *   "benefitDetails": {
 *     "housingType": "STATE_RESIDENCE",
 *     "diplomaticPassport": true,
 *     "securityDetail": false,
 *     "officialCarWithDriver": true,
 *     "travelBudget": 50000.00
 *   }
 * }
 * }</pre>
 *
 * @param personPublicId           public UUID of the person being assigned (required).
 * @param occupationPublicId       public UUID of the position being filled (required).
 * @param startDate                the date the assignment begins (required).
 * @param endDate                  the date the assignment ends; null for ongoing.
 * @param monthlySalary            base monthly compensation before taxes.
 * @param monthlyLumpSumAllowance  fixed monthly allowances.
 * @param acting                   whether the person is in an acting/temporary capacity.
 * @param exOffoAccess             whether the position grants ex-officio access.
 * @param benefitDetails           structured perks and privileges for this appointment.
 * @param appointmentNote          analyst note providing intelligence context (max 2000).
 * @param verificationStatus       provenance state; defaults to PENDING_REVIEW in service.
 * @author Dimitri / Project Faust
 */
public record AppointmentRequest(

        @NotNull(message = "Person public ID is required.")
        UUID personPublicId,

        @NotNull(message = "Occupation public ID is required.")
        UUID occupationPublicId,

        @NotNull(message = "Start date is required.")
        LocalDate startDate,

        LocalDate endDate,

        BigDecimal monthlySalary,
        BigDecimal monthlyLumpSumAllowance,

        boolean acting,
        boolean exOffoAccess,

        Appointment.BenefitDetails benefitDetails,

        @Size(max = 2000, message = "Appointment note must not exceed 2000 characters.")
        String appointmentNote,

        VerificationStatus verificationStatus

) {}