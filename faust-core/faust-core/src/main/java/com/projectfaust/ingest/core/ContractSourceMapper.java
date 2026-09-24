package com.projectfaust.ingest.core;

import com.projectfaust.shared.enums.SourceSystem;

/**
 * Strategy interface for mapping a source-specific contract DTO into the
 * normalised {@link ExternalContract} entity.
 *
 * <p>Each {@link SourceSystem} has exactly one implementation. This isolates
 * country-specific field names, aliases, and quirks (e.g. Czech field names
 * from Hlidac Statu) from the rest of the ingest pipeline, which only deals
 * with the normalised entity.</p>
 *
 * <p><b>Adding a new source:</b></p>
 * <ol>
 *   <li>Add a value to {@link SourceSystem}.</li>
 *   <li>Create a source-specific DTO in {@code ingest.dto.&lt;source&gt;}.</li>
 *   <li>Implement this interface, registered as a Spring {@code @Component}.</li>
 *   <li>{@link ContractIngestService} automatically picks up the new mapper
 *       via {@link #supports()}.</li>
 * </ol>
 *
 * @param <T> the source-specific DTO type this mapper handles.
 * @author Dimitri / Project Faust
 */
public interface ContractSourceMapper<T> {

    /**
     * Returns the {@link SourceSystem} this mapper handles.
     *
     * @return the source system identifier.
     */
    SourceSystem supports();

    /**
     * Maps a source-specific DTO to a normalised {@link ExternalContract} entity.
     *
     * <p>Implementations should NOT set {@link ExternalContract#getId()},
     * {@link ExternalContract#getIngestedAt()}, or
     * {@link ExternalContract#getUpdatedAt()} — these are managed by JPA
     * lifecycle callbacks and the persistence layer.</p>
     *
     * @param source the raw source DTO.
     * @return a normalised entity ready for deduplication and persistence.
     */
    ExternalContract toEntity(T source);

    /**
     * Convenience check used by {@link ContractIngestService} to determine
     * whether this mapper applies to a given source.
     *
     * @param sourceSystem the source system to check.
     * @return {@code true} if this mapper handles the given source.
     */
    default boolean supportsSource(SourceSystem sourceSystem) {
        return supports() == sourceSystem;
    }
}