package com.projectfaust.ingest.hlidacstatu;

import com.projectfaust.shared.enums.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Fetches public procurement contracts from the Hlidač Státu API v2 (CZ)
 * and dispatches each record to Kafka for normalised ingestion.
 *
 * <p>Fetches both buyer ({@code icoPlatce}) and supplier ({@code icoPrijemce})
 * sides with full pagination up to {@link #MAX_PAGES} × {@link #PAGE_SIZE}
 * records per side. Czech query tokens are confined to URL strings only.</p>
 *
 * @author Dimitri / Project Faust
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalContractFetchService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${faust.hlidac-statu.api-token}")
    private String apiToken;

    @Value("${faust.topics.raw-contracts}")
    private String topic;

    /** Maximum number of pages to fetch per query side (buyer/supplier). */
    private static final int MAX_PAGES = 50;

    /** Number of results per page — Hlidač Státu API maximum. */
    private static final int PAGE_SIZE = 100;

    /**
     * Fetches all contracts for a given IČO from Hlidač Státu (both buyer
     * and supplier sides) and dispatches each to Kafka.
     *
     * @param ico the 8-digit Czech company registration number.
     */
    public void fetchContractsForIco(String ico) {
        log.info("FAUST_INGEST: Starting contract fetch for IČO {}.", ico);

        // Fetch as buyer (platce) — most government institutions appear here
        int buyerCount = fetchByQuery("icoPlatce:" + ico, ico);

        // Fetch as supplier (prijemce)
        int supplierCount = fetchByQuery("icoPrijemce:" + ico, ico);

        log.info("FAUST_INGEST: IČO {} — dispatched {} buyer + {} supplier contracts.",
                ico, buyerCount, supplierCount);
    }

    /**
     * Executes a paginated query against the Hlidač Státu API and dispatches
     * each result to Kafka. Stops when the page is empty or {@link #MAX_PAGES}
     * is reached.
     *
     * @param query the Hlidač Státu query string (Czech tokens confined here).
     * @param ico   the IČO — used only for logging.
     * @return total number of contracts dispatched.
     */
    private int fetchByQuery(String query, String ico) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Token " + apiToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        int totalDispatched = 0;

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = "https://api.hlidacstatu.cz/api/v2/smlouvy/hledat"
                    + "?dotaz=" + query
                    + "&strana=" + page
                    + "&pocet=" + PAGE_SIZE;

            try {
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        entity,
                        new org.springframework.core.ParameterizedTypeReference<>() {});

                if (response.getBody() == null
                        || !response.getBody().containsKey("results")) {
                    log.debug("FAUST_INGEST: No results on page {} for query '{}'.", page, query);
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results =
                        (List<Map<String, Object>>) response.getBody().get("results");

                if (results == null || results.isEmpty()) {
                    log.debug("FAUST_INGEST: Empty page {} — stopping pagination.", page);
                    break;
                }

                results.forEach(contract -> {
                    // Hlidač Státu returns "Id" (capital I) in v2 API
                    // Fall back to lowercase variants to be safe
                    Object idObj = contract.get("Id");
                    if (idObj == null) idObj = contract.get("id");
                    if (idObj == null) idObj = contract.get("ID");

                    if (idObj == null) {
                        log.warn("FAUST_INGEST: Contract has no Id field — skipping.");
                        return;
                    }

                    String externalId = String.valueOf(idObj);
                    kafkaTemplate.send(topic, externalId, Map.of(
                            "sourceSystem", SourceSystem.HLIDAC_STATU_CZ.name(),
                            "payload", contract
                    ));
                });

                totalDispatched += results.size();
                log.debug("FAUST_INGEST: Page {}/{} — {} results for ICO {}.",
                        page, MAX_PAGES, results.size(), ico);

                // If fewer results than page size — we've hit the last page
                if (results.size() < PAGE_SIZE) {
                    log.debug("FAUST_INGEST: Last page reached at page {}.", page);
                    break;
                }

                // Polite delay to avoid hammering the API
                Thread.sleep(200);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("FAUST_INGEST: Pagination interrupted at page {} for ICO {}.", page, ico);
                break;
            } catch (Exception e) {
                log.error("FAUST_INGEST: Error on page {} for query '{}' — {}",
                        page, query, e.getMessage());
                break;
            }
        }

        return totalDispatched;
    }
}