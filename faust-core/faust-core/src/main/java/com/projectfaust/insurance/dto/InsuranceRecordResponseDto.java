package com.projectfaust.insurance.dto;

import com.projectfaust.shared.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Outbound DTO representing an insurance policy record returned by the API.
 *
 * <p>Exposes all analytically relevant fields for the SPA's Insurance panel
 * and the FAUST intelligence pipeline. The subject is identified by
 * {@code personPublicId} and {@code personFullName} — the internal Person id
 * is never exposed.</p>
 *
 * @author Dimitri / Project Faust
 */
public record InsuranceRecordResponseDto(

        /** Public UUID of this insurance record. */
        UUID externalId,

        /** Public UUID of the linked person (policy holder / beneficiary). */
        UUID personPublicId,

        /** Resolved display name of the linked person. */
        String personFullName,

        /** Policy type classification. */
        InsuranceType insuranceType,

        /** Policy number as assigned by the insurer. */
        String policyNumber,

        /** Commercial name of the insurance product. */
        String productName,

        /** Legal name of the insurance company. */
        String insurerName,

        /** IČO or equivalent registration number of the insurer. */
        String insurerRegistrationNumber,

        /** Date from which the policy is in force. */
        LocalDate validFrom,

        /** Date on which the policy expires or was terminated. Null if still active. */
        LocalDate validTo,

        /** Whether the policy is currently active. */
        boolean active,

        /** Gross premium amount per billing cycle. */
        BigDecimal premiumAmount,

        /** ISO 4217 currency code. */
        String currency,

        /** Premium billing frequency. */
        PremiumFrequency premiumFrequency,

        /** Annual premium equivalent — computed by the mapper for financial analysis. */
        BigDecimal annualPremiumEquivalent,

        /** Total coverage / sum assured under the policy. */
        BigDecimal sumAssured,

        /** Named beneficiary as stated in the policy. */
        String beneficiaryName,

        /** National ID or registration number of the beneficiary. */
        String beneficiaryNationalId,

        /** Originating source system. */
        InsuranceSourceSystem sourceSystem,

        /** Reference ID within the source system. */
        String sourceReferenceId,

        /** Direct URL to the policy record in the source registry. */
        String registryUrl,

        /** Evidentiary verification state. */
        VerificationStatus verificationStatus,

        /** Confidence metric from 0.0 to 1.0. */
        Double confidenceScore,

        /** Minimum clearance level required to view this record. */
        ClearanceLevel clearanceLevel,

        /** Analyst notes. */
        String analyticalNote,

        /** Timestamp when the record was first ingested. */
        OffsetDateTime ingestedAt

) {}