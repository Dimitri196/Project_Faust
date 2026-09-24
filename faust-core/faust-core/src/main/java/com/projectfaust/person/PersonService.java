package com.projectfaust.person;

import com.projectfaust.appointment.AppointmentMapper;
import com.projectfaust.appointment.AppointmentRepository;
import com.projectfaust.financial.BankAccount;
import com.projectfaust.financial.BankAccountRepository;
import com.projectfaust.financial.PersonAccountRelation;
import com.projectfaust.person.contact.PersonContact;
import com.projectfaust.person.dto.PersonRequest;
import com.projectfaust.person.dto.PersonResponse;
import com.projectfaust.shared.enums.ContactType;
import com.projectfaust.shared.enums.NameType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * Core service for managing person profiles and their intelligence footprint within Project Faust.
 *
 * <p>Orchestrates three synchronisation engines on every write operation:</p>
 * <ul>
 *   <li><b>Name history sync</b> — builds the primary name record and all aliases from the request.</li>
 *   <li><b>Contact vector sync</b> — normalises, deduplicates, and persists contact channels
 *       with SIGINT metadata (operator, IMEI, confidence score).</li>
 *   <li><b>FININT vector sync</b> — attaches existing or creates new bank accounts with
 *       IBAN deduplication to prevent duplicate account records.</li>
 * </ul>
 *
 * <p>Read operations use {@link PersonRepository#findFullProfileByExternalId} which
 * eager-loads all collections in a single query. The response is then enriched with
 * current active positions via the appointment layer.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;
    private final BankAccountRepository bankAccountRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new person profile and synchronises all intelligence collections.
     *
     * @param request the creation DTO.
     * @return the persisted person as an enriched {@link PersonResponse}.
     */
    @Transactional
    public PersonResponse create(PersonRequest request) {
        log.info("FAUST_PERSON: Ingesting profile — {} {}",
                request.firstName(), request.lastName());

        Person person = mapper.toEntity(request);
        syncNameHistory(person, request);
        syncContactVectors(person, request);
        syncFinancialVectors(person, request);

        return toEnrichedResponse(repository.save(person));
    }

    /**
     * Updates an existing person's scalar fields and resynchronises all intelligence collections.
     *
     * @param publicId the public UUID of the person to update.
     * @param request  the update DTO.
     * @return the updated person as an enriched {@link PersonResponse}.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional
    public PersonResponse update(UUID publicId, PersonRequest request) {
        log.info("FAUST_PERSON: Updating profile {}.", publicId);

        Person person = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("PERSON_NOT_FOUND: " + publicId));

        mapper.updateEntityFromRequest(request, person);
        syncNameHistory(person, request);
        syncContactVectors(person, request);
        syncFinancialVectors(person, request);

        return toEnrichedResponse(repository.save(person));
    }

    /**
     * Creates multiple person profiles within a single database transaction.
     *
     * @param requests list of creation requests.
     * @return list of persisted persons as enriched responses.
     */
    @Transactional
    public List<PersonResponse> createBulk(List<PersonRequest> requests) {
        log.info("FAUST_PERSON_BULK: Processing {} person profiles.", requests.size());

        List<Person> toSave = new ArrayList<>();
        for (PersonRequest req : requests) {
            Person entity = mapper.toEntity(req);
            syncNameHistory(entity, req);
            syncContactVectors(entity, req);
            syncFinancialVectors(entity, req);
            toSave.add(entity);
        }

        return repository.saveAll(toSave).stream()
                .map(this::toEnrichedResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single person by public UUID with full dossier eagerly loaded.
     *
     * @param publicId the public UUID of the person.
     * @return the fully-loaded person as an enriched {@link PersonResponse}.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public PersonResponse getByPublicId(UUID publicId) {
        Person person = repository.findFullProfileByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("PERSON_NOT_FOUND: " + publicId));
        return toEnrichedResponse(person);
    }

    /**
     * Returns a paginated list of all persons.
     *
     * @param pageable pagination and sorting parameters.
     * @return a page of person responses.
     */
    @Transactional(readOnly = true)
    public Page<PersonResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(this::toEnrichedResponse);
    }

    /**
     * Searches persons by their normalised primary name using the DB-generated search column.
     *
     * @param query the search term.
     * @return list of matching persons.
     */
    @Transactional(readOnly = true)
    public List<PersonResponse> searchByName(String query) {
        if (query == null || query.isBlank()) return List.of();
        return mapper.toResponseList(
                repository.searchByFullName(query.trim().toLowerCase()));
    }

    /**
     * Deep alias search across the full name history — including aliases and cover names.
     *
     * @param query the search term.
     * @return list of matching persons.
     */
    @Transactional(readOnly = true)
    public List<PersonResponse> searchByAnyIdentity(String query) {
        if (query == null || query.isBlank()) return List.of();
        return mapper.toResponseList(
                repository.searchByAnyName(query.trim().toLowerCase()));
    }

    /**
     * Finds all persons linked to a specific device by IMEI.
     *
     * @param imei the IMEI of the target device.
     * @return list of persons whose contact records include the given IMEI.
     */
    @Transactional(readOnly = true)
    public List<PersonResponse> findByImei(String imei) {
        log.info("FAUST_SIGINT: IMEI lookup — {}.", imei);
        return mapper.toResponseList(repository.findAllByHardwareImei(imei));
    }

    /**
     * Finds all persons linked to a specific bank account by IBAN.
     *
     * @param iban the IBAN of the target account.
     * @return list of persons with a financial relationship to the given IBAN.
     */
    @Transactional(readOnly = true)
    public List<PersonResponse> findByIban(String iban) {
        log.info("FAUST_FININT: IBAN cross-link lookup — {}.", iban);
        return mapper.toResponseList(repository.findAllByLinkedIban(iban));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link Person} entity to an enriched {@link PersonResponse}.
     *
     * <p>Resolves current active appointments and injects them into the response
     * as {@code currentPositions}. The mapper ignores this field — the service
     * is responsible for enrichment.</p>
     *
     * @param person the person entity.
     * @return an enriched response with current positions resolved.
     */
    private PersonResponse toEnrichedResponse(Person person) {
        PersonResponse base = mapper.toResponse(person);
        var currentPositions = appointmentRepository
                .findActiveByPersonExternalId(person.getExternalId(), LocalDate.now())
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();

        return new PersonResponse(
                base.publicId(),
                base.firstName(),
                base.lastName(),
                base.displayName(),
                base.titleBefore(),
                base.titleAfter(),
                base.nameHistory(),
                base.contactHistory(),
                base.financialAccounts(),
                currentPositions,
                base.primaryEmail(),
                base.primaryPhone(),
                base.age(),
                base.educationLevel(),
                base.fieldOfStudy(),
                base.biography(),
                base.photoUrl(),
                base.politicalAffiliation(),
                base.birthDate(),
                base.gender(),
                base.nationality(),
                base.placeOfBirth(),
                base.deathDate(),
                base.clearanceLevel(),
                base.verificationStatus(),
                base.createdAt(),
                base.updatedAt()
        );
    }

    /**
     * Builds the primary name record and all additional alias records from the request.
     * Clears the existing name collection before repopulating.
     */
    private void syncNameHistory(Person person, PersonRequest request) {
        if (person.getNames() == null) {
            person.setNames(new LinkedHashSet<>());
        } else {
            person.getNames().clear();
        }

        // Add primary name record
        person.getNames().add(PersonName.builder()
                .person(person)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .type(request.primaryNameType() != null ? request.primaryNameType() : NameType.LEGAL)
                .primary(true)
                .build());

        // Add additional names — aliases, cover names, maiden names etc.
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
                            .primary(false)
                            .build()));
        }
    }

    /**
     * Normalises, deduplicates, and persists contact vectors from the request.
     *
     * <p>Deduplication is applied within the request payload to prevent duplicate
     * contact records being flushed in the same transaction. Contact values are
     * normalised via {@link #normalizeContactValue} for consistent cross-source matching.</p>
     */
    private void syncContactVectors(Person person, PersonRequest request) {
        if (person.getContacts() == null) {
            person.setContacts(new LinkedHashSet<>());
        } else {
            person.getContacts().clear();
        }

        if (request.contacts() == null || request.contacts().isEmpty()) return;

        // Deduplication set — catches duplicate contact vectors within the same request
        Set<String> seen = new HashSet<>();

        for (PersonRequest.ContactRequest contactReq : request.contacts()) {
            String normalizedValue = normalizeContactValue(
                    contactReq.contactType(), contactReq.contactValueRaw());
            String key = contactReq.contactType().name() + "::" + normalizedValue;

            if (!seen.add(key)) {
                log.warn("FAUST_PERSON: Skipping duplicate contact vector: {}.", key);
                continue;
            }

            person.getContacts().add(PersonContact.builder()
                    .person(person)
                    .contactType(contactReq.contactType())
                    .contactValueRaw(contactReq.contactValueRaw().trim())
                    .contactValueNormalized(normalizedValue)
                    .operatorName(contactReq.operatorName() != null
                            ? contactReq.operatorName().trim() : null)
                    .imei(contactReq.imei() != null
                            ? contactReq.imei().replaceAll("\\s+", "") : null)
                    .verificationStatus(contactReq.verificationStatus())
                    .confidenceScore(contactReq.confidenceScore() != null
                            ? contactReq.confidenceScore() : 1.0)
                    .clearanceLevel(contactReq.clearanceLevel())
                    .active(contactReq.active())
                    .validFrom(contactReq.validFrom())
                    .validTo(contactReq.validTo())
                    .analyticalNote(contactReq.analyticalNote())
                    .build());
        }
    }

    /**
     * FININT synchronisation engine — attaches existing or creates new bank account
     * relationships with IBAN deduplication.
     *
     * <p>Mirrors the same pattern used in {@code InstitutionService.syncFinancialVectors}
     * for consistency across the platform.</p>
     */
    private void syncFinancialVectors(Person person, PersonRequest request) {
        if (person.getFinancialAccounts() == null) {
            person.setFinancialAccounts(new LinkedHashSet<>());
        } else {
            person.getFinancialAccounts().clear();
        }

        if (request.financialAccounts() == null || request.financialAccounts().isEmpty()) return;

        for (var finReq : request.financialAccounts()) {
            BankAccount account;

            if (finReq.bankAccountExternalId() != null) {
                // Attach an existing global bank account by UUID
                account = bankAccountRepository.findByExternalId(finReq.bankAccountExternalId())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "BANK_ACCOUNT_NOT_FOUND: " + finReq.bankAccountExternalId()));
            } else if (finReq.newBankAccountData() != null) {
                // Normalise IBAN — strip whitespace for consistent deduplication
                String normalizedIban = finReq.newBankAccountData().iban()
                        .trim().replaceAll("\\s+", "");

                // IBAN deduplication — reuse existing account if already in system
                account = bankAccountRepository.findByIban(normalizedIban)
                        .orElseGet(() -> bankAccountRepository.save(BankAccount.builder()
                                .iban(normalizedIban)
                                .bic(finReq.newBankAccountData().bic() != null
                                        ? finReq.newBankAccountData().bic().trim() : null)
                                .bankName(finReq.newBankAccountData().bankName().trim())
                                .currency(finReq.newBankAccountData().currency()
                                        .toUpperCase().trim())
                                .monitored(finReq.newBankAccountData().monitored())
                                .analyticalNote(finReq.newBankAccountData().analyticalNote())
                                .build()));
            } else {
                log.warn("FAUST_FININT: Skipping financial entry with no account data.");
                continue;
            }

            person.getFinancialAccounts().add(PersonAccountRelation.builder()
                    .person(person)
                    .bankAccount(account)
                    .roleType(finReq.roleType())
                    .validFrom(finReq.validFrom())
                    .validTo(finReq.validTo())
                    .active(finReq.active())
                    .build());
        }
    }

    /**
     * Normalises a raw contact value for consistent cross-source matching.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>EMAIL — lowercased.</li>
     *   <li>CELLULAR_GSM — E.164 approximation: strip all non-digit/non-plus characters.</li>
     *   <li>All others — trimmed, case preserved (crypto addresses, IM handles, etc.).</li>
     * </ul>
     *
     * @param type     the contact channel type.
     * @param rawValue the raw contact value as provided.
     * @return the normalised contact value.
     * @throws IllegalArgumentException if the raw value is null or blank.
     */
    private String normalizeContactValue(ContactType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Contact value cannot be empty for type: " + type);
        }

        String clean = rawValue.trim();

        return switch (type) {
            case EMAIL -> clean.toLowerCase();
            case CELLULAR_GSM -> clean.replaceAll("[^0-9+]", "");
            default -> clean;
        };
    }

    @Transactional(readOnly = true)
    public Page<PersonResponse> searchFiltered(PersonFilter filter, Pageable pageable) {
        Specification<Person> spec = PersonSpecifications.build(filter);
        Page<Person> page = repository.findAll(spec, pageable);

        // Map then enrich each result with currentPositions, exactly as
        // findAll()/getByPublicId() already do. DO NOT call personMapper
        // .toResponse() alone here without the enrichment step, or every
        // person in filtered search results will silently have
        // currentPositions missing/empty while every other endpoint has it.
        return page.map(this::toEnrichedResponse); // <- replace with your
        //    actual enrichment
        //    method name/logic
    }
}