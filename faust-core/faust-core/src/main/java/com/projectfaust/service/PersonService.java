package com.projectfaust.service;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.mapper.PersonMapper;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    @Transactional
    public PersonResponse create(PersonRequest request) {
        log.info("Creating person: {} {}", request.firstName(), request.lastName());

        String sanitizedEmail = sanitizeEmail(request.email());

        if (sanitizedEmail != null && repository.existsByEmail(sanitizedEmail)) {
            throw new IllegalArgumentException("Email already exists: " + sanitizedEmail);
        }

        Person person = mapper.toEntity(request);
        // Ruční override po mapování pro jistotu
        person.setEmail(sanitizedEmail);

        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public PersonResponse getByPublicId(UUID publicId) {
        return repository.findByExternalId(publicId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Person not found with ID: " + publicId));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    @Transactional
    public PersonResponse update(UUID publicId, PersonRequest request) {
        Person person = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        String sanitizedEmail = sanitizeEmail(request.email());

        // Validace unikátnosti emailu při změně (pokud se liší od původního)
        if (sanitizedEmail != null && !sanitizedEmail.equalsIgnoreCase(person.getEmail())
                && repository.existsByEmail(sanitizedEmail)) {
            throw new IllegalArgumentException("New email already taken: " + sanitizedEmail);
        }

        mapper.updateEntityFromRequest(request, person);
        person.setEmail(sanitizedEmail); // Zajistíme null místo ""

        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> searchByName(String query) {
        if (query == null || query.isBlank()) return List.of();
        return mapper.toResponseList(repository.searchByFullName(query.trim().toLowerCase()));
    }

    @Transactional
    public List<PersonResponse> createBulk(List<PersonRequest> requests) {
        log.info("Commencing bulk ingestion: {} entries", requests.size());

        // 1. Interní kontrola duplicit v seznamu a příprava entit
        Set<String> processedEmails = new HashSet<>();
        List<Person> entitiesToSave = new ArrayList<>();

        for (PersonRequest req : requests) {
            String sanitizedEmail = sanitizeEmail(req.email());

            // Kontrola duplicit uvnitř nahrávaného listu
            if (sanitizedEmail != null) {
                if (!processedEmails.add(sanitizedEmail)) {
                    throw new IllegalArgumentException("Duplicate email in request list: " + sanitizedEmail);
                }
            }

            Person entity = mapper.toEntity(req);
            entity.setEmail(sanitizedEmail); // Vynutíme null místo ""
            entitiesToSave.add(entity);
        }

        // 2. Hromadná kontrola proti existující databázi
        if (!processedEmails.isEmpty()) {
            List<String> existingEmails = repository.findAllByEmailIn(new ArrayList<>(processedEmails))
                    .stream()
                    .map(Person::getEmail)
                    .toList();

            if (!existingEmails.isEmpty()) {
                throw new IllegalArgumentException("One or more emails already exist in database: " + existingEmails);
            }
        }

        // 3. Uložení všech entit najednou
        return mapper.toResponseList(repository.saveAll(entitiesToSave));
    }

    /**
     * Pomocná metoda pro normalizaci emailu.
     * Převádí prázdné stringy na NULL pro zachování unikátnosti v DB.
     */
    private String sanitizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}