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

import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository repository;
    private final PersonMapper mapper;

    @Transactional
    public PersonResponse create(PersonRequest request) {
        log.info("Creating person: {} {}", request.firstName(), request.lastName());

        if (request.email() != null && repository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists: " + request.email());
        }

        Person person = mapper.toEntity(request);
        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public PersonResponse getByPublicId(UUID publicId) {

        return repository.findByExternalId(publicId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> findAll() {
        return mapper.toResponseList(repository.findAll());
    }

    @Transactional
    public PersonResponse update(UUID publicId, PersonRequest request) {
        Person person = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        mapper.updateEntityFromRequest(request, person);
        return mapper.toResponse(repository.save(person));
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> searchByName(String query) {
        log.info("Full-text searching for: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        return mapper.toResponseList(repository.searchByFullName(query.trim()));
    }
}
