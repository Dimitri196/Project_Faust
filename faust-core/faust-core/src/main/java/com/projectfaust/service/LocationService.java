package com.projectfaust.service;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import com.projectfaust.mapper.LocationMapper;
import com.projectfaust.repository.LocationRepository;
import com.projectfaust.specification.LocationSpecifications;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;
    private final HierarchyValidator hierarchyValidator;

    /**
     * Vytvoří novou lokalizační entitu s kontrolou hierarchické integrity.
     */
    @Transactional
    public LocationResponse createLocation(LocationRequest request) {
        Location location = locationMapper.toEntity(request);

        if (request.parentExternalId() != null) {
            Location parent = locationRepository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "FAILED_TO_LOCATE_PARENT_NODE: " + request.parentExternalId()));

            // Využití tvého generického validátoru pro zamezení cyklů
            hierarchyValidator.verifyNoCircularReference(location, parent);
            location.setParent(parent);
        }

        Location savedLocation = locationRepository.save(location);
        return locationMapper.toResponse(savedLocation);
    }

    /**
     * Získá kořenové uzly (např. země), které nemají žádného rodiče.
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getRootLocations() {
        return locationRepository.findAllByParentIsNull().stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Realizuje drill-down navigaci skrze externalId rodiče.
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> getSubLocations(UUID parentExternalId) {
        // Nejprve ověříme existenci rodiče pro čistší chybové hlášky
        if (!locationRepository.findByExternalId(parentExternalId).isPresent()) {
            throw new EntityNotFoundException("LOCATION_NODE_NOT_FOUND: " + parentExternalId);
        }

        return locationRepository.findAllByParentExternalId(parentExternalId).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationPath(UUID externalId) {
        List<Location> path = new ArrayList<>();
        Location current = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("LOCATION_NOT_FOUND"));

        // Rekurzivní stoupání nahoru k rootu
        while (current != null) {
            path.add(current);
            current = current.getParent();
        }

        // Obrátíme, aby cesta začínala zemí a končila hledaným bodem
        Collections.reverse(path);

        return path.stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Vyhledá konkrétní detail lokace podle UUID.
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocationByExternalId(UUID externalId) {
        return locationRepository.findByExternalId(externalId)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("DATA_QUERY_FAILURE: " + externalId));
    }

    /**
     * Aktualizuje hierarchické zařazení existující lokace.
     */
    @Transactional
    public LocationResponse updateParent(UUID locationId, UUID newParentId) {
        Location location = locationRepository.findByExternalId(locationId)
                .orElseThrow(() -> new EntityNotFoundException("TARGET_NODE_NOT_FOUND"));

        Location newParent = locationRepository.findByExternalId(newParentId)
                .orElseThrow(() -> new EntityNotFoundException("PROPOSED_PARENT_NODE_NOT_FOUND"));

        // Bezpečnostní kontrola proti zacyklení
        hierarchyValidator.verifyNoCircularReference(location, newParent);

        location.setParent(newParent);
        return locationMapper.toResponse(locationRepository.save(location));
    }

    /**
     * Odstraní lokaci ze systému.
     * POZOR: JPA CascadeType.ALL v entitě zajistí smazání všech sub-lokací.
     */
    @Transactional
    public void deleteLocation(UUID externalId) {
        Location location = locationRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("TERMINATION_FAILED: NODE_NOT_EXISTENT"));
        locationRepository.delete(location);
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> search(String name, String type, UUID parentId) {
        Specification<Location> spec = Specification
                .where(LocationSpecifications.nameContains(name))
                .and(LocationSpecifications.hasType(type))
                .and(LocationSpecifications.hasParent(parentId));

        return locationRepository.findAll(spec).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }
}
