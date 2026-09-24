package com.projectfaust.mobility.dto;

import com.projectfaust.mobility.*;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating a {@link com.projectfaust.mobility.MobilityEvent}.
 *
 * <p><b>Subject model:</b> {@code subjectPersonPublicId} and
 * {@code subjectVehiclePublicId} are independently optional — both, either,
 * or neither may be supplied. At least one is expected for meaningful intelligence,
 * but the record is accepted without either (e.g. raw ANPR read pre-linking).</p>
 *
 * <p><b>Cluster conventions:</b><br>
 * Border / port events → populate {@code direction}, {@code originCountryCode},
 * {@code destinationCountryCode}, {@code crossingPointName}, {@code travelDocumentNumber}.<br>
 * Violation events → populate {@code violationCode}, {@code violationDescription},
 * {@code violationSeverity}, {@code fineAmount}, {@code finePaid}.<br>
 * Surveillance events → populate {@code cameraId}, {@code licensePlateRaw},
 * {@code ocrConfidence}.</p>
 *
 * @author Dimitri / Project Faust
 */
public record MobilityEventRequestDto(

        // ── Subjects ──────────────────────────────────────────────────────────

        UUID subjectPersonPublicId,
        UUID subjectVehiclePublicId,

        // ── Core event ────────────────────────────────────────────────────────

        @NotNull
        MobilityEventType eventType,

        @NotNull
        LocalDateTime eventTimestamp,

        // ── Location ──────────────────────────────────────────────────────────

        @Size(max = 300)
        String locationName,

        @Size(min = 2, max = 2)
        String locationCountryCode,

        Double gpsLatitude,
        Double gpsLongitude,

        // ── Border / port cluster ─────────────────────────────────────────────

        MobilityDirection direction,

        @Size(min = 2, max = 2)
        String originCountryCode,

        @Size(min = 2, max = 2)
        String destinationCountryCode,

        @Size(max = 200)
        String crossingPointName,

        @Size(max = 50)
        String travelDocumentNumber,

        @Size(max = 30)
        String travelDocumentType,

        // ── Violation cluster ─────────────────────────────────────────────────

        @Size(max = 50)
        String violationCode,

        String violationDescription,

        MobilityViolationSeverity violationSeverity,

        @DecimalMin("0.00")
        BigDecimal fineAmount,

        @Size(min = 3, max = 3)
        String fineCurrency,

        Boolean finePaid,

        LocalDate fineDueDate,

        @Min(0)
        Integer pointsDeducted,

        @Size(max = 50)
        String officerBadgeNumber,

        // ── Surveillance / ANPR cluster ───────────────────────────────────────

        @Size(max = 100)
        String cameraId,

        @Size(max = 20)
        String licensePlateRaw,

        @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal ocrConfidence,

        // ── Source / provenance ───────────────────────────────────────────────

        @NotNull
        MobilitySourceSystem sourceSystem,

        @Size(max = 200)
        String sourceReferenceId,

        @Size(max = 2000)
        String sourceUrl,

        // ── Intelligence metadata ─────────────────────────────────────────────

        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0")
        BigDecimal confidenceScore,

        ClearanceLevel clearanceLevel,

        String analyticalNote
) {}
