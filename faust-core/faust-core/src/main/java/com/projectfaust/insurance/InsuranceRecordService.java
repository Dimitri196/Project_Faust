package com.projectfaust.insurance;

import com.projectfaust.insurance.dto.InsuranceRecordRequestDto;
import com.projectfaust.insurance.dto.InsuranceRecordResponseDto;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.shared.enums.InsuranceType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Core service for managing insurance policy records within Project Faust.
 *
 * <p>Enforces two invariants:</p>
 * <ul>
 *   <li><b>Person binding</b> — every record must be linked to an existing,
 *       resolvable {@link Person}.</li>
 *   <li><b>Idempotent ingest</b> — a {@code (sourceSystem, sourceReferenceId)}
 *       pair that already exists causes an update rather than a duplicate insert.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceRecordService {

    private final InsuranceRecordRepository repository;
    private final PersonRepository personRepository;
    private final InsuranceRecordMapper mapper;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Creates a new insurance record and persists it.
     *
     * <p>Resolves the {@link Person} FK before saving. If a record with the
     * same {@code (sourceSystem, sourceReferenceId)} already exists, delegates
     * to {@link #upsertBySourceReference} instead of inserting a duplicate.</p>
     *
     * @param request the creation DTO.
     * @return the persisted record as a response DTO.
     * @throws EntityNotFoundException if the referenced person does not exist.
     */
    @Transactional
    public InsuranceRecordResponseDto create(InsuranceRecordRequestDto request) {
        log.info("FAUST_INS: Creating insurance record of type {} for person {}",
                request.insuranceType(), request.personPublicId());

        // Idempotency guard — delegate to upsert if we already have this record
        if (request.sourceReferenceId() != null && request.sourceSystem() != null) {
            return repository.findBySourceSystemAndSourceReferenceId(
                            request.sourceSystem(), request.sourceReferenceId())
                    .map(existing -> {
                        log.info("FAUST_INS: Record already exists ({} / {}), updating.",
                                request.sourceSystem(), request.sourceReferenceId());
                        mapper.updateEntity(request, existing);
                        return mapper.toResponse(repository.save(existing));
                    })
                    .orElseGet(() -> persistNew(request));
        }

        return persistNew(request);
    }

    /**
     * Creates multiple insurance records within a single database transaction.
     *
     * <p>Each record is individually subject to idempotency logic — existing
     * records are updated, new ones are inserted. The entire batch rolls back
     * if any single item fails validation.</p>
     *
     * @param requests list of creation DTOs.
     * @return list of persisted records as response DTOs.
     */
    @Transactional
    public List<InsuranceRecordResponseDto> createBulk(List<InsuranceRecordRequestDto> requests) {
        log.info("FAUST_INS_BULK: Processing {} insurance record requests.", requests.size());
        return requests.stream()
                .map(this::create)
                .toList();
    }

    /**
     * Updates an existing insurance record identified by its public UUID.
     *
     * <p>Applies a partial merge — only non-null fields in the request DTO
     * overwrite the corresponding fields on the entity.</p>
     *
     * @param publicId the public UUID of the record to update.
     * @param request  the partial update DTO.
     * @return the updated record as a response DTO.
     * @throws EntityNotFoundException if no record matches the given UUID.
     */
    @Transactional
    public InsuranceRecordResponseDto update(UUID publicId, InsuranceRecordRequestDto request) {
        log.info("FAUST_INS: Updating insurance record: {}", publicId);

        InsuranceRecord entity = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSURANCE_RECORD_NOT_FOUND: " + publicId));

        // Re-link person if a new personPublicId is supplied
        if (request.personPublicId() != null &&
                !request.personPublicId().equals(entity.getPerson().getExternalId())) {
            Person person = personRepository.findByExternalId(request.personPublicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "PERSON_NOT_FOUND: " + request.personPublicId()));
            entity.setPerson(person);
        }

        mapper.updateEntity(request, entity);
        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Deletes an insurance record by its public UUID.
     *
     * @param publicId the public UUID of the record to delete.
     * @throws EntityNotFoundException if no record matches the given UUID.
     */
    @Transactional
    public void delete(UUID publicId) {
        log.info("FAUST_INS: Deleting insurance record: {}", publicId);
        InsuranceRecord entity = repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("INSURANCE_RECORD_NOT_FOUND: " + publicId));
        repository.delete(entity);
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    /**
     * Retrieves a single insurance record by its public UUID.
     *
     * @param publicId the public UUID of the record.
     * @return the record as a response DTO.
     * @throws EntityNotFoundException if no record matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InsuranceRecordResponseDto getByPublicId(UUID publicId) {
        log.info("FAUST_INS: Fetching insurance record: {}", publicId);
        return repository.findByExternalId(publicId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("INSURANCE_RECORD_NOT_FOUND: " + publicId));
    }

    /**
     * Returns all insurance records linked to the specified person.
     *
     * @param personPublicId the public UUID of the person.
     * @return list of all records for that person, ordered newest-first.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public List<InsuranceRecordResponseDto> getByPerson(UUID personPublicId) {
        log.info("FAUST_INS: Fetching all insurance records for person: {}", personPublicId);
        verifyPersonExists(personPublicId);
        return mapper.toResponseList(repository.findAllByPersonExternalId(personPublicId));
    }

    /**
     * Returns all active insurance records linked to the specified person.
     *
     * @param personPublicId the public UUID of the person.
     * @return list of currently active policies for that person.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public List<InsuranceRecordResponseDto> getActiveByPerson(UUID personPublicId) {
        log.info("FAUST_INS: Fetching active insurance records for person: {}", personPublicId);
        verifyPersonExists(personPublicId);
        return mapper.toResponseList(repository.findActiveByPersonExternalId(personPublicId));
    }

    /**
     * Returns all insurance records of a specific type for the given person.
     *
     * @param personPublicId the public UUID of the person.
     * @param insuranceType  the policy type to filter by.
     * @return list of matching records.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public List<InsuranceRecordResponseDto> getByPersonAndType(
            UUID personPublicId, InsuranceType insuranceType) {
        log.info("FAUST_INS: Fetching {} records for person: {}", insuranceType, personPublicId);
        verifyPersonExists(personPublicId);
        return mapper.toResponseList(
                repository.findByPersonExternalIdAndType(personPublicId, insuranceType));
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the Person FK and persists a new entity.
     */
    private InsuranceRecordResponseDto persistNew(InsuranceRecordRequestDto request) {
        InsuranceRecord entity = mapper.toEntity(request);

        Person person = personRepository.findByExternalId(request.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NOT_FOUND: " + request.personPublicId()));
        entity.setPerson(person);

        return mapper.toResponse(repository.save(entity));
    }

    /**
     * Throws {@link EntityNotFoundException} if the person UUID cannot be resolved.
     */
    private void verifyPersonExists(UUID personPublicId) {
        if (!personRepository.existsByExternalId(personPublicId)) {
            throw new EntityNotFoundException("PERSON_NOT_FOUND: " + personPublicId);
        }
    }
}