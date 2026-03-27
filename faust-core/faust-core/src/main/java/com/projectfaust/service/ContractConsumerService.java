package com.projectfaust.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectfaust.dto.external.ContractDto;
import com.projectfaust.entity.ExternalContract;
import com.projectfaust.repository.ExternalContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractConsumerService {

    private final ExternalContractRepository repository;

    /**
     * Listens to the raw contracts topic and processes incoming byte arrays.
     * Enhanced with fallback mapping for supplier identification.
     */
    @KafkaListener(
            topics = "${faust.topics.raw-contracts}",
            groupId = "faust-v4"
    )
    public void consumeContract(byte[] rawData) {
        log.info(">>>> KAFKA_DEBUG: Intercepting byte stream from cluster.");

        try {
            ObjectMapper mapper = new ObjectMapper();
            // Zajistíme robustní deserializaci
            mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.findAndRegisterModules();

            ContractDto dto = mapper.readValue(rawData, ContractDto.class);

            if (dto.id() == null) {
                log.error("[CONSUMER_ABORT] Received packet with NULL External_ID. Source data corrupted.");
                return;
            }

            // Kontrola idempotence (duplicity)
            if (repository.existsByExternalId(dto.id())) {
                log.info("[SKIP] Contract {} already indexed in Faust_DB.", dto.id());
                return;
            }

            // --- ROBUST SUPPLIER IDENTIFICATION ---
            String supplierIco = null;
            String buyerIco = (dto.buyer() != null) ? dto.buyer().ico() : null;

            // 1. Pokus: Přímé pole 'Dodavatele' (nebo alias 'Prijemce')
            if (dto.suppliers() != null && !dto.suppliers().isEmpty()) {
                supplierIco = dto.suppliers().get(0).ico();
            }

            // 2. Pokus: Pokud je null, prohledáme 'SmluvniStrany' (Hlídač Státu fallback)
            if (supplierIco == null && dto.allParties() != null) {
                log.info("[MAPPING_FALLBACK] Attempting to extract supplier from SmluvniStrany for ID: {}", dto.id());
                supplierIco = dto.allParties().stream()
                        .filter(p -> p.ico() != null)
                        .filter(p -> !p.ico().equals(buyerIco)) // Eliminujeme plátce
                        .map(ContractDto.InstitutionInfo::ico)
                        .findFirst()
                        .orElse(null);
            }

            // Logování výsledku pro ladění
            if (supplierIco == null) {
                log.warn("[DATA_GAP] Unable to identify Supplier_ICO for contract: {}. Node will be saved with NULL_SUPPLIER.", dto.id());
            }

            // --- ENTITY CONSTRUCTION ---
            ExternalContract entity = ExternalContract.builder()
                    .externalId(dto.id())
                    .subjectText(dto.subject())
                    .amountTotal(BigDecimal.valueOf(dto.priceWithoutVat() != null ? dto.priceWithoutVat() : 0.0))
                    .buyerIco(buyerIco)
                    .supplierIco(supplierIco)
                    .build();

            // Parsování data s ochranou proti špatnému formátu
            if (dto.dateConfirmed() != null && dto.dateConfirmed().length() >= 10) {
                try {
                    entity.setContractDate(LocalDate.parse(dto.dateConfirmed().substring(0, 10)));
                } catch (Exception e) {
                    log.warn("[DATE_PARSE_ERROR] Invalid date format: {}", dto.dateConfirmed());
                }
            }

            repository.save(entity);
            log.info("[DB_SUCCESS] Node {} ingested. Buyer: {}, Supplier: {}, Amount: {}",
                    dto.id(), buyerIco, supplierIco, entity.getAmountTotal());

        } catch (Exception e) {
            log.error("[CRITICAL_CONSUMER_FAILURE] Mapping or DB error: {}", e.getMessage());
            log.debug("Raw Payload for forensic analysis: {}", new String(rawData));
        }
    }
}