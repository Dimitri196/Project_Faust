package com.projectfaust.ingest.realestate.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.projectfaust.ingest.realestate.dto.RealEstateOwnershipDto;
import com.projectfaust.shared.enums.CadasterSourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for the raw real estate ownership topic.
 *
 * <p>Reads the {@code sourceSystem} envelope field set by the country-specific
 * fetch service, deserialises the normalised {@link RealEstateOwnershipDto}
 * payload, and delegates to {@link RealEstateIngestService} which routes to
 * the correct {@link RealEstateSourceMapper}.</p>
 *
 * <p>All country-specific fetch services dispatch in the same envelope format:
 * {@code {"sourceSystem": "CUZK_CZ", "payload": {...}}} — same pattern as
 * the contract pipeline ({@link com.projectfaust.ingest.hlidacstatu.ContractConsumerService}).</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealEstateConsumerService {

    private final RealEstateIngestService ingestService;

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
            .findAndAddModules()
            .build();

    @KafkaListener(
            topics = "${faust.topics.raw-realestate:ingest.realestate.ownership}",
            groupId = "faust-realestate-v1"
    )
    public void consumeOwnership(byte[] rawData) {
        try {
            JsonNode envelope = MAPPER.readTree(rawData);
            String sourceSystemRaw = envelope.path("sourceSystem").asText(null);
            JsonNode payload = envelope.path("payload");

            if (sourceSystemRaw == null || payload.isMissingNode()) {
                log.error("REALESTATE_CONSUMER: Missing sourceSystem or payload — skipping.");
                return;
            }

            CadasterSourceSystem sourceSystem = CadasterSourceSystem.valueOf(sourceSystemRaw);
            RealEstateOwnershipDto dto = MAPPER.treeToValue(payload, RealEstateOwnershipDto.class);

            if (dto.externalId() == null) {
                log.error("REALESTATE_CONSUMER: Null externalId for source {} — skipping.",
                        sourceSystem);
                return;
            }

            ingestService.ingestOwnership(sourceSystem, dto);

        } catch (Exception e) {
            log.error("REALESTATE_CONSUMER: Failed to process message — {}", e.getMessage());
            log.debug("REALESTATE_CONSUMER: Raw payload: {}", new String(rawData));
        }
    }
}
