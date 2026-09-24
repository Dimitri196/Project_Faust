package com.projectfaust.appointment.dto;

import com.projectfaust.appointment.Appointment;
import com.projectfaust.shared.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO representing a single appointment record in Project Faust.
 *
 * <p>Provides a complete view of a person-position assignment including
 * financial terms, acting status, ex-officio access, and structured benefit
 * data. Person and occupation are flattened to scalar fields to prevent
 * nested graph serialisation.</p>
 *
 * <p>{@code personPhotoUrl} is included so the frontend can render a person
 * card inline within appointment timeline views without a second API call.</p>
 *
 * @param publicId                 public UUID of this appointment record.
 * @param personDisplayName        display name of the assigned person.
 * @param personPublicId           public UUID of the assigned person.
 * @param personPhotoUrl           URL of the person's photo for card rendering.
 * @param occupationTitle          title of the position held.
 * @param occupationPublicId       public UUID of the position.
 * @param startDate                the date the assignment began.
 * @param endDate                  the date the assignment ended; null if ongoing.
 * @param monthlySalary            base monthly compensation before taxes.
 * @param monthlyLumpSumAllowance  fixed monthly allowances.
 * @param currency                 ISO 4217 currency code for financial values.
 * @param acting                   whether the person holds the office in an acting capacity.
 * @param exOffoAccess             whether the position grants ex-officio access.
 * @param benefitDetails           structured perks and privileges for this appointment.
 * @param appointmentNote          analyst note providing intelligence context.
 * @param verificationStatus       evidentiary provenance state of this record.
 * @param createdAt                timestamp when this record was first persisted.
 * @param updatedAt                timestamp of the most recent modification.
 * @author Dimitri / Project Faust
 */
public record AppointmentResponse(
        UUID publicId,
        String personDisplayName,
        UUID personPublicId,
        String personPhotoUrl,
        String occupationTitle,
        UUID occupationPublicId,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlySalary,
        BigDecimal monthlyLumpSumAllowance,
        String currency,
        boolean acting,
        boolean exOffoAccess,
        Appointment.BenefitDetails benefitDetails,
        String appointmentNote,
        VerificationStatus verificationStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}