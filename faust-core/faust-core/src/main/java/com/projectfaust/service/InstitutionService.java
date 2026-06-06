package com.projectfaust.service;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.BankAccount;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.InstitutionAccountRelation;
import com.projectfaust.mapper.InstitutionMapper;
import com.projectfaust.repository.BankAccountRepository;
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
import java.util.LinkedHashSet;
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
    private final BankAccountRepository bankAccountRepository; // 👈 PŘIDÁNO

    // --- WRITE OPERATIONS ---

    @Transactional
    public InstitutionResponse create(InstitutionRequest request) {
        log.info("FAUST_INST: Creating institution: {}", request.name());
        Institution entity = prepareEntity(request);

        // Synchronizace finančních účtů před uložením
        syncFinancialVectors(entity, request);

        return enrich(mapper.toResponse(repository.save(entity)), entity);
    }

    @Transactional
    public InstitutionResponse update(UUID publicId, InstitutionRequest request) {
        log.info("FAUST_INST: Updating institution: {}", publicId);
        Institution entity = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found: " + publicId));

        // Aktualizace plochých vlastností (případně přes tvůj aktualizační mapper)
        entity.setName(request.name());
        entity.setLevel(request.level());
        entity.setType(request.type());
        entity.setClearanceLevel(request.clearanceLevel());
        entity.setStateOwned(request.isStateOwned());
        entity.setActive(request.active());
        entity.setDescription(request.description());
        entity.setLogoUrl(request.logoUrl());
        entity.setWebsiteUrl(request.websiteUrl());

        if (request.locationId() != null) {
            entity.setLocation(locationRepository.findByExternalId(request.locationId())
                    .orElseThrow(() -> new EntityNotFoundException("Location node not found")));
        }

        // Aktualizace hierarchie a financí
        syncFinancialVectors(entity, request);

        return enrich(mapper.toResponse(repository.save(entity)), entity);
    }

    @Transactional
    public List<InstitutionResponse> createBulk(List<InstitutionRequest> requests) {
        log.info("FAUST_INST_BULK: Processing {} requests", requests.size());
        return requests.stream().map(this::create).toList();
    }

    private Institution prepareEntity(InstitutionRequest request) {
        Institution entity = mapper.toEntity(request);

        if (request.locationId() != null) {
            entity.setLocation(locationRepository.findByExternalId(request.locationId())
                    .orElseThrow(() -> new EntityNotFoundException("Location node not found")));
        }

        if (request.parentExternalId() != null) {
            Institution parent = repository.findByExternalId(request.parentExternalId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent institution not found"));

            hierarchyValidator.verifyNoCircularReference(entity, parent);
            entity.setParent(parent);

            if (entity.getClearanceLevel().getWeight() < parent.getClearanceLevel().getWeight()) {
                entity.setClearanceLevel(parent.getClearanceLevel());
            }
        }
        return entity;
    }

    // --- FININT SYNCHRONIZATION ENGINE ---

    private void syncFinancialVectors(Institution institution, InstitutionRequest request) {
        if (institution.getFinancialAccounts() == null) {
            institution.setFinancialAccounts(new LinkedHashSet<>());
        } else {
            institution.getFinancialAccounts().clear();
        }

        if (request.financialAccounts() == null || request.financialAccounts().isEmpty()) {
            return;
        }

        for (var finReq : request.financialAccounts()) {
            BankAccount account;

            if (finReq.bankAccountExternalId() != null) {
                // Připojení stávajícího globálního účtu
                account = bankAccountRepository.findByExternalId(finReq.bankAccountExternalId())
                        .orElseThrow(() -> new EntityNotFoundException("Global BankAccount not found: " + finReq.bankAccountExternalId()));
            } else if (finReq.newBankAccountData() != null) {
                // Prevence duplicity: podíváme se, jestli už IBAN v systému náhodou není
                String incomingIban = finReq.newBankAccountData().iban().trim().replaceAll("\\s+", "");
                account = bankAccountRepository.findByIban(incomingIban)
                        .orElseGet(() -> bankAccountRepository.save(BankAccount.builder()
                                .iban(incomingIban)
                                .bic(finReq.newBankAccountData().bic() != null ? finReq.newBankAccountData().bic().trim() : null)
                                .bankName(finReq.newBankAccountData().bankName().trim())
                                .currency(finReq.newBankAccountData().currency().toUpperCase().trim())
                                .isMonitored(finReq.newBankAccountData().isMonitored())
                                .analyticalNote(finReq.newBankAccountData().analyticalNote())
                                .build()));
            } else {
                continue;
            }

            institution.getFinancialAccounts().add(InstitutionAccountRelation.builder()
                    .institution(institution)
                    .bankAccount(account)
                    .roleType(finReq.roleType())
                    .validFrom(finReq.validFrom())
                    .validTo(finReq.validTo())
                    .active(finReq.isActive())
                    .build());
        }
    }

    // --- NAVIGATION & TREE OPERATIONS ---

    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getImmediateChildren(UUID parentPublicId) {
        log.info("FAUST_QUERY: Fetching immediate children for parent: {}", parentPublicId);
        return repository.findByParent_ExternalId(parentPublicId).stream()
                .map(mapper::toTreeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionTreeResponse> getRootNodes() {
        log.info("FAUST_QUERY: Fetching top-level roots");
        return repository.findAllRoots().stream()
                .map(mapper::toTreeResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InstitutionTreeResponse getSubTree(UUID publicId) {
        log.info("FAUST_NEXUS: Decrypting deep hierarchy for node: {}", publicId);
        Institution node = repository.findWithDeepHierarchyByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found"));
        return mapper.toTreeResponse(node);
    }

    // --- DETAIL & SEARCH OPERATIONS ---

    @Transactional(readOnly = true)
    public InstitutionResponse getByPublicId(UUID publicId) {
        // Použijeme optimalizovaný hluboký fetch z repository
        return repository.findFullProfileByExternalId(publicId)
                .map(entity -> enrich(mapper.toResponse(entity), entity))
                .orElseThrow(() -> new EntityNotFoundException("Institution not found: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<InstitutionResponse> search(String name, String country, Boolean isStateOwned, UUID locationId, UUID parentId) {
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
     * OPRAVENO: Obohacení zohledňuje novou finanční komponentu z MapStruct DTO (16. parametr)
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
                entity.getParent() != null ? entity.getParent().getExternalId() : null,
                !entity.getChildren().isEmpty(),
                dto.isStateOwned(),
                entity.isActive(),
                dto.description(),
                entity.getLocation() != null ? entity.getLocation().getExternalId() : null,
                entity.getLocation() != null ? entity.getLocation().getName() : null,
                path,
                dto.logoUrl(),
                dto.websiteUrl(),
                dto.financialAccounts() // 👈 16. ARGUMENT DOPLNĚN PRO KOMPILACI
        );
    }

    @Transactional(readOnly = true)
    public InstitutionAscendedResponse getAscendedPath(UUID publicId) {
        log.info("FAUST_QUERY: Resolving ascended path for node: {}", publicId);
        Institution leaf = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Institution not found: " + publicId));
        return mapper.toAscendedResponse(leaf);
    }

    @Transactional(readOnly = true)
    public InstitutionTreeResponse getNexusFocus(UUID publicId) {
        Institution target = repository.findWithDeepHierarchyByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException());

        Institution root = target;
        while (root.getParent() != null) {
            root = root.getParent();
        }
        return mapper.toTreeResponse(root);
    }
}
