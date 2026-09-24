package com.projectfaust.mobility.dto;

import com.projectfaust.mobility.*;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Outbound DTO for {@link com.projectfaust.mobility.MobilityEvent}.
 *
 * <p>Resolved fields:</p>
 * <ul>
 *   <li>{@code subjectPersonPublicId} / {@code subjectVehiclePublicId} — set if FK was resolved</li>
 *   <li>{@code violationOverdue} — {@code true} if unpaid fine past due date</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
public record MobilityEventResponseDto(

        UUID publicId,

        // ── Subjects ──────────────────────────────────────────────────────────

        UUID subjectPersonPublicId,
        UUID subjectVehiclePublicId,

        // ── Core event ────────────────────────────────────────────────────────

        MobilityEventType eventType,
        LocalDateTime eventTimestamp,

        // ── Location ──────────────────────────────────────────────────────────

        String locationName,
        String locationCountryCode,
        Double gpsLatitude,
        Double gpsLongitude,

        // ── Border / port cluster ─────────────────────────────────────────────

        MobilityDirection direction,
        String originCountryCode,
        String destinationCountryCode,
        String crossingPointName,
        String travelDocumentNumber,
        String travelDocumentType,

        // ── Violation cluster ─────────────────────────────────────────────────

        String violationCode,
        String violationDescription,
        MobilityViolationSeverity violationSeverity,
        BigDecimal fineAmount,
        String fineCurrency,
        Boolean finePaid,
        LocalDate fineDueDate,
        Integer pointsDeducted,
        String officerBadgeNumber,

        /**
         * {@code true} if fine is unpaid and {@code fineDueDate} is in the past.
         * Convenience flag for financial / enforcement views.
         */
        boolean violationOverdue,

        // ── Surveillance / ANPR cluster ───────────────────────────────────────

        String cameraId,
        String licensePlateRaw,
        BigDecimal ocrConfidence,

        // ── Source / provenance ───────────────────────────────────────────────

        MobilitySourceSystem sourceSystem,
        String sourceReferenceId,
        String sourceUrl,

        // ── Intelligence metadata ─────────────────────────────────────────────

        VerificationStatus verificationStatus,
        BigDecimal confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,
        LocalDateTime ingestedAt
) {}
