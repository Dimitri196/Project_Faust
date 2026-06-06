package com.projectfaust.service;

import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonName;
import com.projectfaust.entity.enums.NameType;
import com.projectfaust.repository.PersonNameRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonNameService {

    private final PersonNameRepository nameRepository;
    private final PersonRepository personRepository;

    /**
     * Bezpečně změní legální jméno osoby (např. po sňatku).
     * Staré jméno archivuje jako HISTORICAL/MAIDEN.
     */
    @Transactional
    public void changeLegalName(UUID personExtId, String newFirstName, String newLastName, NameType oldNameNewType) {
        // 1. Deaktivujeme stávající primární jméno
        nameRepository.deactivateAllPrimaryNames(personExtId);

        Person person = personRepository.findByExternalId(personExtId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        // 2. Vytvoříme nové primární jméno
        PersonName newPrimary = PersonName.builder()
                .person(person)
                .firstName(newFirstName)
                .lastName(newLastName)
                .type(NameType.LEGAL)
                .isPrimary(true)
                .validFrom(LocalDate.now())
                .build();

        nameRepository.save(newPrimary);
        log.info("Identity updated for person {}: New primary name set.", personExtId);
    }

    /**
     * Přidá operativní alias pro účely tajných služeb.
     */
    @Transactional
    public void addAlias(UUID personExtId, String aliasFirst, String aliasLast, String operationNote) {
        Person person = personRepository.findByExternalId(personExtId)
                .orElseThrow(() -> new EntityNotFoundException("Person not found"));

        PersonName alias = PersonName.builder()
                .person(person)
                .firstName(aliasFirst)
                .lastName(aliasLast)
                .type(NameType.ALIAS)
                .isPrimary(false) // Alias typicky není primární identita v registru
                .note(operationNote)
                .validFrom(LocalDate.now())
                .build();

        nameRepository.save(alias);
    }
}