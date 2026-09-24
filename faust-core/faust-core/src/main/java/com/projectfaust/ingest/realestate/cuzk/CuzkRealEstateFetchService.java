package com.projectfaust.ingest.realestate.cuzk;

import com.projectfaust.ingest.realestate.dto.RealEstateOwnershipDto;
import com.projectfaust.shared.enums.CadasterSourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Fetch service stub for the Czech ČÚZK cadaster (Katastr nemovitostí).
 *
 * <p><b>STUB — not yet implemented.</b> The ČÚZK Remote Access API (Dálkový přístup)
 * requires paid registration and SOAP/REST credentials which are not yet available.
 * This service defines the correct structure and Kafka dispatch pattern so the
 * rest of the pipeline (consumer, ingest service, mapper) is ready to receive
 * data the moment real API access is established.</p>
 *
 * <p>When implementing for real:</p>
 * <ol>
 *   <li>Register at {@code https://www.cuzk.cz/Katastr-nemovitosti/Poskytovani-udaju-z-KN/Dalkovy-pristup.aspx}</li>
 *   <li>Obtain SOAP/REST credentials for the ISKN web service.</li>
 *   <li>Replace {@link #fetchPropertiesForNationalId} with real HTTP calls.</li>
 *   <li>Map the ČÚZK response fields to {@link RealEstateOwnershipDto}.</li>
 * </ol>
 *
 * <p>Alternative: Hlidač Státu may expose aggregated cadaster data via their
 * existing API — worth checking since FAUST already has their API token.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuzkRealEstateFetchService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "ingest.realestate.ownership";

    /**
     * Fetches real estate ownership records for the given national ID (IČO or personal ID)
     * from the ČÚZK cadaster and dispatches each to Kafka.
     *
     * <p><b>STUB:</b> currently logs a warning and returns without dispatching anything.
     * Replace the body with real ČÚZK API calls when access is available.</p>
     *
     * @param nationalId the IČO (for institutions) or personal ID (for persons).
     * @param countryCode always "CZ" for this service.
     */
    public void fetchPropertiesForNationalId(String nationalId, String countryCode) {
        log.warn("CUZK_FETCH: STUB — ČÚZK Remote Access API not yet configured. " +
                        "Skipping property lookup for nationalId={} country={}. " +
                        "Register at cuzk.cz to enable real cadaster data ingest.",
                nationalId, countryCode);

        // ---------------------------------------------------------------
        // TODO: replace with real ČÚZK SOAP/REST call when credentials
        // are available. Dispatch pattern when implemented:
        //
        // RealEstateOwnershipDto dto = mapCuzkResponse(cuzkApiResponse);
        // kafkaTemplate.send(TOPIC, dto.externalId(), Map.of(
        //     "sourceSystem", CadasterSourceSystem.CUZK_CZ.name(),
        //     "payload", dto
        // ));
        // ---------------------------------------------------------------
    }
}