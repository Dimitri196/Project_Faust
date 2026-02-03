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

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        log.info("Creating institution: {}", request.name());
        Institution entity = mapper.toEntity(request);

        if (request.parentExternalId() != null) {
            Institution parent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent institution not found"));

            // Theoretically impossible to have a cycle on a new entity unless parent is itself,
            // but the validator handles that "self-parent" case too.
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
    public List<InstitutionResponse> search(String name, String country, Boolean isStateOwned) {
        // Chain specifications dynamically
        Specification<Institution> spec = Specification
                .where(InstitutionSpecifications.nameContains(name))
                .and(InstitutionSpecifications.hasCountry(country))
                .and(InstitutionSpecifications.isStateOwned(isStateOwned));

        return repository.findAll(spec).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionResponse getByPublicId(UUID publicId) {
        return repository.findByExternalId(publicId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Institution not found with ID: " + publicId));
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

            // THE GUARDRAIL
            hierarchyValidator.verifyNoCircularReference(entity, newParent);
            entity.setParent(newParent);
        } else {
            entity.setParent(null); // Allow making an institution a root node
        }

        mapper.updateEntityFromRequest(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    // 2. Směr NAHORU: Najdeme konkrétní list (agenta) a vyjdeme k rodičům
    @Transactional(readOnly = true)
    public InstitutionAscendedResponse getAscendedPath(UUID publicId) {
        Institution leaf = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));

        // MapStruct automaticky projde parent -> parent -> parent až k null
        return mapper.toAscendedResponse(leaf);
    }

    @Transactional(readOnly = true)
    public InstitutionTreeResponse getSubTree(UUID publicId) {
        log.info("Fetching sub-tree starting from: {}", publicId);

        // 1. Find the middle-level institution
        Institution middleNode = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));

        // 2. Map it – MapStruct will recursively map all its children
        return mapper.toTreeResponse(middleNode);}

}
