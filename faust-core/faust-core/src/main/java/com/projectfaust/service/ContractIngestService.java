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

    public void fetchContractsForIco(String ico) {
        // Změna: subdoména 'api' a cesta '/api/v2/smlouvy/hledat'
        String url = "https://api.hlidacstatu.cz/api/v2/smlouvy/hledat?dotaz=platce.ico:" + ico;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            log.info("[INGEST] Volám API Hlídače na: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("results")) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");

                results.forEach(contract -> {
                    // Pozor: Hlídač vrací klíče s velkým počátečním písmenem (Id)
                    String externalId = String.valueOf(contract.get("Id"));
                    kafkaTemplate.send(topic, externalId, contract);
                });

                log.info("[INGEST] Úspěšně odesláno {} smluv do Kafky pro IČO {}", results.size(), ico);
            } else {
                log.warn("[INGEST] API vrátilo úspěch, ale žádné výsledky pro IČO {}", ico);
            }
        } catch (Exception e) {
            log.error("[INGEST_ERROR] Chyba komunikace: {}", e.getMessage());
        }
    }
}
