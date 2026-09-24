package com.projectfaust.institution;

import com.projectfaust.institution.dto.*;
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
 * REST controller for managing the institutional hierarchy within Project Faust.
 *
 * <p>Exposes endpoints covering the full lifecycle of organisational nodes —
 * from national ministries down to subordinate agencies and private entities.
 * Three distinct tree navigation patterns are supported:</p>
 * <ul>
 *   <li><b>Lazy drill-down</b> — {@code GET /tree} returns root nodes;
 *       {@code GET /parent/{id}} returns immediate children on demand.</li>
 *   <li><b>Subtree descent</b> — {@code GET /{id}/sub-tree} returns a deep
 *       recursive hierarchy downward from any node.</li>
 *   <li><b>Nexus Focus</b> — {@code GET /{id}/nexus-focus} returns the full
 *       organisational tree anchored at the root ancestor of any node.</li>
 * </ul>
 *
 * <p>Security tiers: {@code VIEWER} for all reads, {@code ANALYST} for create,
 * update, and identifier management, {@code ADMIN} for bulk import.</p>
 *
 * <p>Base path: {@code /api/v1/institutions}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Institution Management",
        description = "Operations for mapping the global political and state-corporate hierarchy")
public class InstitutionController {

    private final InstitutionService service;
    private final InstitutionIdentifierService identifierService;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Creates a new institutional node.
     *
     * @param request the creation DTO (validated).
     * @return the persisted institution with HTTP 201.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Create an institution",
            description = "Creates a node with security inheritance and hierarchy validation.")
    public ResponseEntity<InstitutionResponse> create(
            @Valid @RequestBody InstitutionRequest request) {
        return new ResponseEntity<>(service.create(request), HttpStatus.CREATED);
    }

    /**
     * Atomically creates multiple institutional nodes within a single transaction.
     *
     * @param requests list of institution creation requests (max 500).
     * @return list of persisted institutions with HTTP 201.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk create institutions",
            description = "Atomic transaction for seeding multiple nodes. Max 500 per request.")
    public ResponseEntity<List<InstitutionResponse>> createBulk(
            @Valid @RequestBody
            @Size(max = 500, message = "Bulk import is limited to 500 institutions per request.")
            List<@Valid InstitutionRequest> requests) {
        return new ResponseEntity<>(service.createBulk(requests), HttpStatus.CREATED);
    }

    /**
     * Updates an existing institution's scalar fields, location, and financial vectors.
     *
     * <p>Parent reassignment is not handled here — use the dedicated
     * reparent endpoint to ensure hierarchy validation is applied.</p>
     *
     * @param publicId the public UUID of the institution to update.
     * @param request  the update DTO (validated).
     * @return the updated institution.
     */
    @PutMapping("/{publicId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Update an institution",
            description = "Updates scalar fields and financial vectors. Parent changes require the reparent endpoint.")
    public ResponseEntity<InstitutionResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody InstitutionRequest request) {
        return ResponseEntity.ok(service.update(publicId, request));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — single node
    // -------------------------------------------------------------------------

    /**
     * Retrieves a complete institution profile including financial accounts and bank details.
     *
     * @param publicId the public UUID of the institution.
     * @return the fully-loaded institution profile.
     */
    @GetMapping("/{publicId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get institution by UUID",
            description = "Returns full details including financial accounts and location breadcrumbs.")
    public ResponseEntity<InstitutionResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    /**
     * Executes a paginated, filtered search over all active institutions.
     *
     * @param name       partial name match (case-insensitive).
     * @param country    ISO country code filter.
     * @param stateOwned filter by state ownership flag.
     * @param locationId filter by location UUID.
     * @param parentId   filter by parent institution UUID.
     * @param pageable   pagination and sorting (default: 20 per page, sorted by name).
     * @return a page of matching institutions.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Search institutions",
            description = "Dynamic filter search. All parameters are optional.")
    public ResponseEntity<Page<InstitutionResponse>> search(
            @Parameter(description = "Partial name match")
            @RequestParam(required = false) String name,
            @Parameter(description = "ISO country code")
            @RequestParam(required = false) String country,
            @Parameter(description = "Filter by state ownership")
            @RequestParam(required = false) Boolean stateOwned,
            @Parameter(description = "Filter by location UUID")
            @RequestParam(required = false) UUID locationId,
            @Parameter(description = "Filter by parent UUID")
            @RequestParam(required = false) UUID parentId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(
                service.search(name, country, stateOwned, locationId, parentId, pageable));
    }

    // -------------------------------------------------------------------------
    // Read endpoints — tree navigation
    // -------------------------------------------------------------------------

    /**
     * Returns all active root-level institutions for initial tree rendering.
     *
     * @return list of root institutions as flat tree nodes.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get root hierarchy nodes",
            description = "Returns top-level institutions. Use /parent/{id} to fetch deeper levels.")
    public ResponseEntity<List<InstitutionTreeResponse>> getRootNodes() {
        return ResponseEntity.ok(service.getRootNodes());
    }

    /**
     * Returns immediate children of the specified parent node for lazy-load drill-down.
     *
     * @param parentPublicId the public UUID of the parent institution.
     * @return list of immediate child institutions as flat tree nodes.
     */
    @GetMapping("/parent/{parentPublicId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get immediate children",
            description = "Returns immediate subordinate nodes for lazy loading in tree views.")
    public ResponseEntity<List<InstitutionTreeResponse>> getChildren(
            @PathVariable UUID parentPublicId) {
        return ResponseEntity.ok(service.getImmediateChildren(parentPublicId));
    }

    /**
     * Returns a deep recursive subtree rooted at the specified node.
     *
     * @param publicId the public UUID of the subtree root node.
     * @return deep tree response for graph visualisation.
     */
    @GetMapping("/{publicId}/sub-tree")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get sub-tree downwards",
            description = "Returns deep recursive hierarchy starting from this node.")
    public ResponseEntity<InstitutionTreeResponse> getSubTree(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getSubTree(publicId));
    }

    /**
     * Returns the full organisational tree anchored at the root ancestor of any node.
     *
     * @param publicId the public UUID of any node in the target hierarchy.
     * @return the full tree from root as a deep tree response.
     */
    @GetMapping("/{publicId}/nexus-focus")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get Nexus Focus tree",
            description = "Returns the full organisational tree from root ancestor, anchored at any node.")
    public ResponseEntity<InstitutionTreeResponse> getNexusFocus(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getNexusFocus(publicId));
    }

    /**
     * Resolves the full ancestor chain of an institution as an ascended breadcrumb path.
     *
     * @param publicId the public UUID of the target institution.
     * @return the ascended path response for breadcrumb rendering.
     */
    @GetMapping("/{publicId}/path-to-root")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get path to root",
            description = "Resolves the organisational lineage upwards to the top-level parent.")
    public ResponseEntity<InstitutionAscendedResponse> getPathToRoot(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getAscendedPath(publicId));
    }

    // -------------------------------------------------------------------------
    // Identifier endpoints — bridge to external data sources
    // -------------------------------------------------------------------------

    /**
     * Returns all active identifiers registered against an institution.
     *
     * @param publicId the public UUID of the institution.
     * @return list of active identifier records.
     */
    @GetMapping("/{publicId}/identifiers")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get institution identifiers",
            description = "Returns all active national and international registration " +
                    "identifiers (IČO, LEI, DUNS etc.) for this institution.")
    public ResponseEntity<List<InstitutionIdentifierResponse>> getIdentifiers(
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(identifierService.getAll(publicId));
    }

    /**
     * Registers a new identifier against an institution.
     *
     * <p>Adding a {@code NATIONAL_REGISTRATION} identifier (IČO for CZ) enables
     * automatic linkage to procurement contracts from Hlidač Státu.
     * Adding a {@code LEI} enables linkage to financial disclosures and sanctions lists.</p>
     *
     * @param publicId the public UUID of the institution.
     * @param request  the validated identifier request.
     * @return the created identifier record with HTTP 201.
     */
    @PostMapping("/{publicId}/identifiers")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Register institution identifier",
            description = "Registers a national or international registration identifier " +
                    "(IČO, LEI, DUNS, VAT etc.) to enable external data source linkage.")
    public ResponseEntity<InstitutionIdentifierResponse> addIdentifier(
            @PathVariable UUID publicId,
            @Valid @RequestBody InstitutionIdentifierRequest request) {
        return new ResponseEntity<>(
                identifierService.add(publicId, request),
                HttpStatus.CREATED
        );
    }

    /**
     * Deactivates (soft-deletes) an institution identifier.
     *
     * <p>The record is retained for audit purposes but no longer used
     * for external data source linkage.</p>
     *
     * @param publicId     the public UUID of the institution.
     * @param identifierId the UUID of the identifier to deactivate.
     * @return HTTP 204 No Content.
     */
    @DeleteMapping("/{publicId}/identifiers/{identifierId}")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Deactivate institution identifier",
            description = "Soft-deletes an identifier. Retained for audit; " +
                    "no longer used for contract or sanctions linkage.")
    public ResponseEntity<Void> deactivateIdentifier(
            @PathVariable UUID publicId,
            @PathVariable UUID identifierId) {
        identifierService.deactivate(publicId, identifierId);
        return ResponseEntity.noContent().build();
    }
}