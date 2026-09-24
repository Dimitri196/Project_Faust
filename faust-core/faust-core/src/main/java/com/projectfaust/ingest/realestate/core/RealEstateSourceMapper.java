package com.projectfaust.ingest.realestate.core;

import com.projectfaust.ingest.realestate.dto.RealEstateOwnershipDto;
import com.projectfaust.shared.enums.CadasterSourceSystem;

/**
 * Strategy interface for mapping a country-specific raw cadaster payload
 * into the normalised {@link RealEstateOwnership} entity.
 *
 * <p>Mirrors {@link com.projectfaust.ingest.core.ContractSourceMapper} exactly —
 * one implementation per country/registry, registered as a Spring
 * {@code @Component}, auto-discovered by {@link RealEstateIngestService}
 * via its mapper registry. Adding a new country requires only a new
 * implementation of this interface — the core pipeline is unchanged.</p>
 *
 * <p><b>Adding a new cadaster source:</b></p>
 * <ol>
 *   <li>Add a value to {@link CadasterSourceSystem}.</li>
 *   <li>Create a country-specific fetch service in
 *       {@code ingest.realestate.{countrycode}/}.</li>
 *   <li>Implement this interface, annotated {@code @Component}.</li>
 *   <li>{@link RealEstateIngestService} picks it up automatically.</li>
 * </ol>
 *
 * @param <T> the raw country-specific payload type before normalisation.
 * @author Dimitri / Project Faust
 */
public interface RealEstateSourceMapper<T> {

    /**
     * Returns the cadaster source system this mapper handles.
     *
     * @return the source system identifier.
     */
    CadasterSourceSystem supports();

    /**
     * Maps a raw country-specific payload to a normalised
     * {@link RealEstateOwnership} entity ready for deduplication
     * and persistence.
     *
     * <p>Implementations must NOT set {@code id}, {@code externalId},
     * or {@code ingestedAt} — these are managed by JPA lifecycle
     * callbacks.</p>
     *
     * @param source the raw payload.
     * @return normalised entity.
     */
    RealEstateOwnership toEntity(T source);

    /**
     * Convenience check — does this mapper handle the given source?
     *
     * @param source the source system to check.
     * @return {@code true} if this mapper handles the given source.
     */
    default boolean supportsSource(CadasterSourceSystem source) {
        return supports() == source;
    }
}