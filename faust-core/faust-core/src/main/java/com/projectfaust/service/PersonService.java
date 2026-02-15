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

        if (request.email() != null && !request.email().isBlank() && repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        Person person = mapper.toEntity(request);
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

        // Validace unikátnosti emailu při změně
        if (request.email() != null && !request.email().equalsIgnoreCase(person.getEmail())
                && repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("New email already taken: " + request.email());
        }

        mapper.updateEntityFromRequest(request, person);
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

        // 1. Interní kontrola duplicit v seznamu
        Set<String> processedEmails = new HashSet<>();
        for (PersonRequest req : requests) {
            if (req.email() != null && !processedEmails.add(req.email().toLowerCase())) {
                throw new IllegalArgumentException("Duplicate email in request list: " + req.email());
            }
        }

        // 2. Kontrola proti databázi (hromadně)
        List<String> emailsToCheck = requests.stream()
                .map(PersonRequest::email)
                .filter(Objects::nonNull)
                .toList();

        if (!emailsToCheck.isEmpty()) {
            boolean anyExists = repository.findAllByEmailIn(emailsToCheck).size() > 0;
            if (anyExists) throw new IllegalArgumentException("One or more emails already exist in database.");
        }

        // 3. Uložení
        List<Person> entities = requests.stream().map(mapper::toEntity).toList();
        return mapper.toResponseList(repository.saveAll(entities));
    }
}
