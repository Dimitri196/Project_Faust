package com.projectfaust.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.projectfaust.entity.AcademicThesis;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.enums.ThesisType;
import com.projectfaust.repository.AcademicThesisRepository;
import com.projectfaust.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThesisConsumerService {

    private final AcademicThesisRepository thesisRepository;
    private final PersonRepository personRepository; // Potřebujeme pro linkování

    @KafkaListener(
            topics = "${faust.topics.raw-theses:raw-theses}",
            groupId = "faust-thesis-v1"
    )
    public void consumeThesis(byte[] rawData) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();

            // Použijeme tvůj nový Record pro request (nebo Map pro flexibilitu)
            JsonNode json = mapper.readTree(rawData);

            String sourceId = json.get("id").asText();
            UUID personId = UUID.fromString(json.get("targetPersonId").asText());

            // Idempotence: Už tu práci máme?
            if (thesisRepository.existsBySourceSystemId(sourceId)) {
                log.info("[SKIP_THESIS] Thesis {} already exists.", sourceId);
                return;
            }

            Person person = personRepository.findByExternalId(personId)
                    .orElseThrow(() -> new RuntimeException("Person not found"));

            // Mapování na entitu
            AcademicThesis entity = AcademicThesis.builder()
                    .person(person)
                    .sourceSystemId(sourceId)
                    .title(json.get("title").asText())
                    .universityName(json.get("university").asText())
                    .facultyName(json.get("faculty").asText())
                    .supervisorName(json.get("supervisor").asText())
                    .defenseYear(json.get("year").asInt())
                    .thesisType(ThesisType.valueOf(json.get("type").asText()))
                    .repositoryUrl(json.get("url").asText())
                    .isClassified(json.get("is_classified").asBoolean())
                    .isVerifiedByAgent(false) // Čeká na potvrzení analytikem
                    .build();

            thesisRepository.save(entity);
            log.info("[DB_THESIS_SUCCESS] Linked thesis '{}' to person {}", entity.getTitle(), person.getLastName());

        } catch (Exception e) {
            log.error("[THESIS_CONSUMER_FAILURE] Error: {}", e.getMessage());
        }
    }
}