package com.projectfaust.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for the ingestion of public contract data from external registries.
 * Orchestrates the fetching of raw data from the 'Hlídač Státu' API and dispatches
 * individual records to a Kafka topic for downstream processing and entity extraction.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractIngestService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${faust.hlidac-statu.api-token}")
    private String apiToken;

    @Value("${faust.topics.raw-contracts}")
    private String topic;

    /**
     * Initiates a synchronized fetch for all contracts associated with a specific
     * Identification Number (IČO). Each result is transformed into a message
     * and streamed into the 'raw-contracts' Kafka topic.
     *
     * @param ico The unique 8-digit identification number of the legal entity.
     */
    public void fetchContractsForIco(String ico) {
        String url = "https://api.hlidacstatu.cz/api/v2/smlouvy/hledat?dotaz=platce.ico:" + ico;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            log.info("[INGEST] Querying external registry API: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

                results.forEach(contract -> {
                    String externalId = String.valueOf(contract.get("Id"));
                    kafkaTemplate.send(topic, externalId, contract);
                });

                log.info("[INGEST] Successfully dispatched {} contracts to Kafka for ID: {}", results.size(), ico);
            } else {
                log.warn("[INGEST] Registry returned success but zero results for ID: {}", ico);
            }
        } catch (Exception e) {
            log.error("[INGEST_CRITICAL] Communication failure with external provider: {}", e.getMessage());
        }
    }
}
