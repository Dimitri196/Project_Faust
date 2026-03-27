package com.projectfaust.controller;

import com.projectfaust.repository.PersonRepository;
import com.projectfaust.service.ThesisIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ingest/theses")
@RequiredArgsConstructor
public class ThesisIngestController {

    private final ThesisIngestService ingestService;
    private final PersonRepository personRepository;

    /**
     * Spustí hledání závěrečných prací pro osobu v DB.
     * @param personId UUID osoby v systému Faust
     */
    @PostMapping("/person/{personId}")
    public String triggerThesisIngest(@PathVariable UUID personId) {
        var person = personRepository.findByExternalId(personId)
                .orElseThrow(() -> new RuntimeException("Osoba nenalezena v DB"));

        ingestService.fetchThesesForPerson(
                person.getExternalId(),
                person.getFirstName(),
                person.getLastName()
        );

        return "Thesis lookup initiated for: " + person.getFirstName() + " " + person.getLastName();
    }
}