package com.projectfaust.occupation;

import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.occupation.dto.OccupationAscendedResponse;
import com.projectfaust.occupation.dto.OccupationRequest;
import com.projectfaust.occupation.dto.OccupationResponse;
import com.projectfaust.occupation.dto.OccupationTreeResponse;
import com.projectfaust.shared.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core service for managing occupational positions within the Project Faust
 * organisational intelligence layer.
 *
 * <p>An {@link Occupation} represents a named positional slot — independent of
 * whoever currently holds it. The service enforces two invariants:</p>
 * <ul>
 *   <li><b>Institution binding</b> — every position must belong to an existing institution.</li>
 *   <li><b>Circular reference guard</b> — a position cannot report to itself or
 *       any of its own subordinates, enforced via {@link HierarchyValidator}.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OccupationService {

    private final OccupationRepository occupationRepository;
    private final InstitutionRepository institutionRepository;
    private final OccupationMapper mapper;
    private final HierarchyValidator hierarchyValidator;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new occupational position and persists it.
     *
     * <p>Resolves institution and reporting-line references before saving.
     * Applies circular reference validation when a {@code reportsToPublicId}
     * is provided.</p>
     *
     * @param request the creation DTO.
     * @return the persisted position as an {@link OccupationResponse}.
     * @throws EntityNotFoundException if the institution or supervisor position is not found.
     */
    @Transactional
    public OccupationResponse create(OccupationRequest request) {
        log.info("FAUST_OCC: Creating position '{}' (code: {})", request.title(), request.code());

        Occupation entity = mapper.toEntity(request);

        Institution institution = institutionRepository.findByExternalId(request.institutionPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "INSTITUTION_NOT_FOUND: " + request.institutionPublicId()));
        entity.setInstitution(institution);

        if (request.reportsToPublicId() != null) {
            Occupation supervisor = occupationRepository.findByExternalId(request.reportsToPublicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "SUPERVISOR_POSITION_NOT_FOUND: " + request.reportsToPublicId()));

            hierarchyValidator.verifyNoCircularReference(entity, supervisor);
            entity.setReportsTo(supervisor);
        }

        return mapper.toResponse(occupationRepository.save(entity));
    }

    /**
     * Creates multiple occupational positions within a single database transaction.
     *
     * <p>The entire batch is rolled back if any single request fails validation.</p>
     *
     * @param requests list of position creation requests.
     * @return list of persisted positions as responses.
     */
    @Transactional
    public List<OccupationResponse> createBulk(List<OccupationRequest> requests) {
        log.info("FAUST_OCC_BULK: Processing {} occupation requests.", requests.size());
        return requests.stream()
                .map(request -> {
                    Occupation entity = mapper.toEntity(request);

                    Institution institution = institutionRepository
                            .findByExternalId(request.institutionPublicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "INSTITUTION_NOT_FOUND: " + request.institutionPublicId()));
                    entity.setInstitution(institution);

                    if (request.reportsToPublicId() != null) {
                        Occupation supervisor = occupationRepository
                                .findByExternalId(request.reportsToPublicId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "SUPERVISOR_POSITION_NOT_FOUND: " + request.reportsToPublicId()));
                        hierarchyValidator.verifyNoCircularReference(entity, supervisor);
                        entity.setReportsTo(supervisor);
                    }

                    return mapper.toResponse(occupationRepository.save(entity));
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single position by its public UUID.
     *
     * @param publicId the public UUID of the position.
     * @return the position as an {@link OccupationResponse}.
     * @throws EntityNotFoundException if no position matches the given UUID.
     */
    @Transactional(readOnly = true)
    public OccupationResponse getByPublicId(UUID publicId) {
        log.info("FAUST_OCC: Fetching position detail: {}", publicId);
        return occupationRepository.findByExternalId(publicId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("OCCUPATION_NOT_FOUND: " + publicId));
    }

    /**
     * Returns all positions belonging to the specified institution.
     *
     * <p>Uses the fetched variant with JOIN FETCH to avoid N+1 queries
     * when rendering the full position list for an institution.</p>
     *
     * @param institutionPublicId the public UUID of the institution.
     * @return list of positions as flat responses.
     */
    @Transactional(readOnly = true)
    public List<OccupationResponse> getByInstitution(UUID institutionPublicId) {
        log.info("FAUST_OCC: Fetching positions for institution: {}", institutionPublicId);
        return mapper.toResponseList(
                occupationRepository.findByInstitutionExternalIdFetched(institutionPublicId));
    }

    /**
     * Returns all direct subordinate positions of the specified superior.
     *
     * @param parentPublicId the public UUID of the superior position.
     * @return list of subordinate positions as flat responses.
     */
    @Transactional(readOnly = true)
    public List<OccupationResponse> getSubordinates(UUID parentPublicId) {
        log.info("FAUST_OCC: Fetching subordinates for position: {}", parentPublicId);
        return mapper.toResponseList(
                occupationRepository.findByReportsToExternalId(parentPublicId));
    }

    /**
     * Returns the full command chain — all top-level positions with their
     * subordinate hierarchy eagerly loaded.
     *
     * @return list of root positions as recursive tree responses.
     */
    @Transactional(readOnly = true)
    public List<OccupationTreeResponse> getCommandChain() {
        log.info("FAUST_OCC: Fetching full command chain.");
        return mapper.toTreeResponseList(occupationRepository.findTopLevelRoles());
    }

    /**
     * Returns the reporting hierarchy subtree rooted at the specified position.
     *
     * <p>Uses {@link OccupationRepository#findWithSubordinatesByExternalId} to
     * eagerly load subordinates, preventing lazy-load exceptions when the mapper
     * recursively builds the tree response.</p>
     *
     * @param publicId the public UUID of the root position.
     * @return a recursive tree response rooted at the specified position.
     * @throws EntityNotFoundException if no position matches the given UUID.
     */
    @Transactional(readOnly = true)
    public OccupationTreeResponse getCommandChainById(UUID publicId) {
        log.info("FAUST_OCC: Building command chain subtree for node: {}", publicId);
        Occupation root = occupationRepository.findWithSubordinatesByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("OCCUPATION_NOT_FOUND: " + publicId));
        return mapper.toTreeResponse(root);
    }

    /**
     * Resolves the full chain of command upward from the specified position.
     *
     * @param publicId the public UUID of the starting position.
     * @return the ascended chain of command for breadcrumb rendering.
     * @throws EntityNotFoundException if no position matches the given UUID.
     */
    @Transactional(readOnly = true)
    public OccupationAscendedResponse getAscendedChain(UUID publicId) {
        log.info("FAUST_OCC: Resolving chain of command for position: {}", publicId);
        Occupation leaf = occupationRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("OCCUPATION_NOT_FOUND: " + publicId));
        return mapper.toAscendedResponse(leaf);
    }
}