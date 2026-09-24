package com.projectfaust.institution;

import com.projectfaust.shared.enums.IdentifierType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link InstitutionIdentifier} entities.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface InstitutionIdentifierRepository extends JpaRepository<InstitutionIdentifier, UUID> {

    /**
     * Returns all active identifiers for a given institution.
     *
     * @param institutionId internal Long PK of the institution.
     * @return list of active identifiers.
     */
    List<InstitutionIdentifier> findAllByInstitutionIdAndActiveTrue(Long institutionId);

    /**
     * Finds a specific active identifier by institution and type.
     *
     * @param institutionId internal Long PK of the institution.
     * @param type          the identifier scheme to look up.
     * @return the matching identifier, if present.
     */
    Optional<InstitutionIdentifier> findByInstitutionIdAndTypeAndActiveTrue(
            Long institutionId, IdentifierType type);

    /**
     * Finds all institutions that hold a specific identifier value under a
     * given scheme — used for deduplication and cross-referencing
     * ("which institution in our registry has this LEI?").
     *
     * @param type  the identifier scheme.
     * @param value the identifier value to look up.
     * @return list of matching identifiers (usually 0 or 1).
     */
    List<InstitutionIdentifier> findAllByTypeAndValueAndActiveTrue(
            IdentifierType type, String value);

    /**
     * Returns all active national registration number identifiers for a
     * given institution in a specific country — the primary join key for
     * national procurement data sources.
     *
     * <p>For Czech Hlidač Státu data, call with {@code countryCode = "CZ"}
     * to retrieve IČO values. For Polish e-Zamówienia use {@code "PL"},
     * for French BOAMP use {@code "FR"} — no code change required when
     * adding support for new countries.</p>
     *
     * @param institutionId internal Long PK of the institution.
     * @param countryCode   ISO 3166-1 alpha-2 country code.
     * @return list of active national registration identifiers.
     */
    @Query("SELECT i FROM InstitutionIdentifier i " +
            "WHERE i.institution.id = :institutionId " +
            "AND i.type = com.projectfaust.shared.enums.IdentifierType.NATIONAL_REGISTRATION " +
            "AND i.countryCode = :countryCode " +
            "AND i.active = true")
    List<InstitutionIdentifier> findActiveNationalRegistrationsByInstitutionAndCountry(
            @Param("institutionId") Long institutionId,
            @Param("countryCode") String countryCode
    );
}