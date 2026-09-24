package com.projectfaust.network;

import com.projectfaust.person.PersonConnection;
import com.projectfaust.person.PersonConnectionMapper;
import com.projectfaust.person.PersonConnectionRepository;
import com.projectfaust.person.PersonRepository;

import com.projectfaust.person.dto.PersonConnectionRequest;
import com.projectfaust.person.dto.PersonConnectionResponse;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD service for {@link PersonConnection} — directed edges of the HUMINT graph.
 *
 * <p>Handles creation, update, soft-deactivation, and deletion of connections.
 * Graph analytics are delegated to {@link NetworkGraphService}.</p>
 *
 * <p>Duplicate guard: a connection between the same (source, target, type) triple
 * is rejected — one connection of a given type between two persons is semantically
 * sufficient. If a second connection of the same type is needed (e.g. both were
 * BUSINESS_PARTNER and POLITICAL_ALLY), two separate records are valid.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonConnectionService {

    private final PersonConnectionRepository connectionRepository;
    private final PersonRepository           personRepository;
    private final PersonConnectionMapper     mapper;

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a directed connection between two persons.
     *
     * <p>Defaults {@code verificationStatus} to {@link VerificationStatus#PENDING_REVIEW}
     * if not supplied. Defaults {@code influenceScore} to the connection type's
     * {@code defaultWeight} if null.</p>
     *
     * @param request the connection creation request.
     * @return the persisted connection as a response DTO.
     */
    public PersonConnectionResponse create(PersonConnectionRequest request) {
        verifyPersonExists(request.sourcePersonId());
        verifyPersonExists(request.targetPersonId());

        // Duplicate guard
        if (isDuplicate(request.sourcePersonId(), request.targetPersonId(), request.connectionType())) {
            log.warn("FAUST_NETWORK_CONN_DUP: source={} target={} type={}",
                    request.sourcePersonId(), request.targetPersonId(), request.connectionType());
            throw new IllegalStateException(
                    String.format("Connection of type %s already exists between %s and %s",
                            request.connectionType(), request.sourcePersonId(), request.targetPersonId()));
        }

        PersonConnection entity = mapper.toEntity(request);

        // Apply defaults
        if (entity.getVerificationStatus() == null) {
            entity.setVerificationStatus(VerificationStatus.PENDING_REVIEW);
        }

        PersonConnection saved = connectionRepository.save(entity);
        log.info("FAUST_NETWORK_CONN_CREATE: id={} source={} target={} type={}",
                saved.getExternalId(), request.sourcePersonId(),
                request.targetPersonId(), request.connectionType());

        return mapper.toResponse(saved);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates a connection's mutable fields: description, influence score,
     * start/end dates, and verification status.
     *
     * <p>Source, target, and connection type are immutable after creation —
     * changing those would constitute a different connection. Delete and recreate
     * if a type change is needed.</p>
     *
     * @param connectionPublicId the public UUID of the connection.
     * @param request            the update request.
     * @return the updated connection as a response DTO.
     */
    public PersonConnectionResponse update(UUID connectionPublicId, PersonConnectionRequest request) {
        PersonConnection entity = findOrThrow(connectionPublicId);

        // Only update mutable fields
        if (request.description() != null)       entity.setDescription(request.description());
        if (request.influenceScore() != null)     entity.setInfluenceScore(request.influenceScore());
        if (request.startDate() != null)          entity.setStartDate(request.startDate());
        if (request.endDate() != null)            entity.setEndDate(request.endDate());
        if (request.verificationStatus() != null) entity.setVerificationStatus(request.verificationStatus());

        log.info("FAUST_NETWORK_CONN_UPDATE: id={}", connectionPublicId);
        return mapper.toResponse(entity);
    }

    // =========================================================================
    // Soft deactivate
    // =========================================================================

    /**
     * Soft-deactivates a connection by setting its {@code endDate} to today.
     *
     * <p>The record is retained for audit and historical analysis. Use
     * {@link #delete} for permanent removal.</p>
     *
     * @param connectionPublicId the public UUID of the connection.
     * @return the updated connection.
     */
    public PersonConnectionResponse deactivate(UUID connectionPublicId) {
        PersonConnection entity = findOrThrow(connectionPublicId);
        entity.setEndDate(java.time.LocalDate.now());
        log.info("FAUST_NETWORK_CONN_DEACTIVATE: id={}", connectionPublicId);
        return mapper.toResponse(entity);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Permanently deletes a connection.
     *
     * <p>Prefer {@link #deactivate} for audit-trail preservation.</p>
     *
     * @param connectionPublicId the public UUID of the connection to delete.
     */
    public void delete(UUID connectionPublicId) {
        PersonConnection entity = findOrThrow(connectionPublicId);
        connectionRepository.delete(entity);
        log.info("FAUST_NETWORK_CONN_DELETE: id={}", connectionPublicId);
    }

    // =========================================================================
    // Read
    // =========================================================================

    /**
     * Returns a single connection by its public UUID.
     *
     * @param connectionPublicId the public UUID.
     * @return the connection response DTO.
     */
    @Transactional(readOnly = true)
    public PersonConnectionResponse getById(UUID connectionPublicId) {
        return mapper.toResponse(findOrThrow(connectionPublicId));
    }

    /**
     * Returns all connections (outgoing + incoming) for a given person.
     *
     * @param personPublicId the public UUID of the subject person.
     * @return list of all connections involving this person.
     */
    @Transactional(readOnly = true)
    public List<PersonConnectionResponse> getAllByPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return connectionRepository.findAllByPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns only outgoing connections from a person (source = subject).
     *
     * @param personPublicId the public UUID of the source person.
     * @return list of outgoing connections.
     */
    @Transactional(readOnly = true)
    public List<PersonConnectionResponse> getOutgoing(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return connectionRepository.findBySourcePersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns only incoming connections to a person (target = subject).
     *
     * @param personPublicId the public UUID of the target person.
     * @return list of incoming connections.
     */
    @Transactional(readOnly = true)
    public List<PersonConnectionResponse> getIncoming(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return connectionRepository.findByTargetPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private PersonConnection findOrThrow(UUID publicId) {
        return connectionRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PersonConnection not found: " + publicId));
    }

    private void verifyPersonExists(UUID publicId) {
        if (!personRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Person not found: " + publicId);
    }

    private boolean isDuplicate(UUID sourceId, UUID targetId,
                                com.projectfaust.shared.enums.ConnectionType type) {
        return connectionRepository.findAllByPersonExternalId(sourceId).stream()
                .anyMatch(c ->
                        c.getSourcePerson().getExternalId().equals(sourceId)
                        && c.getTargetPerson().getExternalId().equals(targetId)
                        && c.getConnectionType() == type);
    }
}
