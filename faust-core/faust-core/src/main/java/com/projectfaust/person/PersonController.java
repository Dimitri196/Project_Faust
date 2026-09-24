package com.projectfaust.person;

import com.projectfaust.person.dto.AdditionalNameRequest;
import com.projectfaust.person.dto.PersonNameResponse;
import com.projectfaust.person.dto.PersonRequest;
import com.projectfaust.person.dto.PersonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing person profiles within Project Faust.
 *
 * <p>Person is the most sensitive entity in the system — it is the core HUMINT subject.
 * All endpoints are protected with clearance-based security annotations.</p>
 *
 * <p>Security tiers: {@code VIEWER} for reads, {@code ANALYST} for create/update,
 * {@code ADMIN} for bulk import.</p>
 *
 * <p>Base path: {@code /api/v1/persons}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person Management",
        description = "Operations for managing human intelligence profiles")
public class PersonController {

    private final PersonService service;
    private final PersonNameService nameService;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Creates a new person profile with primary name, contacts, and financial vectors.
     *
     * @param request the creation DTO (validated).
     * @return the persisted profile with HTTP 201.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Create a person",
            description = "Creates a new HUMINT profile. Initialises primary name, contact vectors, and financial accounts.")
    public ResponseEntity<PersonResponse> create(
            @Valid @RequestBody PersonRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Atomically creates multiple person profiles within a single transaction.
     *
     * @param requests list of creation requests (max 500).
     * @return list of persisted profiles with HTTP 201.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk import persons",
            description = "Atomic ingestion of multiple HUMINT profiles. Max 500 per request.")
    public ResponseEntity<List<PersonResponse>> createBulk(
            @Valid @RequestBody
            @Size(max = 500, message = "Bulk import is limited to 500 persons per request.")
            List<@Valid PersonRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Updates an existing person's biographical data and resynchronises all collections.
     *
     * @param publicId the public UUID of the person to update.
     * @param request  the update DTO (validated).
     * @return the updated profile.
     */
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Update person",
            description = "Updates biographical data and resynchronises name, contact, and financial vectors.")
    public ResponseEntity<PersonResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody PersonRequest request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — single and paginated
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated list of all persons.
     *
     * @param pageable pagination and sorting (default: 20 per page, sorted by externalId).
     * @return a page of person responses.
     */
    @GetMapping
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "List all persons (paginated)")
    public ResponseEntity<Page<PersonResponse>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    /**
     * Retrieves a full person dossier by public UUID.
     *
     * @param publicId the public UUID of the person.
     * @return the full dossier including names, contacts, financial accounts, and current positions.
     */
    @GetMapping("/{publicId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get person by UUID",
            description = "Returns the full HUMINT dossier including all intelligence collections.")
    public ResponseEntity<PersonResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — search
    // -------------------------------------------------------------------------

    /**
     * Searches persons by primary name using the normalised DB search column.
     *
     * @param query the search term.
     * @return list of matching persons.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Search by primary name",
            description = "Fast diacritic-insensitive search using the DB-generated normalised name column.")
    public ResponseEntity<List<PersonResponse>> search(
            @Parameter(description = "Search term") @RequestParam String query) {
        return ResponseEntity.ok(service.searchByName(query));
    }

    /**
     * Deep alias search across the full name history — aliases, cover names, maiden names.
     *
     * @param query the search term.
     * @return list of matching persons.
     */
    @GetMapping("/search/deep")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Deep identity search",
            description = "Searches across all historical names and aliases. Slower but comprehensive.")
    public ResponseEntity<List<PersonResponse>> searchDeep(
            @Parameter(description = "Search term") @RequestParam String query) {
        return ResponseEntity.ok(service.searchByAnyIdentity(query));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — SIGINT / FININT intelligence lookups
    // -------------------------------------------------------------------------

    /**
     * Identifies all persons linked to a specific device by IMEI.
     *
     * <p>Multiple persons sharing an IMEI is a high-value SIGINT signal.</p>
     *
     * @param imei the IMEI of the target device.
     * @return list of persons whose contact records include the given IMEI.
     */
    @GetMapping("/search/imei")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "IMEI device lookup",
            description = "Finds all persons linked to a specific device IMEI. High-value SIGINT signal.")
    public ResponseEntity<List<PersonResponse>> findByImei(
            @Parameter(description = "Device IMEI") @RequestParam String imei) {
        return ResponseEntity.ok(service.findByImei(imei));
    }

    /**
     * Finds all persons linked to a specific bank account by IBAN.
     *
     * <p>Used for FININT cross-link analysis — surfaces all account holders
     * and signatories regardless of role type.</p>
     *
     * @param iban the IBAN of the target account.
     * @return list of persons with a financial relationship to the given IBAN.
     */
    @GetMapping("/search/iban")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "IBAN cross-link lookup",
            description = "Finds all persons linked to a specific IBAN. FININT cross-reference analysis.")
    public ResponseEntity<List<PersonResponse>> findByIban(
            @Parameter(description = "Bank account IBAN") @RequestParam String iban) {
        return ResponseEntity.ok(service.findByIban(iban));
    }


    /**
     * Multi-dimensional paginated search across persons.
     *
     * <p>Unlike {@link #search(String)} (name-only, unpaginated, capped
     * implicitly by whatever the DB returns) and {@link #searchDeep(String)}
     * (alias search, also unpaginated), this endpoint supports combining a
     * free-text name query with clearance level, minimum clearance level,
     * education level, nationality, and verification status filters — all
     * optional — and returns a proper {@link Page}, suitable for browsing a
     * registry of any size (thousands to millions of records) without loading
     * more than one page into memory at a time.</p>
     *
     * <p>All filter fields are optional. Omitting all of them returns every
     * person, paginated — equivalent to {@link #getAll(Pageable)} but via a
     * single unified endpoint the frontend can always call regardless of
     * whether the user has applied filters.</p>
     *
     * @param filter   the search/filter criteria; all fields optional.
     * @param pageable pagination and sorting (default: 20 per page).
     * @return a page of matching persons.
     */
    @GetMapping("/search/filtered")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Filtered, paginated person search",
            description = "Combines name search with clearance, education, nationality, and " +
                    "verification status filters. Returns a proper page — safe to use against " +
                    "registries of any size.")
    public ResponseEntity<Page<PersonResponse>> searchFiltered(
            PersonFilter filter,
            @PageableDefault(size = 20, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(service.searchFiltered(filter, pageable));
    }

    /**
     * Returns all name records for a person — primary name plus all
     * aliases, cover names, and historical names.
     *
     * @param publicId the public UUID of the person.
     * @return list of name records.
     */
    @GetMapping("/{publicId}/names")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get person name history",
            description = "Returns the primary name and all additional aliases, " +
                    "cover names, and historical names for this person.")
    public ResponseEntity<List<PersonNameResponse>> getNames(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(nameService.getAll(publicId));
    }

    /**
     * Adds a single additional name (alias, cover name, maiden name, etc.)
     * to a person without touching contacts, financial accounts, or any
     * biographical field.
     *
     * <p>Use this instead of {@code PUT /{publicId}} when only adding a name —
     * the full update endpoint resynchronises the entire profile and will
     * wipe contacts/financial accounts if they are omitted from the request.</p>
     *
     * @param publicId the public UUID of the person.
     * @param request  the additional name to add (validated).
     * @return the created name record with HTTP 201.
     */
    @PostMapping("/{publicId}/names")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Add an additional name",
            description = "Appends a single alias, cover name, or maiden name to a person's " +
                    "identity history without resynchronising contacts or financial accounts. " +
                    "Never affects the primary name.")
    public ResponseEntity<PersonNameResponse> addName(
            @PathVariable UUID publicId,
            @Valid @RequestBody AdditionalNameRequest request) {
        return new ResponseEntity<>(nameService.addName(publicId, request), HttpStatus.CREATED);
    }

    /**
     * Deactivates a name record by setting its end date — never physically
     * deletes it (Hibernate Envers preserves the full audit trail).
     *
     * @param publicId     the public UUID of the person (path consistency;
     *                     not currently used for authorization scoping —
     *                     the name lookup is by nameId alone).
     * @param nameId       the public UUID of the name record to deactivate.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{publicId}/names/{nameId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Deactivate a name record",
            description = "Soft-deactivates an alias or historical name by setting validTo. " +
                    "Cannot deactivate the primary name. Retained for audit via Envers.")
    public ResponseEntity<Void> deactivateName(
            @PathVariable UUID publicId,
            @PathVariable UUID nameId) {
        nameService.deactivate(nameId);
        return ResponseEntity.noContent().build();
    }

}