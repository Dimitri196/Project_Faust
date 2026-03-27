package com.projectfaust.service;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.filters.LocationFilter;
import com.projectfaust.mapper.LocationMapper;
import com.projectfaust.repository.LocationRepository;
import com.projectfaust.specification.LocationSpecifications;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final HierarchyValidator hierarchyValidator;

    /**
     * Vytvoří novou lokalitu. Implementuje Security Inheritance.
     */
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        log.info("FAUST_GEO: Creating node '{}' of type {}", request.name(), request.type());

        Location location = locationMapper.toEntity(request);

        if (request.parentExternalId() != null) {
            Location parent = locationRepository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("PARENT_NOT_FOUND: " + request.parentExternalId()));

            // Validace cyklů
            hierarchyValidator.verifyNoCircularReference(location, parent);

            // Validace granularity
            if (!location.getType().isValidChildOf(parent.getType())) {
                throw new IllegalStateException(String.format(
                        "GRANULARITY_VIOLATION: Cannot place %s under %s.",
                        location.getType(), parent.getType()));
            }

            location.setParent(parent);

            // Security Inheritance: Potomek nesmí mít nižší prověrku než rodič
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

        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Vyhledá lokace na základě komplexního filtru se stránkováním.
     * Využívá centralizovaný LocationSpecifications.build.
     */
    @Transactional(readOnly = true)
    public Page<LocationResponse> search(LocationFilter filter, Pageable pageable) {
        log.debug("FAUST_GEO: Executing search with filter: {}", filter);

        // Použití build metody řeší "Cannot resolve method" chybu
        Specification<Location> spec = LocationSpecifications.build(filter);

        return locationRepository.findAll(spec, pageable)
                .map(locationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getRootLocations() {
        return locationRepository.findAllByParentIsNullAndActiveTrue().stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getSubLocations(UUID parentExternalId) {
        if (locationRepository.findByExternalId(parentExternalId).isEmpty()) {
            throw new EntityNotFoundException("LOCATION_NOT_FOUND: " + parentExternalId);
        }
        return locationRepository.findAllActiveSubLocations(parentExternalId).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Sestaví hierarchickou cestu (breadcrumbs).
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationPath(UUID externalId) {
        LinkedList<LocationResponse> path = new LinkedList<>();
        Location current = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND"));

        while (current != null) {
            path.addFirst(locationMapper.toResponse(current));
            current = current.getParent();
        }
        return path;
    }

    @Transactional(readOnly = true)
    public LocationResponse getLocationByExternalId(UUID externalId) {
        return locationRepository.findByExternalId(externalId)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("DATA_QUERY_FAILURE: " + externalId));
    }

    @Transactional
    public LocationResponse updateParent(UUID locationId, UUID newParentId) {
        Location location = locationRepository.findByExternalId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("TARGET_NODE_NOT_FOUND"));

        Location newParent = locationRepository.findByExternalId(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("PROPOSED_PARENT_NODE_NOT_FOUND"));

        hierarchyValidator.verifyNoCircularReference(location, newParent);

        location.setParent(newParent);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public void deactivateLocation(UUID externalId) {
        Location location = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("TERMINATION_FAILED: NODE_NOT_EXISTENT"));
        location.setActive(false);
        locationRepository.save(location);
        log.warn("FAUST_GEO: Node {} deactivated.", externalId);
    }

    @Transactional
    public List<LocationResponse> createLocationsBulk(List<LocationRequest> requests) {
        return requests.stream()
                .map(this::createLocation)
                .toList();
    }
}
