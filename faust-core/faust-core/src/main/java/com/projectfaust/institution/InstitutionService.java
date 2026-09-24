package com.projectfaust.institution;

import com.projectfaust.financial.BankAccount;
import com.projectfaust.financial.BankAccountRepository;
import com.projectfaust.financial.InstitutionAccountRelation;
import com.projectfaust.institution.dto.InstitutionAscendedResponse;
import com.projectfaust.institution.dto.InstitutionRequest;
import com.projectfaust.institution.dto.InstitutionResponse;
import com.projectfaust.institution.dto.InstitutionTreeResponse;
import com.projectfaust.location.LocationRepository;
import com.projectfaust.location.LocationService;
import com.projectfaust.location.dto.LocationResponse;
import com.projectfaust.shared.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Core service for managing organisational nodes within the Project Faust intelligence platform.
 *
 * <p>This service is the single authority for all lifecycle operations on {@link Institution}
 * entities, enforcing the same domain invariants as {@code LocationService}:</p>
 * <ul>
 *   <li><b>Security inheritance</b> — a child institution can never hold a lower
 *       {@link com.projectfaust.shared.enums.ClearanceLevel} than its parent.</li>
 *   <li><b>Circular reference guard</b> — hierarchy changes are validated by
 *       {@link HierarchyValidator} before persistence.</li>
 * </ul>
 *
 * <p><b>FININT engine:</b> {@link #syncFinancialVectors} manages the relationship between
 * institutions and bank accounts. It supports both attaching existing global accounts
 * (by IBAN lookup) and creating new ones, with IBAN deduplication to prevent duplicate
 * account records across the system.</p>
 *
 * <p><b>Tree operations:</b> three distinct tree projections are supported —
 * immediate children (flat), subtree (deep), and Nexus Focus (full root tree).
 * Each uses a different fetch strategy to match its depth requirements.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository repository;
    private final InstitutionMapper mapper;
    private final HierarchyValidator hierarchyValidator;
    private final LocationService locationService;
    private final LocationRepository locationRepository;
    private final BankAccountRepository bankAccountRepository;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new institutional node and persists it.
     *
     * <p>Resolves location and parent references, applies security inheritance,
     * and synchronises financial account relationships before saving.</p>
     *
     * @param request the creation DTO.
     * @return the persisted institution as an enriched {@link InstitutionResponse}.
     */
    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        log.info("FAUST_INST: Creating institution: {}", request.name());
        Institution entity = prepareEntity(request);
        syncFinancialVectors(entity, request);
        Institution saved = repository.save(entity);
        return toEnrichedResponse(saved);
    }

    /**
     * Updates an existing institution's scalar fields, location, and financial vectors.
     *
     * <p>Parent reassignment is intentionally not handled here — use the dedicated
     * reparent endpoint to ensure hierarchy validation is always applied.</p>
     *
     * @param publicId the public UUID of the institution to update.
     * @param request  the update DTO.
     * @return the updated institution as an enriched {@link InstitutionResponse}.
     * @throws EntityNotFoundException if no institution matches the given UUID.
     */
    @Transactional
    public InstitutionResponse update(UUID publicId, InstitutionRequest request) {
        log.info("FAUST_INST: Updating institution: {}", publicId);
        Institution entity = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSTITUTION_NOT_FOUND: " + publicId));

        // Update scalar fields
        entity.setName(request.name());
        entity.setLevel(request.level());
        entity.setType(request.type());
        entity.setClearanceLevel(request.clearanceLevel());
        entity.setStateOwned(request.stateOwned());
        entity.setActive(request.active());
        entity.setDescription(request.description());
        entity.setLogoUrl(request.logoUrl());
        entity.setWebsiteUrl(request.websiteUrl());

        // Resolve location reference if provided
        if (request.locationId() != null) {
            entity.setLocation(locationRepository.findByExternalId(request.locationId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "LOCATION_NOT_FOUND: " + request.locationId())));
        }

        syncFinancialVectors(entity, request);
        return toEnrichedResponse(repository.save(entity));
    }

    /**
     * Creates multiple institutional nodes within a single database transaction.
     *
     * <p>The entire batch is rolled back if any single request fails validation.
     * Maximum batch size should be enforced at the controller level via
     * {@code @Size(max = 500)}.</p>
     *
     * @param requests list of institution creation requests.
     * @return list of persisted institutions as enriched responses.
     */
    @Transactional
    public List<InstitutionResponse> createBulk(List<InstitutionRequest> requests) {
        log.info("FAUST_INST_BULK: Processing {} requests.", requests.size());
        return requests.stream()
                .map(request -> {
                    Institution entity = prepareEntity(request);
                    syncFinancialVectors(entity, request);
                    return toEnrichedResponse(repository.save(entity));
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a complete institution profile including financial accounts and bank details.
     *
     * @param publicId the public UUID of the institution.
     * @return the fully-loaded institution as an enriched {@link InstitutionResponse}.
     * @throws EntityNotFoundException if no institution matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InstitutionResponse getByPublicId(UUID publicId) {
        return repository.findFullProfileByExternalId(publicId)
                .map(this::toEnrichedResponse)
                .orElseThrow(() -> new EntityNotFoundException("INSTITUTION_NOT_FOUND: " + publicId));
    }

    /**
     * Executes a paginated, filtered search over all active institutions.
     *
     * <p>Filter criteria are composed dynamically by {@link InstitutionSpecifications}.
     * Only non-null parameters produce predicates — omitting a parameter means
     * no restriction on that dimension.</p>
     *
     * @param name        partial name match (case-insensitive).
     * @param country     ISO country code filter.
     * @param stateOwned  filter by state ownership flag.
     * @param locationId  filter by location UUID.
     * @param parentId    filter by parent institution UUID.
     * @param pageable    pagination and sorting parameters.
     * @return a page of matching institutions as enriched responses.
     */
    @Transactional(readOnly = true)
    public Page<InstitutionResponse> search(
            String name, String country, Boolean stateOwned,
            UUID locationId, UUID parentId, Pageable pageable) {

        Specification<Institution> spec = Specification
                .where(InstitutionSpecifications.activeOnly())
                .and(InstitutionSpecifications.nameContains(name))
                .and(InstitutionSpecifications.hasCountry(country))
                .and(InstitutionSpecifications.isStateOwned(stateOwned))
                // CHANGED: hasLocation(locationId) -> hasLocationOrDescendant(...).
                // The old version only matched institutions whose location_id
                // pointed at this EXACT node. Institutions are typically attached
                // to specific buildings/facilities, while location pages are
                // frequently viewed at a broader level (city, region, country) —
                // so this caused "Bound_Institutional_Nodes" on LocationDetailPage
                // to always render empty for any non-leaf location, such as Prague.
                .and(InstitutionSpecifications.hasLocationOrDescendant(locationId, locationRepository))
                .and(InstitutionSpecifications.hasParent(parentId));

        return repository.findAll(spec, pageable)
                .map(this::toEnrichedResponse);
    }

    // -------------------------------------------------------------------------
    // Tree / hierarchy operations
    // -------------------------------------------------------------------------

    /**
     * Returns all active root-level institutions (institutions with no parent).
     *
     * @return list of root institutions as flat tree nodes.
     */
    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getRootNodes() {
        log.info("FAUST_QUERY: Fetching top-level roots.");
        return repository.findAllRoots().stream()
                .map(mapper::toFlatTreeResponse)
                .toList();
    }

    /**
     * Returns all active direct children of the specified parent institution.
     *
     * @param parentPublicId the public UUID of the parent institution.
     * @return list of immediate children as flat tree nodes.
     */
    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getImmediateChildren(UUID parentPublicId) {
        log.info("FAUST_QUERY: Fetching immediate children for parent: {}", parentPublicId);
        return repository.findActiveChildrenByParentExternalId(parentPublicId).stream()
                .map(mapper::toFlatTreeResponse)
                .toList();
    }

    /**
     * Returns a subtree rooted at the specified node, loading up to two levels of children.
     *
     * @param publicId the public UUID of the subtree root node.
     * @return a deep tree response for the Nexus Focus visualisation.
     * @throws EntityNotFoundException if no institution matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InstitutionTreeResponse getSubTree(UUID publicId) {
        log.info("FAUST_NEXUS: Loading deep hierarchy for node: {}", publicId);
        Institution node = repository.findWithDeepHierarchyByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSTITUTION_NOT_FOUND: " + publicId));
        return mapper.toTreeResponse(node);
    }

    /**
     * Returns the full organisational tree starting from the root ancestor of the
     * specified node — the Nexus Focus view.
     *
     * <p>The root ancestor is resolved via the ancestor path query to avoid the
     * N+1 lazy-load problem of walking the parent chain in Java.</p>
     *
     * @param publicId the public UUID of any node in the target hierarchy.
     * @return the full tree from root as a deep {@link InstitutionTreeResponse}.
     * @throws EntityNotFoundException if no institution matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InstitutionTreeResponse getNexusFocus(UUID publicId) {
        // Resolve the root ancestor via a single DB query — avoids N+1 parent chain walk
        Institution root = repository.findRootByDescendantExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSTITUTION_NOT_FOUND: " + publicId));

        // Load the full tree from root with deep hierarchy
        Institution rootWithTree = repository.findWithDeepHierarchyByExternalId(root.getExternalId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "INSTITUTION_TREE_LOAD_FAILED: " + root.getExternalId()));

        return mapper.toTreeResponse(rootWithTree);
    }

    /**
     * Resolves the full ancestor chain of an institution as an ascended (breadcrumb) path.
     *
     * @param publicId the public UUID of the target institution.
     * @return the ascended path response for breadcrumb rendering.
     * @throws EntityNotFoundException if no institution matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InstitutionAscendedResponse getAscendedPath(UUID publicId) {
        log.info("FAUST_QUERY: Resolving ascended path for node: {}", publicId);
        Institution leaf = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSTITUTION_NOT_FOUND: " + publicId));
        return mapper.toAscendedResponse(leaf);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves location and parent references on a freshly mapped entity,
     * and applies security inheritance if a parent is present.
     *
     * @param request the creation DTO.
     * @return a prepared entity ready for financial sync and persistence.
     */
    private Institution prepareEntity(InstitutionRequest request) {
        Institution entity = mapper.toEntity(request);

        // Resolve location reference
        if (request.locationId() != null) {
            entity.setLocation(locationRepository.findByExternalId(request.locationId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "LOCATION_NOT_FOUND: " + request.locationId())));
        }

        // Resolve parent reference and enforce hierarchy invariants
        if (request.parentExternalId() != null) {
            Institution parent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "PARENT_INSTITUTION_NOT_FOUND: " + request.parentExternalId()));

            hierarchyValidator.verifyNoCircularReference(entity, parent);
            entity.setParent(parent);

            // Security inheritance — child clearance can never be below parent
            if (entity.getClearanceLevel().getWeight() < parent.getClearanceLevel().getWeight()) {
                log.warn("FAUST_INST: Elevating clearance of '{}' to match parent: {}",
                        entity.getName(), parent.getClearanceLevel());
                entity.setClearanceLevel(parent.getClearanceLevel());
            }
        }
        return entity;
    }

    /**
     * Synchronises the financial account relationships for an institution.
     *
     * <p>This is the FININT vector engine. It supports two modes per account entry:</p>
     * <ul>
     *   <li><b>Attach existing:</b> looks up a global {@link BankAccount} by its
     *       {@code externalId} and links it to the institution.</li>
     *   <li><b>Create new:</b> checks for an existing account by IBAN to prevent
     *       duplicates, then creates a new {@link BankAccount} if none exists.</li>
     * </ul>
     *
     * <p><b>Must be called within an active {@code @Transactional} context.</b>
     * The method clears the existing financial accounts collection before repopulating —
     * this is intentional and safe because cascade is limited to PERSIST and MERGE,
     * meaning cleared relations are not auto-deleted from the database.</p>
     *
     * @param institution the institution entity being prepared for persistence.
     * @param request     the creation or update DTO containing financial account entries.
     */
    private void syncFinancialVectors(Institution institution, InstitutionRequest request) {
        if (institution.getFinancialAccounts() == null) {
            institution.setFinancialAccounts(new LinkedHashSet<>());
        } else {
            institution.getFinancialAccounts().clear();
        }

        if (request.financialAccounts() == null || request.financialAccounts().isEmpty()) {
            return;
        }

        for (var finReq : request.financialAccounts()) {
            BankAccount account;

            if (finReq.bankAccountExternalId() != null) {
                // Attach an existing global bank account by UUID
                account = bankAccountRepository.findByExternalId(finReq.bankAccountExternalId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "BANK_ACCOUNT_NOT_FOUND: " + finReq.bankAccountExternalId()));

            } else if (finReq.newBankAccountData() != null) {
                // Normalise IBAN — strip whitespace for consistent deduplication
                String normalizedIban = finReq.newBankAccountData().iban()
                        .trim().replaceAll("\\s+", "");

                // IBAN deduplication — reuse existing account if already in system
                account = bankAccountRepository.findByIban(normalizedIban)
                        .orElseGet(() -> bankAccountRepository.save(
                                BankAccount.builder()
                                        .iban(normalizedIban)
                                        .bic(finReq.newBankAccountData().bic() != null
                                                ? finReq.newBankAccountData().bic().trim() : null)
                                        .bankName(finReq.newBankAccountData().bankName().trim())
                                        .currency(finReq.newBankAccountData().currency()
                                                .toUpperCase().trim())
                                        .monitored(finReq.newBankAccountData().monitored())
                                        .analyticalNote(finReq.newBankAccountData().analyticalNote())
                                        .build()));
            } else {
                // Neither existing ID nor new data provided — skip this entry
                log.warn("FAUST_FININT: Skipping financial entry with no account data for institution '{}'.",
                        institution.getName());
                continue;
            }

            institution.getFinancialAccounts().add(
                    InstitutionAccountRelation.builder()
                            .institution(institution)
                            .bankAccount(account)
                            .roleType(finReq.roleType())
                            .validFrom(finReq.validFrom())
                            .validTo(finReq.validTo())
                            .active(finReq.active())
                            .build());
        }
    }

    /**
     * Converts an {@link Institution} entity to an enriched {@link InstitutionResponse},
     * resolving the {@code hasChildren} flag and the location breadcrumb path.
     *
     * <p>The {@code hasChildren} flag is resolved via a lightweight existence query
     * to avoid triggering a lazy collection load inside the mapper.</p>
     *
     * <p>The location path (breadcrumbs) is resolved from the GEOINT layer only if
     * the institution has a location reference.</p>
     *
     * @param entity the institution entity.
     * @return an enriched response with hasChildren and location path resolved.
     */
    private InstitutionResponse toEnrichedResponse(Institution entity) {
        boolean hasChildren = repository.existsByParentExternalId(entity.getExternalId());

        List<LocationResponse> locationPath = (entity.getLocation() != null)
                ? locationService.getLocationPath(entity.getLocation().getExternalId())
                : Collections.emptyList();

        InstitutionResponse base = mapper.toResponse(entity, hasChildren);

        // Return a new record with the enriched locationPath field
        return new InstitutionResponse(
                base.publicId(),
                base.name(),
                base.level(),
                base.type(),
                base.clearanceLevel(),
                base.parentId(),
                base.hasChildren(),
                base.stateOwned(),
                base.active(),
                base.description(),
                base.locationId(),
                base.locationName(),
                locationPath,
                base.logoUrl(),
                base.websiteUrl(),
                base.financialAccounts(),
                base.verificationStatus(),
                base.createdAt(),
                base.updatedAt()
        );
    }
}
