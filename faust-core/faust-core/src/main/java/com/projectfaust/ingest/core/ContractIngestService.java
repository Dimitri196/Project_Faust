package com.projectfaust.ingest.core;

import com.projectfaust.shared.enums.SourceSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core ingestion service for external contract records.
 *
 * <p>Source-agnostic — delegates DTO-to-entity mapping to the registered
 * {@link ContractSourceMapper} implementation for the relevant
 * {@link SourceSystem}. New sources require only a new mapper implementation;
 * this service requires no changes.</p>
 *
 * <p><b>Deduplication:</b> records are deduplicated by
 * ({@code sourceSystem}, {@code externalId}). Re-ingesting a known record
 * is a no-op — updates to existing records are not yet supported (v1).</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
public class ContractIngestService {

    private final ExternalContractRepository contractRepository;
    private final Map<SourceSystem, ContractSourceMapper<?>> mappersBySource;

    /**
     * Constructs the service, indexing all {@link ContractSourceMapper} beans
     * by the {@link SourceSystem} they support.
     *
     * @param contractRepository repository for persisted contract records.
     * @param mappers            all registered source mapper implementations.
     */
    @Autowired
    public ContractIngestService(
            ExternalContractRepository contractRepository,
            List<ContractSourceMapper<?>> mappers) {
        this.contractRepository = contractRepository;
        this.mappersBySource = mappers.stream()
                .collect(Collectors.toMap(ContractSourceMapper::supports, m -> m));

        log.info("FAUST_INGEST: Registered contract source mappers for: {}",
                mappersBySource.keySet());
    }

    /**
     * Ingests a single contract record from the given source.
     *
     * <p>If a record with the same ({@code sourceSystem}, {@code externalId})
     * already exists, ingestion is skipped and {@code false} is returned.</p>
     *
     * @param sourceSystem the source system the DTO came from.
     * @param sourceDto    the raw source-specific DTO.
     * @param <T>          the DTO type.
     * @return {@code true} if a new record was persisted, {@code false} if skipped (duplicate).
     * @throws IllegalStateException if no mapper is registered for the given source system.
     * @throws IllegalArgumentException if the DTO type does not match the mapper's expected type.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T> boolean ingestContract(SourceSystem sourceSystem, T sourceDto) {
        ContractSourceMapper<T> mapper = (ContractSourceMapper<T>) mappersBySource.get(sourceSystem);

        if (mapper == null) {
            throw new IllegalStateException(
                    "FAUST_INGEST: No contract mapper registered for source: " + sourceSystem);
        }

        ExternalContract entity = mapper.toEntity(sourceDto);

        if (contractRepository.existsBySourceSystemAndExternalId(
                entity.getSourceSystem(), entity.getExternalId())) {
            log.debug("FAUST_INGEST: Skipping duplicate contract {} from {}.",
                    entity.getExternalId(), sourceSystem);
            return false;
        }

        contractRepository.save(entity);
        log.info("FAUST_INGEST: Ingested contract {} from {}.",
                entity.getExternalId(), sourceSystem);
        return true;
    }

    /**
     * Ingests a batch of contract records from the given source.
     *
     * <p>Each record is processed independently — a failure on one record
     * does not stop processing of the remaining records. Failures are logged
     * and counted in the returned result.</p>
     *
     * @param sourceSystem the source system the DTOs came from.
     * @param sourceDtos   the raw source-specific DTOs.
     * @param <T>          the DTO type.
     * @return an {@link IngestResult} summarising the batch outcome.
     */
    @Transactional
    public <T> IngestResult ingestBatch(SourceSystem sourceSystem, List<T> sourceDtos) {
        int ingested = 0;
        int skipped = 0;
        int failed = 0;

        for (T dto : sourceDtos) {
            try {
                if (ingestContract(sourceSystem, dto)) {
                    ingested++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                failed++;
                log.error("FAUST_INGEST: Failed to ingest record from {} — {}",
                        sourceSystem, e.getMessage());
            }
        }

        log.info("FAUST_INGEST: Batch complete for {} — ingested={}, skipped={}, failed={}, total={}.",
                sourceSystem, ingested, skipped, failed, sourceDtos.size());

        return new IngestResult(sourceSystem, ingested, skipped, failed, sourceDtos.size());
    }

    /**
     * Summary of a batch ingestion operation.
     *
     * @param sourceSystem the source system processed.
     * @param ingested     number of new records persisted.
     * @param skipped      number of records skipped as duplicates.
     * @param failed       number of records that failed to map or persist.
     * @param total        total number of records in the batch.
     */
    public record IngestResult(
            SourceSystem sourceSystem,
            int ingested,
            int skipped,
            int failed,
            int total
    ) {}
}