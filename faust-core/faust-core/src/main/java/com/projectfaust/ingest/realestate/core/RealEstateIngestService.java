package com.projectfaust.ingest.realestate.core;

import com.projectfaust.shared.enums.CadasterSourceSystem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core ingestion service for real estate ownership records.
 *
 * <p>Source-agnostic — delegates mapping to the registered
 * {@link RealEstateSourceMapper} for the relevant {@link CadasterSourceSystem}.
 * Mirrors {@link com.projectfaust.ingest.core.ContractIngestService} exactly:
 * new countries require only a new mapper implementation, this service
 * requires no changes.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
public class RealEstateIngestService {

    private final RealEstateOwnershipRepository repository;
    private final Map<CadasterSourceSystem, RealEstateSourceMapper<?>> mappersBySource;

    @Autowired
    public RealEstateIngestService(
            RealEstateOwnershipRepository repository,
            List<RealEstateSourceMapper<?>> mappers) {
        this.repository = repository;
        this.mappersBySource = mappers.stream()
                .collect(Collectors.toMap(RealEstateSourceMapper::supports, m -> m));

        log.info("REALESTATE_INGEST: Registered cadaster source mappers for: {}",
                mappersBySource.keySet());
    }

    /**
     * Ingests a single ownership record from the given source.
     * Deduplicates by (sourceSystem, sourceRegistryId).
     *
     * @param source    the cadaster source system.
     * @param sourceDto the raw country-specific DTO.
     * @param <T>       the DTO type.
     * @return {@code true} if persisted, {@code false} if duplicate.
     */
    @Transactional
    @SuppressWarnings("unchecked")
    public <T> boolean ingestOwnership(CadasterSourceSystem source, T sourceDto) {
        RealEstateSourceMapper<T> mapper =
                (RealEstateSourceMapper<T>) mappersBySource.get(source);

        if (mapper == null) {
            throw new IllegalStateException(
                    "REALESTATE_INGEST: No mapper registered for source: " + source);
        }

        RealEstateOwnership entity = mapper.toEntity(sourceDto);

        if (repository.existsBySourceSystemAndSourceRegistryId(
                entity.getSourceSystem(), entity.getSourceRegistryId())) {
            log.debug("REALESTATE_INGEST: Skipping duplicate {} from {}.",
                    entity.getSourceRegistryId(), source);
            return false;
        }

        repository.save(entity);
        log.info("REALESTATE_INGEST: Ingested property {} from {}.",
                entity.getSourceRegistryId(), source);
        return true;
    }
}