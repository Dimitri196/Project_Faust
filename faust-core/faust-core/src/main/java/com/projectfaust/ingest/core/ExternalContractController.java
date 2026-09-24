package com.projectfaust.ingest.core;

import com.projectfaust.ingest.dto.ExternalContractResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for querying public contract records linked to institutions.
 *
 * <p>Base path: {@code /api/v1/contracts}</p>
 *
 * <p>Contracts are linked to institutions at query time via the institution's
 * {@link com.projectfaust.institution.InstitutionIdentifier} collection —
 * no FK exists between the two tables. Current source: Hlidač Státu (CZ),
 * matched via {@code NATIONAL_REGISTRATION} identifiers with
 * {@code countryCode = "CZ"}.</p>
 *
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>{@code GET /institution/{id}} — contracts for a specific institution,
 *       with optional supplier search filter.</li>
 *   <li>{@code GET /supplier/search} — cross-institution supplier intelligence:
 *       all contracts where a given supplier appears regardless of buyer.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Tag(name = "Public Contracts",
        description = "External procurement contract records linked to institutions")
public class ExternalContractController {

    private final ExternalContractService contractService;

    /**
     * Returns paginated public contracts linked to an institution.
     *
     * <p>When {@code supplierName} is provided, results are filtered server-side
     * to contracts where the supplier name, registration number, or tax ID
     * matches the search term. This replaces the previous role filter (BUYER /
     * SUPPLIER) which was misleading — most government institutions only appear
     * as buyers, so SUPPLIER always returned empty results.</p>
     *
     * @param institutionPublicId public UUID of the institution.
     * @param supplierName        optional — search supplier name, registration
     *                            number, or tax ID within this institution's contracts.
     * @param fetchIfEmpty        if true, triggers a fresh Hlidač Státu fetch when
     *                            no contracts exist yet. Use only on explicit analyst
     *                            action (e.g. the "Refresh" button) — not on every
     *                            page load.
     * @param pageable            pagination (default: 20 per page, newest first).
     * @return page of matching contract records with computed role field.
     */
    @GetMapping("/institution/{institutionPublicId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(
            summary = "Get contracts by institution",
            description = "Returns paginated procurement contracts for an institution. " +
                    "Optional ?supplierName= filters by supplier name, registration number, " +
                    "or tax ID. Returns empty page if no applicable identifier is registered."
    )
    public ResponseEntity<Page<ExternalContractResponse>> getByInstitution(
            @PathVariable UUID institutionPublicId,
            @Parameter(description = "Search supplier name, registration number, or tax ID")
            @RequestParam(required = false) String supplierName,
            @Parameter(description = "Trigger fresh fetch from Hlidač Státu if no " +
                    "contracts exist yet. Use sparingly.")
            @RequestParam(defaultValue = "false") boolean fetchIfEmpty,
            @PageableDefault(size = 20, sort = "contractDate") Pageable pageable
    ) {
        return ResponseEntity.ok(
                contractService.findByInstitution(
                        institutionPublicId, supplierName, fetchIfEmpty, pageable));
    }

    /**
     * Cross-institution supplier intelligence — finds all contracts where the
     * given supplier appears regardless of which institution was the buyer.
     *
     * <p>Use case: map a supplier's full government contract footprint across
     * all institutions in the registry. Accepts supplier name (partial,
     * case-insensitive), registration number (exact), or tax ID (exact).</p>
     *
     * <p>Requires ANALYST role — this is an active intelligence query across
     * the full contract dataset, not a passive dossier view.</p>
     *
     * @param query    supplier name (partial), registration number, or tax ID.
     * @param pageable pagination parameters (default: 20 per page).
     * @return page of matching contracts across all institutions.
     */
    @GetMapping("/supplier/search")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(
            summary = "Cross-institution supplier search",
            description = "Finds all contracts where the given supplier appears across all " +
                    "institutions — maps the supplier's full government contract footprint. " +
                    "Accepts supplier name (partial), registration number, or tax ID. " +
                    "ANALYST access required."
    )
    public ResponseEntity<Page<ExternalContractResponse>> searchBySupplier(
            @Parameter(description = "Supplier name (partial), registration number, or tax ID")
            @RequestParam String query,
            @PageableDefault(size = 20, sort = "contractDate") Pageable pageable
    ) {
        return ResponseEntity.ok(contractService.searchBySupplier(query, pageable));
    }
}