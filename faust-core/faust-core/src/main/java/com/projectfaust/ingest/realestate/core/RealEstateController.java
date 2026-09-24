package com.projectfaust.ingest.realestate.core;

import com.projectfaust.ingest.realestate.dto.RealEstateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for real estate ownership intelligence.
 *
 * <p>Read-only — all data enters via the Kafka ingest pipeline
 * ({@link RealEstateConsumerService}), not via REST POST.
 * Exposes ownership records scoped to persons and institutions
 * for dossier display and financial intelligence analysis.</p>
 *
 * <p>Base path: {@code /api/v1/realestate}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/realestate")
@RequiredArgsConstructor
@Tag(name = "Real Estate Intelligence",
        description = "Property ownership records linked to FAUST persons and institutions")
public class RealEstateController {

    private final RealEstateOwnershipRepository repository;
    private final RealEstateMapper mapper;

    // -------------------------------------------------------------------------
    // Person-scoped
    // -------------------------------------------------------------------------

    /**
     * Returns all real estate ownership records linked to the given person.
     * Used by the person dossier's FININT panel.
     *
     * @param personId the public UUID of the FAUST Person.
     * @return list of property ownership records, empty if none found.
     */
    @GetMapping("/person/{personId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get properties owned by a person",
            description = "Returns all cadaster-sourced ownership records linked " +
                    "to the given person. ANALYST access required.")
    public ResponseEntity<List<RealEstateResponse>> getByPerson(
            @PathVariable UUID personId) {
        List<RealEstateResponse> results = repository
                .findAllByPersonExternalId(personId)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * Returns the count of properties owned by a person — used for
     * the dossier badge without loading the full list.
     *
     * @param personId the public UUID of the FAUST Person.
     * @return ownership record count.
     */
    @GetMapping("/person/{personId}/count")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Count properties owned by a person")
    public ResponseEntity<Long> countByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(repository.countByPersonExternalId(personId));
    }

    /**
     * Returns only encumbered (mortgaged/liened) properties for a person —
     * key financial risk signal for FININT analysis.
     *
     * @param personId the public UUID of the FAUST Person.
     * @return list of encumbered properties.
     */
    @GetMapping("/person/{personId}/encumbered")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get encumbered properties for a person",
            description = "Returns properties with registered mortgages, liens, " +
                    "or easements — financial risk signal. ANALYST access required.")
    public ResponseEntity<List<RealEstateResponse>> getEncumberedByPerson(
            @PathVariable UUID personId) {
        List<RealEstateResponse> results = repository
                .findEncumberedByPersonExternalId(personId)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    // -------------------------------------------------------------------------
    // Institution-scoped
    // -------------------------------------------------------------------------

    /**
     * Returns all real estate ownership records linked to the given institution.
     * Used by the institution dossier's FININT panel.
     *
     * @param institutionId the public UUID of the FAUST Institution.
     * @return list of property ownership records.
     */
    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get properties owned by an institution",
            description = "Returns all cadaster-sourced ownership records linked " +
                    "to the given institution. ANALYST access required.")
    public ResponseEntity<List<RealEstateResponse>> getByInstitution(
            @PathVariable UUID institutionId) {
        List<RealEstateResponse> results = repository
                .findAllByInstitutionExternalId(institutionId)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * Returns the count of properties owned by an institution.
     *
     * @param institutionId the public UUID of the FAUST Institution.
     * @return ownership record count.
     */
    @GetMapping("/institution/{institutionId}/count")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Count properties owned by an institution")
    public ResponseEntity<Long> countByInstitution(@PathVariable UUID institutionId) {
        return ResponseEntity.ok(repository.countByInstitutionExternalId(institutionId));
    }

    // -------------------------------------------------------------------------
    // Geographic intelligence view
    // -------------------------------------------------------------------------

    /**
     * Returns all ownership records from a specific country — used for
     * cross-border property intelligence analysis.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g. "CZ", "SK").
     * @return list of ownership records from that country.
     */
    @GetMapping("/country/{countryCode}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Get all properties from a country",
            description = "Returns all ingested ownership records from the given " +
                    "country's cadaster. ANALYST access required.")
    public ResponseEntity<List<RealEstateResponse>> getByCountry(
            @PathVariable String countryCode) {
        List<RealEstateResponse> results = repository
                .findAllByCountryCode(countryCode.toUpperCase())
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(results);
    }

    // -------------------------------------------------------------------------
    // Trigger endpoint — fires fetch for a person when source is available
    // -------------------------------------------------------------------------

    /**
     * Triggers a real estate ownership lookup for the given person
     * from all configured cadaster sources.
     *
     * <p><b>STUB:</b> currently a no-op since no cadaster fetch services
     * are fully implemented yet. Will dispatch to configured
     * {@link com.projectfaust.ingest.realestate.cuzk.CuzkRealEstateFetchService}
     * (and future country services) once API access is established.</p>
     *
     * @param personId the public UUID of the target FAUST Person.
     * @return acknowledgement message.
     */
    @PostMapping("/person/{personId}/trigger")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Trigger property lookup for a person",
            description = "Fires cadaster lookup for the given person across all " +
                    "configured source countries. STUB — no-op until API access " +
                    "is configured. ANALYST access required.")
    public ResponseEntity<String> triggerForPerson(@PathVariable UUID personId) {
        return ResponseEntity.accepted()
                .body("Real estate lookup triggered for person " + personId +
                        " — cadaster API access not yet configured, no data fetched.");
    }
}