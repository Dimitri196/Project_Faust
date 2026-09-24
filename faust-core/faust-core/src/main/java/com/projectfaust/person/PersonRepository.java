package com.projectfaust.person;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Person} entities within Project Faust.
 *
 * <p>Provides three categories of queries:</p>
 * <ul>
 *   <li><b>Technical footprint</b> — contact cross-reference, IMEI device tracking,
 *       and IBAN link analysis for SIGINT and FININT operations.</li>
 *   <li><b>Identity resolution</b> — name-based search across the full historical
 *       alias registry and the normalised full-name search column.</li>
 *   <li><b>Graph hydration</b> — optimised eager-fetch queries that eliminate N+1
 *       overhead when loading full person dossiers for the SPA.</li>
 * </ul>
 *
 * <p><b>ID contract:</b> all public-facing lookups use {@code externalId} (UUID).
 * The internal {@code Long} primary key is never exposed outside the persistence layer.</p>
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface PersonRepository extends
        JpaRepository<Person, Long>,
        JpaSpecificationExecutor<Person> {

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Checks whether a person with the given public UUID exists.
     *
     * @param externalId the public UUID to check.
     * @return {@code true} if a matching person exists.
     */
    boolean existsByExternalId(UUID externalId);

    // -------------------------------------------------------------------------
    // Single entity lookups
    // -------------------------------------------------------------------------

    /**
     * Retrieves a person by their public UUID.
     *
     * @param externalId the public UUID of the person.
     * @return an {@link Optional} containing the person, or empty if not found.
     */
    Optional<Person> findByExternalId(UUID externalId);

    /**
     * Retrieves a complete person dossier with all identity, contact, and financial
     * data eagerly loaded in a single database round trip.
     *
     * <p>Eliminates N+1 query overhead when rendering the full dossier view.
     * Loads: names, contacts, financial account relations, and underlying bank accounts.</p>
     *
     * @param externalId the public UUID of the person.
     * @return an {@link Optional} containing the fully-loaded person dossier.
     */
    @Query("SELECT DISTINCT p FROM Person p " +
            "LEFT JOIN FETCH p.names n " +
            "LEFT JOIN FETCH p.contacts c " +
            "LEFT JOIN FETCH p.financialAccounts fa " +
            "LEFT JOIN FETCH fa.bankAccount ba " +
            "WHERE p.externalId = :externalId")
    Optional<Person> findFullProfileByExternalId(@Param("externalId") UUID externalId);

    // -------------------------------------------------------------------------
    // Bulk lookups
    // -------------------------------------------------------------------------

    /**
     * Resolves a collection of persons by their public UUIDs.
     * Used by bulk appointment processing to pre-fetch all required persons
     * in a single query.
     *
     * @param externalIds collection of public UUIDs to fetch.
     * @return list of found persons.
     */
    List<Person> findAllByExternalIdIn(Collection<UUID> externalIds);

    // -------------------------------------------------------------------------
    // Technical footprint — SIGINT / FININT queries
    // -------------------------------------------------------------------------

    /**
     * Identifies all persons associated with any of the provided normalised contact values.
     *
     * <p>Critical for batch intelligence operations — cross-references a leaked
     * contact dataset against all known subjects in a single query.</p>
     *
     * @param values collection of normalised contact values (phone numbers, emails, etc.).
     * @return list of persons whose contact records match any of the provided values.
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.contacts c " +
            "WHERE c.contactValueNormalized IN :values")
    List<Person> findAllByNormalizedContactsIn(@Param("values") Collection<String> values);

    /**
     * Identifies all persons associated with a specific hardware device by IMEI.
     *
     * <p>Multiple persons sharing an IMEI is a high-value SIGINT signal — indicating
     * phone sharing, device hand-off, or cover identity usage.</p>
     *
     * @param imei the IMEI of the target device.
     * @return list of persons whose contact records include the given IMEI.
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.contacts c WHERE c.imei = :imei")
    List<Person> findAllByHardwareImei(@Param("imei") String imei);

    /**
     * Checks whether a person with a specific normalised contact value exists.
     *
     * <p>Used for deduplication before ingesting new contact vectors.</p>
     *
     * @param type            the contact channel type.
     * @param normalizedValue the normalised contact value to check.
     * @return {@code true} if a matching contact record exists.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.contacts c " +
            "WHERE c.contactType = :type AND c.contactValueNormalized = :normalizedValue")
    boolean existsByContactVector(
            @Param("type") ContactType type,
            @Param("normalizedValue") String normalizedValue);

    /**
     * Finds all persons linked to a specific bank account by IBAN.
     *
     * <p>Used for cross-link financial analysis (FININT) — surfaces all persons
     * connected to a given account regardless of their role type.</p>
     *
     * @param iban the IBAN of the target bank account.
     * @return list of persons with a financial relationship to the given IBAN.
     */
    @Query("SELECT DISTINCT p FROM Person p " +
            "JOIN p.financialAccounts fa " +
            "JOIN fa.bankAccount ba " +
            "WHERE ba.iban = :iban")
    List<Person> findAllByLinkedIban(@Param("iban") String iban);

    // -------------------------------------------------------------------------
    // Identity and name resolution
    // -------------------------------------------------------------------------

    /**
     * Checks whether a person with the given name exists in any name record —
     * primary name, alias, cover name, or historical name.
     *
     * <p>Case-insensitive. Used for deduplication before creating new persons.</p>
     *
     * @param firstName the first name to check.
     * @param lastName  the last name to check.
     * @return {@code true} if any name record matches.
     */
    @Query("SELECT COUNT(p) > 0 FROM Person p JOIN p.names n " +
            "WHERE LOWER(n.firstName) = LOWER(:firstName) " +
            "AND LOWER(n.lastName) = LOWER(:lastName)")
    boolean existsByName(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName);

    /**
     * Full-text search using the database-generated normalised name column.
     *
     * <p>Leverages a DB-level trigger that aggregates and normalises name data
     * into a single searchable column, enabling diacritic-insensitive search
     * optimised for high-speed SPA autocomplete.</p>
     *
     * @param query the search term.
     * @return list of persons whose normalised name contains the query.
     */
    @Query("SELECT p FROM Person p " +
            "WHERE p.fullNameSearchNormalized LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByFullName(@Param("query") String query);

    /**
     * Deep alias search across the complete un-aggregated name history table.
     *
     * <p>Uncovers persons using historic aliases, cover names, or maiden names
     * that may not appear in the normalised search column. Slower than
     * {@link #searchByFullName} but comprehensive.</p>
     *
     * @param query the search term.
     * @return list of persons with any name record matching the query.
     */
    @Query("SELECT DISTINCT p FROM Person p JOIN p.names n " +
            "WHERE LOWER(n.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(n.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Person> searchByAnyName(@Param("query") String query);

    /**
     * Returns all persons with the specified clearance level.
     *
     * @param level the clearance level to filter by.
     * @return list of persons at the given clearance level.
     */
    @Query("SELECT p FROM Person p WHERE p.clearanceLevel = :level")
    List<Person> findAllByClearanceLevel(@Param("level") ClearanceLevel level);
}