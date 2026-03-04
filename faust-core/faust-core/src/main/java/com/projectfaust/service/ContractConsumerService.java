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

/**
 * Service responsible for consuming and processing contract data from Kafka topics.
 * Handles deserialization, validation, and persistence into the relational database.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractConsumerService {

    private final ExternalContractRepository repository;

    /**
     * Listens to the raw contracts topic and processes incoming byte arrays.
     * Performs idempotency checks before saving to prevent duplicate entries.
     *
     * @param rawData The raw byte array received from the Kafka broker.
     */
    @KafkaListener(
            topics = "${faust.topics.raw-contracts}",
            groupId = "faust-v4"
    )
    public void consumeContract(byte[] rawData) {
        log.info(">>>> KAFKA_DEBUG: Received raw data from Kafka.");

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            mapper.findAndRegisterModules();

            ContractDto dto = mapper.readValue(rawData, ContractDto.class);

            log.info(">>>> KAFKA_MAPPER: Mapped ID: {}", dto.id());

            if (dto.id() == null) {
                log.warn("!!!! WARNING: ID is still null. Check the ContractDto structure.");
                return;
            }

            if (repository.existsByExternalId(dto.id())) {
                log.info("Contract {} already exists in DB, skipping.", dto.id());
                return;
            }

            ExternalContract entity = ExternalContract.builder()
                    .externalId(dto.id())
                    .subjectText(dto.subject())
                    .amountTotal(BigDecimal.valueOf(dto.priceWithoutVat() != null ? dto.priceWithoutVat() : 0.0))
                    .buyerIco(dto.buyer() != null ? dto.buyer().ico() : null)
                    .supplierIco((dto.suppliers() != null && !dto.suppliers().isEmpty())
                            ? dto.suppliers().get(0).ico()
                            : null)
                    .build();

            if (dto.dateConfirmed() != null && dto.dateConfirmed().length() >= 10) {
                entity.setContractDate(LocalDate.parse(dto.dateConfirmed().substring(0, 10)));
            }

            repository.save(entity);
            log.info("[DB_SUCCESS] Contract {} saved to PostgreSQL.", dto.id());

        } catch (Exception e) {
            log.error("!!!! CONSUMER ERROR: {}", e.getMessage());
            log.error("Raw JSON for inspection: {}", new String(rawData));
        }
    }
}
