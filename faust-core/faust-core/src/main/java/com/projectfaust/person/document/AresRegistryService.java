package com.projectfaust.person.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Client for the Czech ARES registry (Administrativní registr ekonomických subjektů).
 *
 * <p>ARES v2 API endpoint:
 * {@code https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/{ico}}
 *
 * <p>Returns company/entity data for a given IČO including:
 * <ul>
 *   <li>Legal name, address</li>
 *   <li>Date of registration / dissolution</li>
 *   <li>DIČ (VAT number)</li>
 *   <li>Legal form</li>
 *   <li>Statutory representatives</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AresRegistryService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String ARES_BASE_URL =
            "https://ares.gov.cz/ekonomicke-subjekty-v-be/rest/ekonomicke-subjekty/";

    /**
     * Fetches entity data from ARES for a given IČO and returns a
     * pre-populated {@link PersonDocumentRequest} ready for persistence.
     *
     * @param ico            the 8-digit Czech company registration number.
     * @param personPublicId the person to link this document to.
     * @return populated request or empty if ARES returns no data.
     */
    public Optional<AresResult> fetchByIco(String ico) {
        try {
            String url = ARES_BASE_URL + ico.replaceAll("\\s", "");
            log.info("ARES: Fetching IČO {}.", ico);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "ProjectFaust/1.0");

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class);

            if (response.getBody() == null) {
                log.warn("ARES: Empty response for IČO {}.", ico);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            AresResult result = parseAresResponse(ico, root, response.getBody());
            log.info("ARES: Found entity '{}' for IČO {}.", result.legalName(), ico);
            return Optional.of(result);

        } catch (Exception e) {
            log.error("ARES: Failed to fetch IČO {} — {}", ico, e.getMessage());
            return Optional.empty();
        }
    }

    private AresResult parseAresResponse(String ico, JsonNode root, String rawJson) {
        String legalName    = getTextField(root, "obchodniJmeno");
        String dic          = getTextField(root, "dic");
        String legalForm    = getTextField(root, "pravniForma", "nazev");
        String registeredAt = getTextField(root, "datumVzniku");
        String dissolvedAt  = getTextField(root, "datumZaniku");

        // Address
        JsonNode addr = root.path("sidlo");
        String address = addr.isMissingNode() ? null :
                String.join(", ",
                        getTextField(addr, "nazevUlice"),
                        getTextField(addr, "cisloDomovni"),
                        getTextField(addr, "nazevObce"),
                        getTextField(addr, "psc"));

        return new AresResult(
                ico,
                dic,
                legalName,
                legalForm,
                address,
                parseDate(registeredAt),
                parseDate(dissolvedAt),
                rawJson
        );
    }

    private String getTextField(JsonNode node, String... path) {
        JsonNode current = node;
        for (String key : path) {
            current = current.path(key);
            if (current.isMissingNode()) return null;
        }
        return current.isTextual() ? current.asText() : null;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Result DTO from ARES lookup.
     */
    public record AresResult(
            String ico,
            String dic,
            String legalName,
            String legalForm,
            String address,
            LocalDate registeredAt,
            LocalDate dissolvedAt,
            String rawJson
    ) {}
}