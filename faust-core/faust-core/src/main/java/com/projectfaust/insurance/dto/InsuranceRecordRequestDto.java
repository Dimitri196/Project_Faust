package com.projectfaust.insurance.dto;

import com.projectfaust.shared.enums.InsuranceSourceSystem;
import com.projectfaust.shared.enums.InsuranceType;
import com.projectfaust.shared.enums.PremiumFrequency;
import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound DTO for creating or updating an {@link com.projectfaust.insurance.InsuranceRecord}.
 *
 * <p>Validation annotations enforce the minimum data quality required before
 * a record is persisted. The {@code personPublicId} is the only mandatory
 * link to the subject — all policy-detail fields are optional to accommodate
 * partial data from external registries.</p>
 *
 * @author Dimitri / Project Faust
 */
public record InsuranceRecordRequestDto(

        /**
         * Public UUID of the person who holds or is the beneficiary of this policy.
         * Must reference an existing Person record.
         */
        @NotNull(message = "personPublicId is required.")
        UUID personPublicId,

        /**
         * Policy type classification.
         */
        @NotNull(message = "insuranceType is required.")
        InsuranceType insuranceType,

        /**
         * Policy number as assigned by the insurer.
         */
        @Size(max = 100, message = "policyNumber must not exceed 100 characters.")
        String policyNumber,

        /**
         * Commercial product name (e.g. "Flexi Life Plus").
         */
        @Size(max = 200, message = "productName must not exceed 200 characters.")
        String productName,

        /**
         * Legal name of the insurance company.
         */
        @Size(max = 300, message = "insurerName must not exceed 300 characters.")
        String insurerName,

        /**
         * IČO or equivalent registration number of the insurer.
         */
        @Size(max = 30, message = "insurerRegistrationNumber must not exceed 30 characters.")
        String insurerRegistrationNumber,

        /**
         * Date from which the policy is in force.
         */
        LocalDate validFrom,

        /**
         * Date on which the policy expires or was terminated.
         */
        LocalDate validTo,

        /**
         * Whether the policy is currently active.
         */
        Boolean active,

        /**
         * Gross premium amount per billing cycle (before discounts/taxes).
         */
        @DecimalMin(value = "0.0", inclusive = true, message = "premiumAmount must be non-negative.")
        BigDecimal premiumAmount,

        /**
         * ISO 4217 currency code (e.g. "CZK", "EUR", "USD").
         */
        @Size(min = 3, max = 3, message = "currency must be a 3-character ISO 4217 code.")
        String currency,

        /**
         * Frequency at which the premium is billed.
         */
        PremiumFrequency premiumFrequency,

        /**
         * Total coverage / sum assured under the policy.
         */
        @DecimalMin(value = "0.0", inclusive = true, message = "sumAssured must be non-negative.")
        BigDecimal sumAssured,

        /**
         * Named beneficiary as stated in the policy.
         */
        @Size(max = 300, message = "beneficiaryName must not exceed 300 characters.")
        String beneficiaryName,

        /**
         * National ID or registration number of the beneficiary.
         */
        @Size(max = 50, message = "beneficiaryNationalId must not exceed 50 characters.")
        String beneficiaryNationalId,

        /**
         * Originating source system for deduplication.
         */
        @NotNull(message = "sourceSystem is required.")
        InsuranceSourceSystem sourceSystem,

        /**
         * The record's unique reference ID within the source system.
         */
        @Size(max = 200, message = "sourceReferenceId must not exceed 200 characters.")
        String sourceReferenceId,

        /**
         * Direct URL to the policy record in the source registry.
         */
        @Size(max = 500, message = "registryUrl must not exceed 500 characters.")
        String registryUrl,

        /**
         * Raw payload as received from the source — preserved for audit.
         */
        String rawData,

        /**
         * Evidentiary verification state of this record.
         */
        VerificationStatus verificationStatus,

        /**
         * Confidence metric from 0.0 (speculative) to 1.0 (confirmed).
         */
        @DecimalMin(value = "0.0", message = "confidenceScore must be between 0.0 and 1.0.")
        @DecimalMax(value = "1.0", message = "confidenceScore must be between 0.0 and 1.0.")
        Double confidenceScore,

        /**
         * Minimum clearance level required to view this record.
         */
        ClearanceLevel clearanceLevel,

        /**
         * Analyst notes — context, caveats, secondary leads.
         */
        String analyticalNote

) {}