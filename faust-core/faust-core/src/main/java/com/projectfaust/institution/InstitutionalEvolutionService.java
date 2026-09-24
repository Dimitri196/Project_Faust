package com.projectfaust.institution;

import com.projectfaust.institution.dto.InstitutionalEvolutionRequest;
import com.projectfaust.institution.dto.InstitutionalEvolutionResponse;
import com.projectfaust.shared.enums.InstitutionalEvolutionType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InstitutionalEvolutionService {

    private final InstitutionalEvolutionRepository repository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionalEvolutionMapper mapper;

    /**
     * Vytvoří záznam o evoluci/nástupnictví.
     * Automaticky řeší stav 'active' u předchůdce.
     */
    @Transactional
    public InstitutionalEvolutionResponse create(InstitutionalEvolutionRequest request) {
        log.info("FAUST_EVO: Executing {} for predecessor {}",
                request.evolutionType(), request.predecessorExternalId());

        Institution predecessor = institutionRepository.findByExternalId(request.predecessorExternalId())
                .orElseThrow(() -> new EntityNotFoundException("Predecessor institution not found"));

        InstitutionalEvolution entity = mapper.toEntity(request);
        entity.setPredecessor(predecessor);

        // Successor může být null u TOTAL_DISSOLUTION
        if (request.successorExternalId() != null) {
            Institution successor = institutionRepository.findByExternalId(request.successorExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("Successor institution not found"));
            entity.setSuccessor(successor);
        }

        // --- LOGIKA AKTIVITY ---
        // Pokud instituce zaniká, dělí se nebo je pohlcena, původní uzel už není aktivní.
        if (shouldDeactivatePredecessor(request.evolutionType())) {
            log.info("FAUST_EVO: Deactivating predecessor institution: {}", predecessor.getName());
            predecessor.setActive(false);
            institutionRepository.save(predecessor);
        }

        InstitutionalEvolution saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    /**
     * Vrací kompletní časovou osu (všechny transformace spojené s daným UUID).
     */
    @Transactional(readOnly = true)
    public List<InstitutionalEvolutionResponse> getTimeline(UUID institutionId) {
        log.debug("FAUST_QUERY: Resolving evolution timeline for institution: {}", institutionId);
        List<InstitutionalEvolution> evolutions = repository.findAllByInstitutionExternalId(institutionId);
        return mapper.toResponseList(evolutions);
    }

    /**
     * Bulk operace pro historické migrace (např. import struktur z roku 1993).
     */
    @Transactional
    public List<InstitutionalEvolutionResponse> createBulk(List<InstitutionalEvolutionRequest> requests) {
        log.info("FAUST_EVO_BULK: Processing {} evolution links", requests.size());
        return requests.stream().map(this::create).toList();
    }

    /**
     * Pomocná metoda pro rozhodování o stavu entity.
     */
    private boolean shouldDeactivatePredecessor(InstitutionalEvolutionType type) {
        return switch (type) {
            case ORGANIZATIONAL_SPLIT_OFF,
                 INSTITUTIONAL_MERGER,
                 COMPETENCE_ABSORPTION,
                 TOTAL_DISSOLUTION,
                 SUCCESSION_BY_PARTITION -> true;
            case CONTINUITY_REORGANIZATION -> false;
        };
    }
}