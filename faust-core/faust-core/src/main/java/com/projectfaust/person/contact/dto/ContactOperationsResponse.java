package com.projectfaust.person.contact.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.ContactType;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a single contact vector record.
 *
 * @param publicId               public UUID of the contact record.
 * @param personPublicId         public UUID of the subject person.
 * @param subjectDisplayName     formatted full name of the subject for UI context.
 * @param contactType            contact channel classification.
 * @param contactValueRaw        raw contact value as ingested.
 * @param contactValueNormalized normalised form for deduplication and search.
 * @param operatorName           telecom operator or service provider.
 * @param imei                   device IMEI for hardware fingerprinting.
 * @param verificationStatus     evidentiary provenance of this contact.
 * @param confidenceScore        analyst confidence score (0.0–1.0).
 * @param clearanceLevel         minimum clearance to view this contact.
 * @param active                 whether this contact is currently active.
 * @param validFrom              date from which this contact was valid.
 * @param validTo                date until which this contact was valid.
 * @param analyticalNote         free-text analyst context note.
 * @param createdAt              timestamp when this record was first persisted.
 * @author Dimitri / Project Faust
 */
public record ContactOperationsResponse(
        UUID publicId,
        UUID personPublicId,
        String subjectDisplayName,
        ContactType contactType,
        String contactValueRaw,
        String contactValueNormalized,
        String operatorName,
        String imei,
        VerificationStatus verificationStatus,
        Double confidenceScore,
        ClearanceLevel clearanceLevel,
        boolean active,
        LocalDate validFrom,
        LocalDate validTo,
        String analyticalNote,
        OffsetDateTime createdAt
) {}