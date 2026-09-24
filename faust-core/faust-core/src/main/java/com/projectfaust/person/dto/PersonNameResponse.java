package com.projectfaust.person.dto;

import com.projectfaust.shared.enums.NameType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single name record on a person — either the primary
 * legal name or an additional alias/cover/maiden name.
 *
 * @param publicId  public UUID of the name record itself.
 * @param firstName first name component.
 * @param lastName  last name component.
 * @param type      classification of the name (LEGAL, ALIAS, COVER, MAIDEN, etc.).
 * @param primary   true if this is the person's primary legal name.
 * @param validFrom date from which this name was in use; null if unknown.
 * @param validTo   date until which this name was in use; null if still active.
 * @param note      analyst note providing context for this name.
 *
 * @author Dimitri / Project Faust
 */
public record PersonNameResponse(
        UUID publicId,
        String firstName,
        String lastName,
        NameType type,
        boolean primary,
        LocalDate validFrom,
        LocalDate validTo,
        String note
) {}
