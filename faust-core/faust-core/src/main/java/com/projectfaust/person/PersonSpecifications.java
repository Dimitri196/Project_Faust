package com.projectfaust.person;

import com.projectfaust.shared.enums.ClearanceLevel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds a composable JPA {@link Specification} from a {@link PersonFilter}.
 *
 * <p>Mirrors {@code LocationSpecifications} — only non-null filter fields
 * contribute a predicate; all present predicates are combined with AND.</p>
 *
 * @author Dimitri / Project Faust
 */
public final class PersonSpecifications {

    private PersonSpecifications() {
        // Utility class — no instances.
    }

    /**
     * Builds a {@link Specification} reflecting only the non-null fields of
     * the given filter.
     *
     * <p>The free-text {@code query} matches against
     * {@code fullNameSearchNormalized} using a case-insensitive substring
     * match — same DB-generated normalised column used by
     * {@link PersonRepository#searchByFullName(String)}, so diacritic
     * handling is consistent between this endpoint and the existing
     * {@code /search} endpoint.</p>
     *
     * <p>{@code minClearanceLevel} resolves all {@link ClearanceLevel} constants
     * whose {@code weight} is greater than or equal to the filter's weight,
     * then filters using an {@code IN(...)} predicate against that resolved
     * set — rather than comparing raw ordinals, which would silently break
     * if the enum were ever reordered.</p>
     *
     * @param filter the filter criteria (nullable fields are ignored).
     * @return a specification combining all present filter predicates with AND.
     */
    public static Specification<Person> build(PersonFilter filter) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.query() != null && !filter.query().isBlank()) {
                String likePattern = "%" + filter.query().toLowerCase() + "%";
                predicates.add(cb.like(
                        cb.lower(root.get("fullNameSearchNormalized")), likePattern));
            }

            if (filter.clearanceLevel() != null) {
                predicates.add(cb.equal(root.get("clearanceLevel"), filter.clearanceLevel()));
            }

            if (filter.minClearanceLevel() != null) {
                // ClearanceLevel.weight (1-5) is the correct, explicit
                // comparison value -- NOT enum ordinal, which is a fragile
                // implementation detail. Criteria API predicates can't call
                // getWeight() directly inside the predicate tree, so the
                // qualifying enum constants are resolved here in plain Java
                // and turned into an IN(...) predicate instead.
                List<ClearanceLevel> qualifying = Arrays.stream(ClearanceLevel.values())
                        .filter(level -> level.getWeight() >= filter.minClearanceLevel().getWeight())
                        .toList();
                predicates.add(root.get("clearanceLevel").in(qualifying));
            }

            if (filter.educationLevel() != null) {
                predicates.add(cb.equal(root.get("educationLevel"), filter.educationLevel()));
            }

            if (filter.nationality() != null && !filter.nationality().isBlank()) {
                predicates.add(cb.equal(root.get("nationality"), filter.nationality()));
            }

            if (filter.verificationStatus() != null) {
                predicates.add(cb.equal(root.get("verificationStatus"), filter.verificationStatus()));
            }

            if (filter.placeOfBirth() != null && !filter.placeOfBirth().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("placeOfBirth")),
                        "%" + filter.placeOfBirth().toLowerCase() + "%"));
            }

            if (filter.gender() != null) {
                predicates.add(cb.equal(root.get("gender"), filter.gender()));
            }

            if (filter.politicalAffiliation() != null && !filter.politicalAffiliation().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("politicalAffiliation")),
                        "%" + filter.politicalAffiliation().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}