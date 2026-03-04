package com.projectfaust.repository;

import com.projectfaust.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing individual subjects (Personnel) within Project Faust.
 * Provides capabilities for identity verification, full-text searching, and
 * deep-fetching of complete professional profiles.
 */
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    /**
     * Retrieves a person based on their global unique identifier.
     *
     * @param externalId The UUID assigned to the subject.
     * @return An Optional containing the found Person, or empty if not found.
     */
    Optional<Person> findByExternalId(UUID externalId);

    /**
     * Retrieves a collection of people for bulk processing using their external IDs.
     *
     * @param externalIds A collection of UUIDs to fetch.
     * @return A list of found individuals.
     */
    List<Person> findAllByExternalIdIn(Collection<UUID> externalIds);

    /**
     * Retrieves a collection of people based on a set of email addresses.
     *
     * @param emails A collection of email strings to match.
     * @return A list of matching individuals.
     */
    List<Person> findAllByEmailIn(Collection<String> emails);

    /**
     * Verifies the existence of a record based on a first and last name combination.
     *
     * @param firstName The subject's first name.
     * @param lastName The subject's last name.
     * @return true if a matching record exists.
     */
    boolean existsByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Verifies if an email address is already registered in the system.
     *
     * @param email The email address to check.
     * @return true if the email is present in the database.
     */
    boolean existsByEmail(String email);

    /**
     * Performs a partial match search using a normalized full-name field.
     * Optimized for UI search bars and autocomplete functionality.
     *
     * @param query The search string (e.g., "John Doe").
     * @return A list of subjects matching the name pattern.
     */
    @Query(value = "SELECT p FROM Person p WHERE p.fullNameSearchNormalized LIKE " +
            "LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByFullName(@Param("query") String query);

    /**
     * Retrieves the complete biographical and professional dossier of a subject.
     * Performs a deep fetch of all past and present appointments, including
     * associated occupations and institutions to prevent N+1 query overhead.
     *
     * @param externalId The unique identifier of the subject.
     * @return An Optional containing the fully initialized profile.
     */
    @Query("SELECT p FROM Person p " +
            "LEFT JOIN FETCH p.appointments a " +
            "LEFT JOIN FETCH a.occupation o " +
            "LEFT JOIN FETCH o.institution i " +
            "WHERE p.externalId = :externalId")
    Optional<Person> findFullProfileByExternalId(@Param("externalId") UUID externalId);
}
