package com.projectfaust.ingest.hlidacstatu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.projectfaust.ingest.core.ContractIngestService;
import com.projectfaust.ingest.dto.HlidacContractDto;
import com.projectfaust.ingest.dto.TedNoticeDto;
import com.projectfaust.shared.enums.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka consumer for the raw contracts topic.
 *
 * <p><b>Pipeline position:</b> consumes raw payloads produced by either
 * {@link ExternalContractFetchService} (Hlidač Státu) or
 * {@link com.projectfaust.ingest.ted.TedContractFetchService} (TED EU),
 * deserialises them into the appropriate source-specific DTO based on the
 * {@code sourceSystem} marker each producer attaches, and delegates
 * persistence to {@link ContractIngestService}.</p>
 *
 * <p><b>FIXED:</b> previously deserialised every message directly as
 * {@link HlidacContractDto} regardless of origin. Once
 * {@code TedContractFetchService} began dispatching to the same Kafka topic
 * (wrapped as {@code {"sourceSystem": "TED_EU", "payload": {...}}}), this
 * consumer would have attempted to force TED notices into the Hlidač Státu
 * DTO shape — a structural mismatch (TED uses nested buyer/award-outcome
 * objects, Hlidač Státu uses flat aliased fields), causing either outright
 * deserialisation failure or silent partial/null field population. The
 * consumer now reads the {@code sourceSystem} marker first and routes to
 * the matching DTO type and {@link SourceSystem} before delegating to
 * {@code ingestService}, which already dispatches to the correct
 * {@link com.projectfaust.ingest.core.ContractSourceMapper} by source.</p>
 *
 * <p>All source-specific mapping (field aliases, supplier-resolution fallbacks,
 * date parsing) lives in the per-source mapper implementations
 * ({@link HlidacStatuContractMapper}, {@link com.projectfaust.ingest.ted.TedContractMapper})
 * — this consumer is intentionally thin and source-agnostic beyond the
 * routing step above.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractConsumerService {

    private final ContractIngestService ingestService;

    /**
     * Shared, immutable Jackson mapper for this consumer.
     *
     * <p><b>CHANGED:</b> previously built via the now-deprecated
     * {@code new ObjectMapper().configure(MapperFeature, boolean)} pattern
     * (deprecated since Jackson 2.13). Now constructed via
     * {@link JsonMapper#builder()}, the recommended fluent/immutable
     * builder API. {@code ObjectMapper} is retained as the declared field
     * type since {@code JsonMapper} is a concrete subclass — this keeps the
     * rest of the class (and any future code referencing {@code MAPPER})
     * unaffected by the construction change.</p>
     */
    private static final ObjectMapper MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
                .findAndAddModules()
                .build();
    }

    /**
     * Consumes a single raw contract record from Kafka.
     *
     * <p>Reads the {@code sourceSystem} marker attached by the producer
     * (see {@link ExternalContractFetchService} and
     * {@link com.projectfaust.ingest.ted.TedContractFetchService}) and
     * routes to the matching DTO type before delegating to
     * {@link ContractIngestService}. Deserialisation failures and
     * individual record errors are logged and swallowed — a malformed
     * record does not stop the consumer from processing subsequent
     * messages.</p>
     *
     * @param rawData the raw JSON payload as received from the producer,
     *                shaped as {@code {"sourceSystem": "...", "payload": {...}}}.
     */
    @KafkaListener(
            topics = "${faust.topics.raw-contracts}",
            groupId = "faust-v5"
    )
    public void consumeContract(byte[] rawData) {
        try {
            JsonNode envelope = MAPPER.readTree(rawData);
            String sourceSystemRaw = envelope.path("sourceSystem").asText(null);
            JsonNode payload = envelope.path("payload");

            if (sourceSystemRaw == null || payload.isMissingNode()) {
                log.error("FAUST_INGEST: Received message with missing sourceSystem or " +
                        "payload envelope — payload corrupted, skipping.");
                return;
            }

            SourceSystem sourceSystem = SourceSystem.valueOf(sourceSystemRaw);

            switch (sourceSystem) {
                case HLIDAC_STATU_CZ -> {
                    HlidacContractDto dto = MAPPER.treeToValue(payload, HlidacContractDto.class);
                    if (dto.id() == null) {
                        log.error("FAUST_INGEST: Received Hlidac Statu contract with null ID — " +
                                "payload corrupted, skipping.");
                        return;
                    }
                    ingestService.ingestContract(SourceSystem.HLIDAC_STATU_CZ, dto);
                }
                case TED_EU -> {
                    TedNoticeDto dto = MAPPER.treeToValue(payload, TedNoticeDto.class);
                    if (dto.publicationNumber() == null) {
                        log.error("FAUST_INGEST: Received TED notice with null publication number — " +
                                "payload corrupted, skipping.");
                        return;
                    }
                    ingestService.ingestContract(SourceSystem.TED_EU, dto);
                }
                default -> log.warn("FAUST_INGEST: No consumer routing defined for source system {} — skipping.",
                        sourceSystem);
            }

        } catch (Exception e) {
            log.error("FAUST_INGEST: Failed to process contract message — {}", e.getMessage());
            log.debug("FAUST_INGEST: Raw payload for forensic analysis: {}", new String(rawData));
        }
    }
}