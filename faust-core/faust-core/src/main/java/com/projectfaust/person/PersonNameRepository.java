package com.projectfaust.person;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for individual person name records.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface PersonNameRepository extends JpaRepository<PersonName, Long> {

    /**
     * Retrieves a single name record by its public UUID.
     *
     * @param externalId the public UUID of the name record.
     * @return the name record if found.
     */
    Optional<PersonName> findByExternalId(UUID externalId);

    /**
     * Returns all name records for a person, primary name first,
     * then remaining names in creation order.
     *
     * @param personExternalId the public UUID of the person.
     * @return list of name records.
     */
    @Query("SELECT n FROM PersonName n " +
            "WHERE n.person.externalId = :personExternalId " +
            "ORDER BY n.primary DESC, n.createdAt ASC")
    List<PersonName> findAllByPersonExternalIdOrderByPrimaryDescCreatedAtAsc(
            @Param("personExternalId") UUID personExternalId);
}
