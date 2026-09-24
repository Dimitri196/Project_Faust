package com.projectfaust.vehicle.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.vehicle.FuelType;
import com.projectfaust.vehicle.VehicleCategory;
import com.projectfaust.vehicle.VehicleOwnerType;
import com.projectfaust.vehicle.VehicleSourceSystem;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating a {@link com.projectfaust.vehicle.VehicleRecord}.
 *
 * <p>Owner and operator are each expressed as a {@link VehicleOwnerType}
 * discriminator plus exactly one of a person UUID or institution UUID.
 * The service layer validates that the correct FK is supplied for the
 * declared type.</p>
 *
 * @author Dimitri / Project Faust
 */
public record VehicleRecordRequestDto(

        // ── Registration identifiers ──────────────────────────────────────────

        @Size(min = 17, max = 17, message = "VIN must be exactly 17 characters.")
        String vin,

        @Size(max = 20, message = "licensePlate must not exceed 20 characters.")
        String licensePlate,

        @NotBlank(message = "countryCode is required.")
        @Size(min = 2, max = 2, message = "countryCode must be a 2-character ISO 3166-1 code.")
        String countryCode,

        @Size(max = 200, message = "sourceReferenceId must not exceed 200 characters.")
        String sourceReferenceId,

        // ── Vehicle description ───────────────────────────────────────────────

        @NotNull(message = "category is required.")
        VehicleCategory category,

        @Size(max = 100) String make,
        @Size(max = 100) String model,
        @Size(max = 100) String variant,

        Integer modelYear,

        @Size(max = 50) String color,

        FuelType fuelType,

        @Min(value = 0, message = "engineDisplacementCc must be non-negative.")
        Integer engineDisplacementCc,

        @Min(value = 0, message = "powerKw must be non-negative.")
        Integer powerKw,

        @Min(value = 0, message = "seatCount must be non-negative.")
        Integer seatCount,

        @Min(value = 0, message = "grossWeightKg must be non-negative.")
        Integer grossWeightKg,

        // ── Owner ─────────────────────────────────────────────────────────────

        @NotNull(message = "ownerType is required.")
        VehicleOwnerType ownerType,

        /** Set when ownerType = PERSON. */
        UUID ownerPersonPublicId,

        /** Set when ownerType = INSTITUTION. */
        UUID ownerInstitutionPublicId,

        @Size(max = 300) String ownerNameRaw,
        @Size(max = 50)  String ownerNationalId,

        // ── Operator ──────────────────────────────────────────────────────────

        VehicleOwnerType operatorType,

        /** Set when operatorType = PERSON. */
        UUID operatorPersonPublicId,

        /** Set when operatorType = INSTITUTION. */
        UUID operatorInstitutionPublicId,

        @Size(max = 300) String operatorNameRaw,
        @Size(max = 50)  String operatorNationalId,

        // ── Registration validity ─────────────────────────────────────────────

        LocalDate firstRegisteredOn,
        LocalDate registeredSince,
        LocalDate deregisteredOn,
        Boolean active,

        // ── Technical inspection ──────────────────────────────────────────────

        LocalDate lastInspectionDate,
        LocalDate inspectionValidUntil,

        // ── Financial profile ─────────────────────────────────────────────────

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal acquisitionValue,

        @Size(min = 3, max = 3, message = "currency must be a 3-character ISO 4217 code.")
        String currency,

        Boolean leased,

        @Size(max = 300) String lessorName,

        Boolean encumbered,

        /** Optional link to the compulsory liability InsuranceRecord. */
        UUID insuranceRecordPublicId,

        // ── Source provenance ─────────────────────────────────────────────────

        @NotNull(message = "sourceSystem is required.")
        VehicleSourceSystem sourceSystem,

        @Size(max = 500) String registryUrl,

        String rawData,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,

        @DecimalMin("0.0") @DecimalMax("1.0")
        Double confidenceScore,

        ClearanceLevel clearanceLevel,

        String analyticalNote

) {}
