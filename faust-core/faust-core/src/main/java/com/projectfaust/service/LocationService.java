package com.projectfaust.service;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.mapper.LocationMapper;
import com.projectfaust.repository.LocationRepository;
import com.projectfaust.specification.LocationSpecifications;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

        // 1. Mapování (Ujisti se, že mapper zná rozdíl mezi 'clearance' v requestu a 'clearanceLevel' v entitě)
        Location location = locationMapper.toEntity(request);

        if (request.parentExternalId() != null) {
            Location parent = locationRepository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("PARENT_NOT_FOUND: " + request.parentExternalId()));

            // 2. Validace cyklů (Využívá tvůj generický HierarchyValidator)
            hierarchyValidator.verifyNoCircularReference(location, parent);

            // 3. Validace granularity (NOVÉ)
            // Brání vytvoření COUNTRY pod CITY apod.
            if (!location.getType().isValidChildOf(parent.getType())) {
                throw new IllegalStateException(String.format(
                        "GRANULARITY_VIOLATION: Cannot place %s under %s. Logic: child granularity must be > parent granularity.",
                        location.getType(), parent.getType()));
            }

            location.setParent(parent);

            // 4. Bezpečnostní pojistka (Security Inheritance)
            if (location.getClearanceLevel().getWeight() < parent.getClearanceLevel().getWeight()) {
                log.warn("FAUST_GEO: Elevating clearance of '{}' to match parent level: {}",
                        location.getName(), parent.getClearanceLevel());
                location.setClearanceLevel(parent.getClearanceLevel());
            }
        } else {
            // Fallback pro root uzly (pokud není zadán clearance, nastavíme Public)
            if (location.getClearanceLevel() == null) {
                location.setClearanceLevel(ClearanceLevel.LEVEL_1_PUBLIC);
            }
        }

        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Hromadné vytvoření lokalit.
     * Vzhledem k transakčnosti (rollback při chybě) je vhodné pro konzistentní importy hierarchií.
     */
    @Transactional
    public List<LocationResponse> createLocationsBulk(List<LocationRequest> requests) {
        log.info("FAUST_GEO: Initiating bulk import for {} location requests", requests.size());

        return requests.stream()
                .map(this::createLocation) // Reusing existing logic including hierarchy & security checks
                .toList();
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
     * Sestaví hierarchickou cestu (např. Česko -> Praha -> Strakova akademie).
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

    /**
     * Soft-deaktivace uzlu. V systému Faust zachováváme historickou stopu.
     */
    @Transactional
    public void deactivateLocation(UUID externalId) {
        Location location = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("TERMINATION_FAILED: NODE_NOT_EXISTENT"));
        location.setActive(false);
        locationRepository.save(location);
        log.warn("FAUST_GEO: Node {} deactivated.", externalId);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> search(String name, String type, UUID parentId) {
        // Implementace Specification je oddělena v LocationSpecifications
        Specification<Location> spec = Specification.where(LocationSpecifications.activeOnly())
                .and(LocationSpecifications.nameContains(name))
                .and(LocationSpecifications.hasType(type))
                .and(LocationSpecifications.hasParent(parentId));

        return locationRepository.findAll(spec).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }
}
