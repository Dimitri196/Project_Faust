package com.projectfaust.service;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.*;
import com.projectfaust.entity.enums.ContactType;
import com.projectfaust.entity.enums.NameType;
import com.projectfaust.mapper.PersonMapper;
import com.projectfaust.repository.BankAccountRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service orchestrating human identity dossiers and their technical footprint telemetry.
 * Leverages high-fidelity intelligence normalization to cross-reference transient contact vectors.
 * * @author Dimitri
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;
    private final BankAccountRepository bankAccountRepository;

    @Transactional
    public PersonResponse create(PersonRequest request) {
        log.info("Ingesting target profile with primary identity: {} {}", request.firstName(), request.lastName());

        Person person = mapper.toEntity(request);

        syncNameHistory(person, request);
        syncContactVectors(person, request);
        syncFinancialVectors(person, request); // 👈 PŘIDÁNO

        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public PersonResponse getByPublicId(UUID publicId) {
        Person person = repository.findFullProfileByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Intelligence profile not found with external trace: " + publicId));

        if (person.getNames() != null) person.getNames().size();
        if (person.getContacts() != null) person.getContacts().size();
        if (person.getFinancialAccounts() != null) person.getFinancialAccounts().size(); // 👈 Inicializace

        return mapper.toResponse(person);
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    @Transactional
    public PersonResponse update(UUID publicId, PersonRequest request) {
        log.info("Executing comprehensive update on target footprint profile ID: {}", publicId);

        Person person = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Target identity matrix not found"));

        mapper.updateEntityFromRequest(request, person);

        syncNameHistory(person, request);
        syncContactVectors(person, request);
        syncFinancialVectors(person, request); // 👈 PŘIDÁNO

        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> searchByName(String query) {
        if (query == null || query.isBlank()) return List.of();
        return mapper.toResponseList(repository.searchByFullName(query.trim().toLowerCase()));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> searchByAnyIdentity(String query) {
        if (query == null || query.isBlank()) return List.of();
        return mapper.toResponseList(repository.searchByAnyName(query.trim().toLowerCase()));
    }

    @Transactional
    public List<PersonResponse> createBulk(List<PersonRequest> requests) {
        log.info("Commencing high-throughput bulk intelligence ingestion: {} target tracks", requests.size());
        List<Person> entitiesToSave = new ArrayList<>();

        for (PersonRequest req : requests) {
            Person entity = mapper.toEntity(req);
            syncNameHistory(entity, req);
            syncContactVectors(entity, req);
            syncFinancialVectors(entity, req); // 👈 PŘIDÁNO
            entitiesToSave.add(entity);
        }

        return mapper.toResponseList(repository.saveAll(entitiesToSave));
    }
    // --- Intelligence Footprint & Identity Synchronization Engine ---

    /**
     * Normalizes and aggregates incoming metadata parameters into dedicated technical vectors.
     * Prevents unique key violations while building shared interception leads.
     */
    private void syncContactVectors(Person person, PersonRequest request) {
        // Oprava: Inicializujeme jako LinkedHashSet kvůli změně typu v entitě Person
        if (person.getContacts() == null) {
            person.setContacts(new LinkedHashSet<>());
        } else {
            person.getContacts().clear();
        }

        if (request.contacts() == null || request.contacts().isEmpty()) {
            return;
        }

        // Catch internal request duplicates to prevent flushing invalid SQL batches
        Set<String> uniqueLookupsInRequest = new HashSet<>();

        for (PersonRequest.ContactRequest contactReq : request.contacts()) {
            String normalizedValue = normalizeContactValue(contactReq.contactType(), contactReq.contactValueRaw());
            String duplicationKey = contactReq.contactType().name() + "::" + normalizedValue;

            if (!uniqueLookupsInRequest.add(duplicationKey)) {
                log.warn("Omitting internal request duplication for signature vector: {}", duplicationKey);
                continue; // Skip layout redundancy inside the same request payload
            }

            person.getContacts().add(PersonContact.builder()
                    .person(person)
                    .contactType(contactReq.contactType())
                    .contactValueRaw(contactReq.contactValueRaw().trim())
                    .contactValueNormalized(normalizedValue)
                    .operatorName(contactReq.operatorName() != null ? contactReq.operatorName().trim() : null)
                    .imei(contactReq.imei() != null ? contactReq.imei().replaceAll("\\s+", "") : null)
                    .verificationStatus(contactReq.verificationStatus())
                    .confidenceScore(contactReq.confidenceScore() != null ? contactReq.confidenceScore() : 1.0)
                    .clearanceLevel(contactReq.clearanceLevel())
                    .active(contactReq.isActive())
                    .validFrom(contactReq.validFrom())
                    .validTo(contactReq.validTo())
                    .analyticalNote(contactReq.analyticalNote())
                    .build());
        }
    }

    // --- FININT Synchronization Engine ---

    private void syncFinancialVectors(Person person, PersonRequest request) {
        if (person.getFinancialAccounts() == null) {
            person.setFinancialAccounts(new LinkedHashSet<>());
        } else {
            person.getFinancialAccounts().clear();
        }

        if (request.financialAccounts() == null || request.financialAccounts().isEmpty()) {
            return;
        }

        for (var finReq : request.financialAccounts()) {
            BankAccount account;

            if (finReq.bankAccountExternalId() != null) {
                // Připojení existujícího globálního uzlu
                account = bankAccountRepository.findByExternalId(finReq.bankAccountExternalId())
                        .orElseThrow(() -> new EntityNotFoundException("Global BankAccount not found: " + finReq.bankAccountExternalId()));
            } else if (finReq.newBankAccountData() != null) {
                // Detekce duplicity podle IBANu
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

            person.getFinancialAccounts().add(PersonAccountRelation.builder()
                    .person(person)
                    .bankAccount(account)
                    .roleType(finReq.roleType())
                    .validFrom(finReq.validFrom())
                    .validTo(finReq.validTo())
                    .active(finReq.isActive())
                    .build());
        }
    }

    private void syncNameHistory(Person person, PersonRequest request) {
        // Oprava: Inicializujeme jako LinkedHashSet kvůli změně typu v entitě Person
        if (person.getNames() == null) {
            person.setNames(new LinkedHashSet<>());
        } else {
            person.getNames().clear();
        }

        addPrimaryName(person, request);

        if (request.additionalNames() != null) {
            request.additionalNames().forEach(nameReq ->
                    person.getNames().add(PersonName.builder()
                            .person(person)
                            .firstName(nameReq.firstName().trim())
                            .lastName(nameReq.lastName().trim())
                            .type(nameReq.type() != null ? nameReq.type() : NameType.LEGAL)
                            .validFrom(nameReq.validFrom())
                            .validTo(nameReq.validTo())
                            .note(nameReq.note())
                            .isPrimary(false)
                            .build())
            );
        }
    }

    private void addPrimaryName(Person person, PersonRequest request) {
        person.getNames().add(PersonName.builder()
                .person(person)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .type(request.primaryNameType() != null ? request.primaryNameType() : NameType.LEGAL)
                .isPrimary(true)
                .build());
    }

    /**
     * Deterministic normalization engine applying protocol-specific parsing constraints.
     * Guarantees exact matches across varied external SIGINT ingestion feeds.
     */
    private String normalizeContactValue(ContactType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Raw communication identifier node cannot be empty");
        }

        String clean = rawValue.trim();

        if (type == ContactType.EMAIL) {
            return clean.toLowerCase();
        }

        if (type == ContactType.CELLULAR_GSM) {
            // E.164 compliance standard approximation: strip formatting, keep cross-border tokens
            return clean.replaceAll("[^0-9+]", "");
        }

        // Decentralized IDs, Crypto addresses, and IM tags maintain raw case-sensitivity distribution
        return clean;
    }
}