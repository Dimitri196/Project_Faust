package com.projectfaust.ingest.ted;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectfaust.ingest.dto.TedNoticeDto;
import com.projectfaust.shared.enums.SourceSystem;
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
 * Fetches public procurement notice data from the TED (Tenders Electronic Daily)
 * API v3 and dispatches raw records to Kafka for asynchronous processing.
 *
 * <p><b>Pipeline position:</b> entry point for TED data — performs the outbound
 * HTTP call to TED and produces to Kafka. The consumer side
 * ({@link com.projectfaust.ingest.hlidacstatu.ContractConsumerService}) deserialises
 * and persists via {@link com.projectfaust.ingest.core.ContractIngestService}.</p>
 *
 * <p><b>API characteristics:</b></p>
 * <ul>
 *   <li>Endpoint: {@code POST https://api.ted.europa.eu/v3/notices/search}</li>
 *   <li>Anonymous access — no API key required for published notices.</li>
 *   <li>Query language: TED Expert Search syntax
 *       (e.g. {@code buyer-country=CZE AND national-id=00006947}).</li>
 *   <li>Pagination: ITERATION mode (scroll) for large result sets.</li>
 *   <li>Notice types: both Contract Notices ({@code cn-standard}) and
 *       Contract Award Notices ({@code can-standard}) are fetched —
 *       the mapper handles both.</li>
 * </ul>
 *
 * <p><b>Trust level:</b> TED is the EU's official procurement journal.
 * Records are ingested with {@link com.projectfaust.shared.enums.VerificationStatus#OFFICIAL_REGISTRY}
 * — the highest trust level in the current pipeline, unlike Hlidač Státu
 * which defaults to {@code UNVERIFIED}.</p>
 *
 * <p><b>Mirrors:</b> {@link com.projectfaust.ingest.hlidacstatu.ExternalContractFetchService}
 * in structure — same Kafka dispatch pattern, same pipeline position,
 * different source and query syntax.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TedContractFetchService {

    private static final String TED_SEARCH_URL = "https://api.ted.europa.eu/v3/notices/search";

    // Fields requested from TED API — only what the mapper needs.
    // Requesting fewer fields = smaller payloads and faster responses.
    private static final List<String> REQUESTED_FIELDS = List.of(
            "publication-number",
            "publication-date",
            "notice-type",
            "buyer",
            "award-outcome",
            "notice-value",
            "short-description",
            "main-cpv"
    );

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${faust.topics.raw-contracts}")
    private String topic;

    /**
     * Fetches all TED notices where the given national registration number appears
     * as the buyer's national ID, and dispatches each to Kafka.
     *
     * <p>Queries both Contract Notices (cn-standard) and Contract Award Notices
     * (can-standard) in a single request using TED's OR syntax. Award notices
     * contain the supplier and final awarded value; contract notices contain
     * only the tender announcement.</p>
     *
     * <p>TED uses ISO 3166-1 alpha-3 country codes ("CZE" not "CZ") in the
     * buyer-country filter — this is a TED-specific quirk handled here so
     * callers can always pass the standard alpha-2 code.</p>
     *
     * @param nationalId  the buyer's national registration number (IČO for CZ).
     * @param countryCode ISO 3166-1 alpha-2 country code of the buyer's registry
     *                    (e.g. "CZ" — converted to alpha-3 "CZE" internally for TED).
     */
    public void fetchContractsForNationalId(String nationalId, String countryCode) {
        String tedCountryCode = toAlpha3(countryCode);
        // TED Expert Search query — matches notices where:
        //   - buyer-country is the target country (alpha-3)
        //   - buyer's national-id matches the registration number
        //   - notice-type is either a tender or an award notice
        String query = String.format(
                "buyer-country=%s AND national-id=%s AND notice-type=[cn-standard OR can-standard]",
                tedCountryCode, nationalId
        );

        log.info("TED_FETCH: Querying TED API for national-id={} country={}.",
                nationalId, tedCountryCode);

        int page = 1;
        int totalDispatched = 0;
        boolean hasMore = true;

        while (hasMore) {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "fields", REQUESTED_FIELDS,
                    "limit", 100,
                    "page", page,
                    "scope", "ACTIVE",
                    "paginationMode", "PAGE"
            );

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        TED_SEARCH_URL,
                        HttpMethod.POST,
                        entity,
                        new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});

                if (response.getBody() == null) {
                    log.warn("TED_FETCH: Null response body for national-id={} page={}.",
                            nationalId, page);
                    break;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> notices =
                        (List<Map<String, Object>>) response.getBody().get("notices");

                if (notices == null || notices.isEmpty()) {
                    hasMore = false;
                    break;
                }

                for (Map<String, Object> rawNotice : notices) {
                    try {
                        // Deserialise to typed DTO for Kafka dispatch
                        TedNoticeDto dto = objectMapper.convertValue(rawNotice, TedNoticeDto.class);
                        String key = dto.publicationNumber() != null
                                ? dto.publicationNumber()
                                : String.valueOf(System.nanoTime());

                        // Wrap with source system marker so ContractConsumerService
                        // routes to the correct mapper — same pattern as Hlidač Státu.
                        kafkaTemplate.send(topic, key, Map.of(
                                "sourceSystem", SourceSystem.TED_EU.name(),
                                "payload", rawNotice
                        ));
                        totalDispatched++;
                    } catch (Exception e) {
                        log.error("TED_FETCH: Failed to dispatch notice — {}", e.getMessage());
                    }
                }

                // Check if there are more pages
                @SuppressWarnings("unchecked")
                Map<String, Object> links = (Map<String, Object>) response.getBody().get("links");
                hasMore = links != null && links.containsKey("next");
                page++;

            } catch (Exception e) {
                log.error("TED_FETCH: Communication failure with TED API — {}", e.getMessage());
                break;
            }
        }

        log.info("TED_FETCH: Dispatched {} notices to Kafka for national-id={} country={}.",
                totalDispatched, nationalId, tedCountryCode);
    }

    // -------------------------------------------------------------------------
    // Helper — ISO 3166-1 alpha-2 → alpha-3 conversion for TED query syntax.
    // TED uses alpha-3 in buyer-country filter ("CZE" not "CZ").
    // Covers the countries most likely to appear in Faust's institution registry.
    // -------------------------------------------------------------------------
    private String toAlpha3(String alpha2) {
        if (alpha2 == null) return "CZE"; // default to CZ
        return switch (alpha2.toUpperCase()) {
            case "CZ" -> "CZE";
            case "SK" -> "SVK";
            case "DE" -> "DEU";
            case "AT" -> "AUT";
            case "PL" -> "POL";
            case "FR" -> "FRA";
            case "HU" -> "HUN";
            case "RO" -> "ROU";
            case "GB" -> "GBR";
            case "US" -> "USA";
            case "UA" -> "UKR";
            case "RU" -> "RUS";
            case "CN" -> "CHN";
            default   -> {
                log.warn("TED_FETCH: No alpha-3 mapping for country code '{}' — " +
                        "passing as-is; TED query may return no results.", alpha2);
                yield alpha2;
            }
        };
    }
}