package com.projectfaust.person.document;

import com.projectfaust.shared.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PersonDocument} persistence.
 *
 * @author Dimitri / Project Faust
 */
@Repository
public interface PersonDocumentRepository extends JpaRepository<PersonDocument, Long> {

    Optional<PersonDocument> findByExternalId(UUID externalId);

    /** All documents for a person — used by the person dossier panel. */
    List<PersonDocument> findAllByPersonExternalIdOrderByValidToDesc(UUID personExternalId);

    /** Active documents only. */
    @Query("SELECT d FROM PersonDocument d " +
            "WHERE d.person.externalId = :personId AND d.active = true " +
            "ORDER BY d.validTo DESC NULLS LAST")
    List<PersonDocument> findActiveByPersonExternalId(@Param("personId") UUID personId);

    /** Cross-reference lookup — find person by document number. */
    @Query("SELECT d FROM PersonDocument d " +
            "WHERE d.documentNumberNormalized = :number " +
            "AND d.documentType = :type")
    List<PersonDocument> findByDocumentNumberAndType(
            @Param("number") String number,
            @Param("type") DocumentType type);

    /** Find all documents by issuing state — useful for jurisdiction analysis. */
    List<PersonDocument> findAllByIssuingStateAndActiveTrue(String issuingState);

    /** Check for duplicate document numbers across all persons. */
    @Query("SELECT d FROM PersonDocument d " +
            "WHERE d.documentNumberNormalized = :number " +
            "AND d.person.externalId != :personId")
    List<PersonDocument> findDuplicatesAcrossPersons(
            @Param("number")   String number,
            @Param("personId") UUID personId);
}