package com.projectfaust.ingest.theses;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.shared.enums.ThesisType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka consumer for the raw-theses topic.
 *
 * <p><b>Pipeline position:</b> consumes thesis records produced by
 * {@link ThesisFetchService} (Semantic Scholar), deserialises the JSON payload,
 * deduplicates by {@code sourceSystemId}, resolves the target {@link Person},
 * maps to {@link AcademicThesis}, and persists.</p>
 *
 * <p><b>Idempotency:</b> {@code existsBySourceSystemId} is checked before
 * every persist — re-ingesting the same thesis record is always a no-op.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisConsumerService {

    private final AcademicThesisRepository thesisRepository;
    private final PersonRepository personRepository;

    /**
     * Shared, immutable Jackson mapper.
     *
     * <p><b>FIXED:</b> previously instantiated a new {@code ObjectMapper}
     * inside {@code consumeThesis()} on every Kafka message — ObjectMapper
     * construction is expensive (reflection, module scanning) and should
     * never happen per-message. Now a static final field built once via
     * {@link JsonMapper#builder()}, matching the pattern used in
     * {@link com.projectfaust.ingest.hlidacstatu.ContractConsumerService}.</p>
     */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .findAndAddModules()
            .build();

    @KafkaListener(
            topics = "${faust.topics.raw-theses:raw-theses}",
            groupId = "faust-thesis-v1"
    )
    public void consumeThesis(byte[] rawData) {
        try {
            JsonNode json = MAPPER.readTree(rawData);

            String sourceId = getText(json, "id");
            if (sourceId == null) {
                log.error("THESIS_CONSUMER: Received thesis with null id — skipping.");
                return;
            }

            // Idempotency — skip if already persisted
            if (thesisRepository.existsBySourceSystemId(sourceId)) {
                log.debug("THESIS_CONSUMER: Thesis {} already exists — skipping.", sourceId);
                return;
            }

            String personIdStr = getText(json, "targetPersonId");
            if (personIdStr == null) {
                log.error("THESIS_CONSUMER: Thesis {} has no targetPersonId — skipping.", sourceId);
                return;
            }

            UUID personId = UUID.fromString(personIdStr);
            Person person = personRepository.findByExternalId(personId)
                    .orElseThrow(() -> new RuntimeException(
                            "THESIS_CONSUMER: Person not found: " + personId));

            // Resolve ThesisType safely — default MASTER if missing or unrecognised
            ThesisType thesisType = MASTER_DEFAULT;
            String typeStr = getText(json, "type");
            if (typeStr != null) {
                try {
                    thesisType = ThesisType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("THESIS_CONSUMER: Unknown ThesisType '{}' for thesis {} — defaulting to MASTER.",
                            typeStr, sourceId);
                }
            }

            AcademicThesis entity = AcademicThesis.builder()
                    .person(person)
                    .sourceSystemId(sourceId)
                    .title(getText(json, "title"))
                    .universityName(getTextOrDefault(json, "university", "Unknown"))
                    .facultyName(getText(json, "faculty"))           // nullable — safe
                    .supervisorName(getText(json, "supervisor"))     // nullable — safe
                    .opponentName(getText(json, "opponent"))         // nullable — safe
                    .defenseYear(getInt(json, "year"))               // nullable — safe
                    .thesisType(thesisType)
                    .repositoryUrl(getText(json, "url"))             // nullable — safe
                    // FIXED: was .isClassified(...) — field renamed to 'classified',
                    // Lombok builder method is now .classified(...)
                    .classified(getBoolean(json, "is_classified"))
                    // FIXED: was .isVerifiedByAgent(false) — field renamed to 'verifiedByAgent',
                    // Lombok builder method is now .verifiedByAgent(...)
                    .verifiedByAgent(false)
                    .language(getText(json, "language"))
                    .abstractText(getText(json, "abstract"))
                    .keywords(getText(json, "keywords"))
                    .build();

            thesisRepository.save(entity);
            log.info("THESIS_CONSUMER: Linked thesis '{}' to person {} {}.",
                    entity.getTitle(), person.getFirstName(), person.getLastName());

        } catch (Exception e) {
            log.error("THESIS_CONSUMER: Failed to process thesis message — {}", e.getMessage());
            log.debug("THESIS_CONSUMER: Raw payload: {}", new String(rawData));
        }
    }

    // -------------------------------------------------------------------------
    // Null-safe JSON helpers — prevents NullPointerException when optional
    // fields are absent from the payload. json.get("field") returns null
    // for missing keys; calling .asText() on null throws NPE.
    // -------------------------------------------------------------------------

    private static final ThesisType MASTER_DEFAULT = ThesisType.MASTER;

    /** Returns text value or null if field is missing. */
    private static String getText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return (node != null && !node.isNull()) ? node.asText() : null;
    }

    /** Returns text value or a fallback default if field is missing. */
    private static String getTextOrDefault(JsonNode json, String field, String defaultValue) {
        String val = getText(json, field);
        return val != null ? val : defaultValue;
    }

    /** Returns int value or null if field is missing. */
    private static Integer getInt(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return (node != null && !node.isNull()) ? node.asInt() : null;
    }

    /** Returns boolean value or false if field is missing. */
    private static boolean getBoolean(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return (node != null && !node.isNull()) && node.asBoolean();
    }
}