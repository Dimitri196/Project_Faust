package com.projectfaust.service;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.mapper.InstitutionMapper;
import com.projectfaust.repository.InstitutionRepository;
import com.projectfaust.repository.LocationRepository;
import com.projectfaust.specification.InstitutionSpecifications;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final InstitutionRepository repository;
    private final InstitutionMapper mapper;
    private final HierarchyValidator hierarchyValidator;
    private final LocationService locationService;
    private final LocationRepository locationRepository;

    /**
     * Vytvoří jednu instituci s validací hierarchie a bezpečnosti.
     */
    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        log.info("FAUST_INST: Creating institution: {}", request.name());
        Institution entity = prepareEntity(request);
        return enrich(mapper.toResponse(repository.save(entity)), entity);
    }

    /**
     * Bulk vytvoření. Ideální pro hromadný import úřadů.
     */
    @Transactional
    public List<InstitutionResponse> createBulk(List<InstitutionRequest> requests) {
        log.info("FAUST_INST_BULK: Processing {} requests", requests.size());

        return requests.stream()
                .map(this::create) // Voláme interní create pro zajištění validací u každého kusu
                .toList();
    }

    /**
     * Pomocná metoda pro přípravu entity (DRY - Don't Repeat Yourself).
     */
    private Institution prepareEntity(InstitutionRequest request) {
        Institution entity = mapper.toEntity(request);

        // Lokace
        if (request.locationId() != null) {
            entity.setLocation(locationRepository.findByExternalId(request.locationId())
                    .orElseThrow(() -> new EntityNotFoundException("Location node not found")));
        }

        // Hierarchie
        if (request.parentExternalId() != null) {
            Institution parent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent institution not found"));

            hierarchyValidator.verifyNoCircularReference(entity, parent);
            entity.setParent(parent);

            // Dědičnost bezpečnosti
            if (entity.getClearanceLevel().getWeight() < parent.getClearanceLevel().getWeight()) {
                entity.setClearanceLevel(parent.getClearanceLevel());
            }
        }
        return entity;
    }

    // --- Navigační metody pro Controller ---

    @Transactional(readOnly = true)
    public List<InstitutionResponse> getImmediateChildren(UUID parentPublicId) {
        log.info("FAUST_QUERY: Fetching children for parent: {}", parentPublicId);
        return repository.findByParent_ExternalId(parentPublicId).stream()
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getFullTree() {
        log.info("FAUST_QUERY: Fetching complete hierarchy tree");
        return repository.findAllRoots().stream()
                .map(mapper::toTreeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionTreeResponse getSubTree(UUID publicId) {
        Institution node = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));
        return mapper.toTreeResponse(node);
    }

    @Transactional(readOnly = true)
    public InstitutionAscendedResponse getAscendedPath(UUID publicId) {
        Institution leaf = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));
        return mapper.toAscendedResponse(leaf);
    }

    @Transactional(readOnly = true)
    public InstitutionResponse getByPublicId(UUID publicId) {
        return repository.findByExternalId(publicId)
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .orElseThrow(() -> new EntityNotFoundException("Institution not found: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> search(String name, String country, Boolean isStateOwned, UUID locationId, UUID parentId) {
        // Zde využíváme vylepšené specifikace, které už umí JOIN na lokaci pro countryCode
        Specification<Institution> spec = Specification
                .where(InstitutionSpecifications.activeOnly())
                .and(InstitutionSpecifications.nameContains(name))
                .and(InstitutionSpecifications.hasCountry(country))
                .and(InstitutionSpecifications.isStateOwned(isStateOwned))
                .and(InstitutionSpecifications.hasLocation(locationId))
                .and(InstitutionSpecifications.hasParent(parentId));

        return repository.findAll(spec).stream()
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .toList();
    }

    /**
     * Obohacení DTO o geografická data a breadcrumbs.
     */
    private InstitutionResponse enrich(InstitutionResponse dto, Institution entity) {
        List<LocationResponse> path = (entity.getLocation() != null)
                ? locationService.getLocationPath(entity.getLocation().getExternalId())
                : Collections.emptyList();

        return new InstitutionResponse(
                dto.publicId(),
                dto.name(),
                dto.level(),
                dto.type(),
                entity.getClearanceLevel(),
                dto.parentId(),
                !entity.getChildren().isEmpty(),
                dto.isStateOwned(),
                entity.isActive(),
                dto.description(),
                entity.getLocation() != null ? entity.getLocation().getExternalId() : null,
                entity.getLocation() != null ? entity.getLocation().getName() : null,
                path,
                dto.logoUrl(),
                dto.websiteUrl()
        );
    }
}