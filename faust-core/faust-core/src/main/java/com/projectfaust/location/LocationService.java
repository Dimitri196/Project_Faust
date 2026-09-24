package com.projectfaust.location;

import com.projectfaust.location.dto.LocationRequest;
import com.projectfaust.location.dto.LocationResponse;
import com.projectfaust.shared.enums.ClearanceLevel;
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
import java.util.List;
import java.util.UUID;

/**
 * Core service for managing geographic nodes within the Project Faust spatial intelligence layer.
 *
 * <p>This service is the single authority for all lifecycle operations on {@link Location} entities,
 * enforcing two critical intelligence domain invariants on every write operation:</p>
 *
 * <ul>
 *   <li><b>Security Inheritance</b> — a child node can never hold a lower
 *       {@link ClearanceLevel} than its parent. Any attempt to assign a lower
 *       clearance is silently elevated and logged.</li>
 *   <li><b>Granularity Validation</b> — placement of a node type under an incompatible
 *       parent type (e.g., a ROOM directly under a COUNTRY) is rejected via
 *       {@link LocationType#isValidChildOf(LocationType)}.</li>
 * </ul>
 *
 * <p>All read operations are marked {@code readOnly = true} to allow the JPA provider
 * to skip dirty checking and optimise connection usage.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final HierarchyValidator hierarchyValidator;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a single geographic node and persists it.
     *
     * <p>If a {@code parentExternalId} is supplied in the request, the following
     * validations are executed before persistence:</p>
     * <ol>
     *   <li>Parent existence check.</li>
     *   <li>Circular reference guard via {@link HierarchyValidator}.</li>
     *   <li>Granularity check — child type must be a valid child of parent type.</li>
     *   <li>Security inheritance — child clearance is elevated if below parent.</li>
     * </ol>
     *
     * @param request the incoming DTO containing all location attributes.
     * @return the persisted location as a flattened {@link LocationResponse}.
     * @throws EntityNotFoundException  if the specified parent does not exist.
     * @throws IllegalStateException    if the child/parent type combination violates granularity rules.
     */
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        log.info("FAUST_GEO: Creating node '{}' of type {}", request.name(), request.type());

        Location location = locationMapper.toEntity(request);
        applyParentLogic(location, request.parentExternalId());

        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Creates multiple geographic nodes within a single database transaction.
     *
     * <p>Unlike calling {@link #createLocation} in a loop (which would open N separate
     * transactions), this method processes the entire batch atomically. If any single
     * request fails validation, the whole batch is rolled back.</p>
     *
     * @param requests list of location creation requests.
     * @return list of persisted locations as {@link LocationResponse} objects,
     *         in the same order as the input.
     * @throws EntityNotFoundException if any referenced parent does not exist.
     * @throws IllegalStateException   if any type granularity rule is violated.
     */
    @Transactional
    public List<LocationResponse> createLocationsBulk(List<LocationRequest> requests) {
        log.info("FAUST_GEO: Bulk creating {} nodes.", requests.size());
        return requests.stream()
                .map(request -> {
                    Location location = locationMapper.toEntity(request);
                    applyParentLogic(location, request.parentExternalId());
                    return locationMapper.toResponse(locationRepository.save(location));
                })
                .toList();
    }

    /**
     * Relocates a node to a new parent within the spatial hierarchy.
     *
     * <p>In addition to the circular reference guard, this method re-applies the
     * security inheritance rule after reparenting — if the new parent carries a
     * higher clearance, the node's clearance is elevated accordingly.</p>
     *
     * @param locationId  the {@code externalId} of the node to relocate.
     * @param newParentId the {@code externalId} of the intended new parent.
     * @return the updated location as a {@link LocationResponse}.
     * @throws EntityNotFoundException if either node cannot be found.
     */
    @Transactional
    public LocationResponse updateParent(UUID locationId, UUID newParentId) {
        Location location = locationRepository.findByExternalId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("TARGET_NODE_NOT_FOUND: " + locationId));

        Location newParent = locationRepository.findByExternalId(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("PROPOSED_PARENT_NODE_NOT_FOUND: " + newParentId));

        hierarchyValidator.verifyNoCircularReference(location, newParent);
        location.setParent(newParent);

        // Re-apply security inheritance after reparenting
        if (location.getClearanceLevel().getWeight() < newParent.getClearanceLevel().getWeight()) {
            log.warn("FAUST_GEO: Reparent elevated clearance of '{}' to: {}",
                    location.getName(), newParent.getClearanceLevel());
            location.setClearanceLevel(newParent.getClearanceLevel());
        }

        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Soft-deactivates a geographic node, marking it as inactive without physical deletion.
     *
     * <p>Deactivation cascades to all direct and indirect children — a deactivated
     * COUNTRY implicitly renders all its CITY, BUILDING, and ROOM descendants
     * operationally inactive. Children are updated via a single bulk repository call
     * to avoid N+1 updates.</p>
     *
     * @param externalId the public {@code UUID} of the node to deactivate.
     * @throws EntityNotFoundException if the node does not exist.
     */
    @Transactional
    public void deactivateLocation(UUID externalId) {
        Location location = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("TERMINATION_FAILED: NODE_NOT_FOUND: " + externalId));

        location.setActive(false);
        locationRepository.save(location);

        // Cascade deactivation to all descendants
        int affected = locationRepository.deactivateAllDescendants(externalId);
        log.warn("FAUST_GEO: Node {} deactivated. Cascaded to {} descendant(s).", externalId, affected);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Executes a paginated, filtered search over all location nodes.
     *
     * <p>The filter predicate is assembled by {@link LocationSpecifications#build(LocationFilter)},
     * which composes only the non-null criteria present in the filter object.</p>
     *
     * @param filter   the search criteria (nullable fields are ignored).
     * @param pageable pagination and sorting parameters.
     * @return a page of matching locations mapped to {@link LocationResponse}.
     */
    @Transactional(readOnly = true)
    public Page<LocationResponse> search(LocationFilter filter, Pageable pageable) {
        log.debug("FAUST_GEO: Executing search with filter: {}", filter);
        Specification<Location> spec = LocationSpecifications.build(filter);
        return locationRepository.findAll(spec, pageable)
                .map(locationMapper::toResponse);
    }

    /**
     * Returns all active root-level nodes (nodes with no parent).
     *
     * <p>In a standard Faust deployment these are sovereign states or
     * top-level classified regions.</p>
     *
     * @return list of active root locations, never {@code null}.
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getRootLocations() {
        return locationRepository.findAllByParentIsNullAndActiveTrue().stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    /**
     * Returns all active direct children of the specified parent node.
     *
     * @param parentExternalId the {@code UUID} of the parent node.
     * @return list of active child locations.
     * @throws EntityNotFoundException if the parent node does not exist.
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getSubLocations(UUID parentExternalId) {
        // existsByExternalId avoids a full entity fetch just for existence check
        if (!locationRepository.existsByExternalId(parentExternalId)) {
            throw new EntityNotFoundException("LOCATION_NOT_FOUND: " + parentExternalId);
        }
        return locationRepository.findAllActiveSubLocations(parentExternalId).stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    /**
     * Resolves the full ancestor chain of a node as an ordered breadcrumb path.
     *
     * <p>The path is assembled via a single recursive CTE database query, avoiding
     * the N+1 lazy-load problem of traversing the parent chain in Java.
     * The result is ordered from root to the requested node (e.g., CZ → Prague → Strakova).</p>
     *
     * @param externalId the {@code UUID} of the target node.
     * @return ordered list of ancestors from root to node, inclusive.
     * @throws EntityNotFoundException if the node does not exist.
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationPath(UUID externalId) {
        List<Location> path = locationRepository.findAncestorPath(externalId);
        if (path.isEmpty()) {
            throw new EntityNotFoundException("LOCATION_NOT_FOUND: " + externalId);
        }
        // Recursive CTE returns child-first; reverse for root-first breadcrumb order
        Collections.reverse(path);
        return path.stream()
                .map(locationMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves a single location by its public {@code UUID}.
     *
     * @param externalId the public identifier of the location.
     * @return the matched location as a {@link LocationResponse}.
     * @throws EntityNotFoundException if no location matches the given UUID.
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocationByExternalId(UUID externalId) {
        return locationRepository.findByExternalId(externalId)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("DATA_QUERY_FAILURE: " + externalId));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Applies parent resolution, circular reference checks, granularity validation,
     * and security inheritance to a freshly constructed {@link Location} entity.
     *
     * <p>Extracted into a shared helper so both {@link #createLocation} and
     * {@link #createLocationsBulk} enforce identical rules without duplication.</p>
     *
     * @param location         the entity being prepared for persistence.
     * @param parentExternalId the UUID of the intended parent, or {@code null} for root nodes.
     */
    private void applyParentLogic(Location location, UUID parentExternalId) {
        if (parentExternalId != null) {
            Location parent = locationRepository.findByExternalId(parentExternalId)
                    .orElseThrow(() -> new EntityNotFoundException("PARENT_NOT_FOUND: " + parentExternalId));

            // Circular reference guard
            hierarchyValidator.verifyNoCircularReference(location, parent);

            // Granularity check — e.g. ROOM cannot be placed directly under COUNTRY
            if (!location.getType().isValidChildOf(parent.getType())) {
                throw new IllegalStateException(String.format(
                        "GRANULARITY_VIOLATION: Cannot place %s under %s.",
                        location.getType(), parent.getType()));
            }

            location.setParent(parent);

            // Security inheritance — child clearance can never be lower than parent
            if (location.getClearanceLevel().getWeight() < parent.getClearanceLevel().getWeight()) {
                log.warn("FAUST_GEO: Elevating clearance of '{}' to match parent level: {}",
                        location.getName(), parent.getClearanceLevel());
                location.setClearanceLevel(parent.getClearanceLevel());
            }
        } else {
            if (location.getClearanceLevel() == null) {
                location.setClearanceLevel(ClearanceLevel.LEVEL_1_PUBLIC);
            }
        }
    }
}