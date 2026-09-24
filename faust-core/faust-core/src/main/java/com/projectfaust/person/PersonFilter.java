package com.projectfaust.person;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.EducationLevel;
import com.projectfaust.shared.enums.Gender;
import com.projectfaust.shared.enums.VerificationStatus;

/**
 * Filter criteria for paginated, multi-dimensional person search.
 *
 * <p>Mirrors the {@code LocationFilter} pattern used by the Location module —
 * all fields are optional; omitting a field means "no restriction on that
 * dimension". Composed into a JPA {@link org.springframework.data.jpa.domain.Specification}
 * by {@link PersonSpecifications#build(PersonFilter)}.</p>
 *
 * <p>Bound from query parameters by Spring MVC (matching field names to
 * request params automatically — no {@code @ModelAttribute} needed when
 * used as a plain controller method parameter, same as {@code LocationFilter}).</p>
 *
 * <p><b>Name filtering note:</b> deliberately does NOT expose discrete
 * {@code firstName}/{@code lastName} filters. Those fields are deprecated
 * legacy columns on {@link Person} — actual name data lives in the
 * {@code PersonName} history collection. The {@code query} field already
 * searches the DB-generated {@code fullNameSearchNormalized} column, which
 * aggregates current and historical names correctly; filtering against the
 * deprecated scalar fields directly would bypass that and return stale or
 * incomplete matches for persons with name changes or aliases.</p>
 *
 * @param query              free-text search against the normalised full-name column.
 * @param clearanceLevel     restrict to persons at exactly this clearance level.
 * @param minClearanceLevel  restrict to persons at or above this clearance level (by weight).
 * @param educationLevel     restrict to persons with this education level.
 * @param nationality        restrict to persons with this nationality (exact match).
 * @param verificationStatus restrict to persons with this verification/provenance status.
 * @param placeOfBirth       restrict to persons with this place of birth (case-insensitive substring match).
 * @param gender              restrict to persons with this gender.
 * @param politicalAffiliation restrict to persons with this political affiliation (case-insensitive substring match).
 * @author Dimitri / Project Faust
 */
public record PersonFilter(
        String query,
        ClearanceLevel clearanceLevel,
        ClearanceLevel minClearanceLevel,
        EducationLevel educationLevel,
        String nationality,
        VerificationStatus verificationStatus,
        String placeOfBirth,
        Gender gender,
        String politicalAffiliation
) {}