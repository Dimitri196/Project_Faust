package com.projectfaust.vehicle.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import com.projectfaust.vehicle.FuelType;
import com.projectfaust.vehicle.VehicleCategory;
import com.projectfaust.vehicle.VehicleOwnerType;
import com.projectfaust.vehicle.VehicleSourceSystem;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Outbound representation of a {@link com.projectfaust.vehicle.VehicleRecord}.
 *
 * <p>Both the owner and operator are flattened into display-name + public-ID
 * pairs so consumers never need a second call to resolve them. The ownership
 * history is embedded as a list of {@link VehicleOwnershipHistoryDto} entries,
 * ordered by transfer date descending (most recent first).</p>
 *
 * @author Dimitri / Project Faust
 */
public record VehicleRecordResponseDto(

        // ── Identity ──────────────────────────────────────────────────────────

        UUID publicId,

        // ── Registration identifiers ──────────────────────────────────────────

        String vin,
        String licensePlate,
        String countryCode,
        String sourceReferenceId,

        // ── Vehicle description ───────────────────────────────────────────────

        VehicleCategory category,
        String make,
        String model,
        String variant,
        Integer modelYear,
        String color,
        FuelType fuelType,
        Integer engineDisplacementCc,
        Integer powerKw,
        Integer seatCount,
        Integer grossWeightKg,

        // ── Owner (resolved) ─────────────────────────────────────────────────

        VehicleOwnerType ownerType,

        /** Public UUID of the owning person; null when ownerType = INSTITUTION. */
        UUID ownerPersonPublicId,

        /** Public UUID of the owning institution; null when ownerType = PERSON. */
        UUID ownerInstitutionPublicId,

        /** Display name of the owner, resolved from person/institution or raw fallback. */
        String ownerDisplayName,

        String ownerNameRaw,
        String ownerNationalId,

        // ── Operator (resolved) ───────────────────────────────────────────────

        VehicleOwnerType operatorType,

        /** Public UUID of the operating person; null when operatorType = INSTITUTION or absent. */
        UUID operatorPersonPublicId,

        /** Public UUID of the operating institution; null when operatorType = PERSON or absent. */
        UUID operatorInstitutionPublicId,

        /** Display name of the operator, resolved or raw fallback. */
        String operatorDisplayName,

        String operatorNameRaw,
        String operatorNationalId,

        // ── Registration validity ─────────────────────────────────────────────

        LocalDate firstRegisteredOn,
        LocalDate registeredSince,
        LocalDate deregisteredOn,
        boolean active,

        // ── Technical inspection ──────────────────────────────────────────────

        LocalDate lastInspectionDate,
        LocalDate inspectionValidUntil,

        /** True if the technical inspection is still valid as of today. */
        boolean inspectionValid,

        // ── Financial profile ─────────────────────────────────────────────────

        BigDecimal acquisitionValue,
        String currency,
        boolean leased,
        String lessorName,
        boolean encumbered,

        // ── Insurance cross-reference ─────────────────────────────────────────

        /** Public UUID of the linked InsuranceRecord; null if not cross-referenced. */
        UUID insuranceRecordPublicId,

        // ── Ownership history ─────────────────────────────────────────────────

        List<VehicleOwnershipHistoryDto> ownershipHistory,

        // ── Source provenance ─────────────────────────────────────────────────

        VehicleSourceSystem sourceSystem,
        String registryUrl,

        // ── Quality & access control ──────────────────────────────────────────

        VerificationStatus verificationStatus,
        Double confidenceScore,
        ClearanceLevel clearanceLevel,
        String analyticalNote,

        // ── Audit ─────────────────────────────────────────────────────────────

        OffsetDateTime ingestedAt

) {

    // -------------------------------------------------------------------------
    // Nested: single ownership-transfer event
    // -------------------------------------------------------------------------

    /**
     * A single ownership-transfer event in a vehicle's history.
     */
    public record VehicleOwnershipHistoryDto(

            UUID publicId,
            LocalDate transferDate,
            String licensePlateAtTransfer,
            BigDecimal transferPrice,
            String currency,

            // From (previous owner) —
            VehicleOwnerType fromOwnerType,
            UUID fromPersonPublicId,
            UUID fromInstitutionPublicId,
            String fromDisplayName,
            String fromNameRaw,
            String fromNationalId,

            // To (new owner) —
            VehicleOwnerType toOwnerType,
            UUID toPersonPublicId,
            UUID toInstitutionPublicId,
            String toDisplayName,
            String toNameRaw,
            String toNationalId,

            String analyticalNote,
            OffsetDateTime ingestedAt

    ) {}
}
