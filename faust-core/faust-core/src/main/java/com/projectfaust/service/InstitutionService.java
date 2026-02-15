package com.projectfaust.service;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.mapper.InstitutionMapper;
import com.projectfaust.repository.InstitutionRepository;
import com.projectfaust.specification.InstitutionSpecifications;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        log.info("Creating institution: {}", request.name());
        Institution entity = mapper.toEntity(request);

        if (request.parentExternalId() != null) {
            Institution parent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent institution not found"));

            hierarchyValidator.verifyNoCircularReference(entity, parent);
            entity.setParent(parent);
        }

        return mapper.toResponse(repository.save(entity));
    }

    @Transactional
    public List<InstitutionResponse> createBulk(List<InstitutionRequest> requests) {
        log.info("Bulk creating {} institutions", requests.size());

        // Using a loop to ensure individual validation and parent linking
        List<Institution> entities = new ArrayList<>();
        for (InstitutionRequest request : requests) {
            Institution entity = mapper.toEntity(request);
            if (request.parentExternalId() != null) {
                repository.findByExternalId(request.parentExternalId())
                        .ifPresent(entity::setParent);
            }
            entities.add(entity);
        }

        return repository.saveAll(entities).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> search(String name, String country, Boolean isStateOwned, UUID locationId) {
        Specification<Institution> spec = Specification
                .where(InstitutionSpecifications.nameContains(name))
                .and(InstitutionSpecifications.hasCountry(country))
                .and(InstitutionSpecifications.isStateOwned(isStateOwned))
                .and(InstitutionSpecifications.hasLocation(locationId)); // Requires this spec method

        return repository.findAll(spec).stream()
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .toList();
    }

    private InstitutionResponse enrich(InstitutionResponse dto, Institution entity) {
        if (entity.getLocation() == null) return dto;

        var path = locationService.getLocationPath(entity.getLocation().getExternalId());

        return new InstitutionResponse(
                dto.publicId(),
                dto.name(),
                dto.countryCode(),
                dto.level(),
                dto.type(),
                dto.parentId(),
                dto.hasChildren(),
                dto.isStateOwned(),
                dto.description(),
                entity.getLocation().getExternalId(),
                entity.getLocation().getName(),
                path,
                dto.logoUrl(),
                dto.websiteUrl()
        );
    }

    @Transactional(readOnly = true)
    public InstitutionResponse getByPublicId(UUID publicId) {
        return repository.findByExternalId(publicId)
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .orElseThrow(() -> new EntityNotFoundException("Institution not found: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getFullTree() {
        log.info("Fetching complete institutional hierarchy tree");
        List<Institution> roots = repository.findAllRoots();
        return mapper.toTreeResponseList(roots);
    }

    @Transactional
    public InstitutionResponse update(UUID publicId, InstitutionRequest request) {
        log.info("Updating institution: {}", publicId);
        Institution entity = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));

        if (request.parentExternalId() != null) {
            Institution newParent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("New parent not found"));

            hierarchyValidator.verifyNoCircularReference(entity, newParent);
            entity.setParent(newParent);
        } else {
            entity.setParent(null);
        }

        mapper.updateEntityFromRequest(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public InstitutionAscendedResponse getAscendedPath(UUID publicId) {
        Institution leaf = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));

        return mapper.toAscendedResponse(leaf);
    }

    @Transactional(readOnly = true)
    public InstitutionTreeResponse getSubTree(UUID publicId) {
        log.info("Fetching sub-tree starting from: {}", publicId);

        Institution middleNode = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));

        return mapper.toTreeResponse(middleNode);}

}
