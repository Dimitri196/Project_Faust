package com.projectfaust.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * TO DO AS A FIRST: TRY TO FIND REAL API FOR ALL EDUCATIONAL RECORDS THESIS SERVICES ABROAD OF ALL CZ UNIVERSITIES OR OTHER RELATED HIGH SCHOOLS...
 * secondary API services: list of middle schools and lower schools, list of courses, list of teachers, list of students, list of research projects, list of publications, list of patents, list of grants, list of awards, list of conferences, list of workshops, list of seminars, list of internships, list of scholarships, list of exchange programs, list of alumni associations, list of student organizations, list of faculty organizations, list of research centers, list of laboratories, list of libraries, list of museums, list of archives, list of galleries, list of theaters, list of cinemas, list of sports facilities, list of cultural facilities, list of social facilities, list of health facilities, list of transportation facilities, list of communication facilities, list of energy facilities, list of water facilities, list of waste facilities, list of environmental facilities.
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class ThesisIngestService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${faust.topics.raw-theses}")
    private String topic;

    /**
     * Vyhledá práce pro konkrétní osobu podle jména a pošle je do Kafky.
     */
    public void fetchThesesForPerson(UUID personPublicId, String firstName, String lastName) {
        // Příklad URL: vyhledávání v registru přes jméno a příjmení
        String query = firstName + " " + lastName;
        String url = "https://api.theses.cz/api/v1/search?q=" + query;

        try {
            log.info("[INGEST_THESIS] Searching academic records for: {} (ID: {})", query, personPublicId);

            // Poznámka: Zde předpokládáme, že API vrací seznam prací
            ResponseEntity<List> response = restTemplate.getForEntity(url, List.class);

            if (response.getBody() != null) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody();

                results.forEach(thesis -> {
                    // Do Kafky posíláme mapu, která obsahuje i personPublicId,
                    // abychom v Consumeru věděli, ke komu jsme to původně hledali.
                    thesis.put("targetPersonId", personPublicId.toString());
                    String externalId = String.valueOf(thesis.get("id"));
                    kafkaTemplate.send(topic, externalId, thesis);
                });

                log.info("[INGEST_THESIS] Dispatched {} potential theses to Kafka for {}", results.size(), query);
            }
        } catch (Exception e) {
            log.error("[INGEST_THESIS_ERROR] Failed to fetch data: {}", e.getMessage());
        }
    }
}