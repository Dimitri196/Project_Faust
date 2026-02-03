package com.projectfaust.service;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.Occupation;
import com.projectfaust.mapper.OccupationMapper;
import com.projectfaust.repository.InstitutionRepository;
import com.projectfaust.repository.OccupationRepository;
import com.projectfaust.validator.HierarchyValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OccupationService {

    private final OccupationRepository occupationRepository;
    private final InstitutionRepository institutionRepository;
    private final OccupationMapper mapper;
    private final HierarchyValidator hierarchyValidator;

    @Transactional
    public OccupationResponse create(OccupationRequest request) {
        log.info("Creating occupation: {} (Code: {})", request.title(), request.code());

        Occupation entity = mapper.toEntity(request);

        // 1. Resolve Institution
        Institution institution = institutionRepository.findByExternalId(request.institutionPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));
        entity.setInstitution(institution);

        // 2. Resolve Supervisor if provided
        if (request.reportsToPublicId() != null) {
            Occupation supervisor = occupationRepository.findByExternalId(request.reportsToPublicId())
                    .orElseThrow(() -> new EntityNotFoundException("Supervisor position not found"));

            // Validate hierarchy
            hierarchyValidator.verifyNoCircularReference(entity, supervisor);
            entity.setReportsTo(supervisor);
        }

        return mapper.toResponse(occupationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<OccupationResponse> getByInstitution(UUID institutionPublicId) {
        return mapper.toResponseList(occupationRepository.findByInstitutionExternalId(institutionPublicId));
    }

    @Transactional(readOnly = true)
    public List<OccupationTreeResponse> getCommandChain() {
        log.info("Fetching full reporting hierarchy (Command Chain)");
        List<Occupation> roots = occupationRepository.findTopLevelRoles();
        return mapper.toTreeResponseList(roots);
    }

    @Transactional
    public List<OccupationResponse> createBulk(List<OccupationRequest> requests) {
        log.info("Bulk creating {} occupations", requests.size());

        return requests.stream()
                .map(this::create)
                .collect(Collectors.toList());
    }
}