package com.projectfaust.repository;

import com.projectfaust.entity.PersonConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for managing social, professional, and political relationships (Connections)
 * between individuals within the Faust Intelligence network.
 * Specialized in deep-graph traversal to identify clusters of influence.
 */
@Repository
public interface PersonConnectionRepository extends JpaRepository<PersonConnection, Long> {

    /**
     * Retrieves all connections involving a specific person, either as a source or target.
     * Performs a deep fetch of both subjects, their current appointments, and associated
     * occupations to enable real-time relationship mapping without N+1 query latency.
     *
     * @param id The global unique identifier (UUID) of the subject person.
     * @return A list of connections with fully initialized subject and professional metadata.
     */
    @Query("""
        SELECT pc FROM PersonConnection pc 
        JOIN FETCH pc.sourcePerson sp
        JOIN FETCH pc.targetPerson tp
        LEFT JOIN FETCH tp.appointments app
        LEFT JOIN FETCH app.occupation
        WHERE sp.externalId = :id OR tp.externalId = :id
    """)
    List<PersonConnection> findAllByPersonExternalId(UUID id);
}
