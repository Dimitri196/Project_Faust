package com.projectfaust.financial.dto;

import com.projectfaust.shared.enums.PersonAccountRole;
import com.projectfaust.shared.enums.VerificationStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a {@code PersonAccountRelation}.
 *
 * <p>Includes denormalised account identifiers (IBAN, bankName) for
 * display without a second round-trip.</p>
 *
 * @param publicId           External UUID of the relation record itself
 * @param personPublicId     External UUID of the linked person
 * @param accountPublicId    External UUID of the bank account
 * @param iban               IBAN of the bank account (denormalised)
 * @param bankName           Bank name (denormalised)
 * @param roleType           Person's role on the account
 * @param validFrom          Start of the relationship
 * @param validTo            End of the relationship (null = still active)
 * @param active             Whether currently active
 * @param verificationStatus Evidentiary status
 * @param createdAt          Record creation timestamp
 * @param updatedAt          Record last-update timestamp
 */
public record PersonFinancialRelationResponse(
        UUID               publicId,
        UUID               personPublicId,
        UUID               accountPublicId,
        String             iban,
        String             bankName,
        PersonAccountRole  roleType,
        LocalDate          validFrom,
        LocalDate          validTo,
        boolean            active,
        VerificationStatus verificationStatus,
        OffsetDateTime     createdAt,
        OffsetDateTime     updatedAt
) {}
