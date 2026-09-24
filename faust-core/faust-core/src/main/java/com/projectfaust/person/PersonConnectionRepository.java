package com.projectfaust.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link PersonConnection} entities — the edges of the
 * HUMINT relationship graph within Project Faust.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface PersonConnectionRepository extends JpaRepository<PersonConnection, Long> {

    // -------------------------------------------------------------------------
    // Existence checks and single lookups
    // -------------------------------------------------------------------------

    /**
     * Checks whether a connection with the given public UUID exists.
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching connection exists.
     */
    boolean existsByExternalId(UUID externalId);

    /**
     * Retrieves a connection by its public UUID.
     *
     * @param externalId the public UUID of the connection.
     * @return an {@link Optional} containing the connection, or empty if not found.
     */
    Optional<PersonConnection> findByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Graph queries
    // -------------------------------------------------------------------------

    /**
     * Retrieves all connections involving a specific person, either as source or target.
     *
     * <p>Performs a deep eager fetch of both persons, their active appointments,
     * and associated occupations in a single query — enabling real-time relationship
     * mapping without N+1 latency when building the influence graph.</p>
     *
     * @param id the public UUID of the subject person.
     * @return list of connections with fully-loaded subject and occupational metadata.
     */
    @Query("""
            SELECT pc FROM PersonConnection pc
            JOIN FETCH pc.sourcePerson sp
            JOIN FETCH pc.targetPerson tp
            LEFT JOIN FETCH tp.appointments app
            LEFT JOIN FETCH app.occupation
            WHERE sp.externalId = :id OR tp.externalId = :id
            """)
    List<PersonConnection> findAllByPersonExternalId(@Param("id") UUID id);

    /**
     * Retrieves all connections where the specified person is the source node.
     *
     * @param sourcePersonId the public UUID of the source person.
     * @return list of outgoing connections from the person.
     */
    List<PersonConnection> findBySourcePersonExternalId(UUID sourcePersonId);

    /**
     * Retrieves all connections where the specified person is the target node.
     *
     * @param targetPersonId the public UUID of the target person.
     * @return list of incoming connections to the person.
     */
    List<PersonConnection> findByTargetPersonExternalId(UUID targetPersonId);
}