package com.projectfaust.criminal;

import com.projectfaust.criminal.dto.CriminalRecordRequestDto;
import com.projectfaust.criminal.dto.CriminalRecordResponseDto;
import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link CriminalRecord} entities.
 *
 * <p><b>Idempotency:</b> create operations check for an existing record with
 * the same {@code (sourceSystem, sourceReferenceId)} pair and upsert if found.</p>
 *
 * <p><b>Default clearance:</b> criminal records default to
 * {@code LEVEL_3_CONFIDENTIAL} at the entity level — the service does not
 * downgrade this unless the caller explicitly supplies a lower level.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CriminalRecordService {

    private final CriminalRecordRepository repository;
    private final PersonRepository         personRepository;
    private final InstitutionRepository    institutionRepository;
    private final CriminalRecordMapper     mapper;

    // =========================================================================
    // Create
    // =========================================================================

    public CriminalRecordResponseDto create(CriminalRecordRequestDto dto) {
        if (dto.sourceReferenceId() != null) {
            var existing = repository.findBySourceSystemAndSourceReferenceId(
                    dto.sourceSystem(), dto.sourceReferenceId());
            if (existing.isPresent()) {
                log.info("FAUST_CRIMINAL_UPSERT: existing record source={} ref={}; updating.",
                        dto.sourceSystem(), dto.sourceReferenceId());
                return doUpdate(existing.get(), dto);
            }
        }

        CriminalRecord entity = mapper.toEntity(dto);
        resolveAndLinkSubject(entity, dto);

        CriminalRecord saved = repository.save(entity);
        log.info("FAUST_CRIMINAL_CREATE: publicId={} subject={} category={} source={}",
                saved.getExternalId(), saved.getSubjectType(),
                saved.getOffenseCategory(), saved.getSourceSystem());
        return mapper.toResponse(saved);
    }

    public List<CriminalRecordResponseDto> createBulk(List<CriminalRecordRequestDto> dtos) {
        log.info("FAUST_CRIMINAL_BULK: processing {} records.", dtos.size());
        return dtos.stream().map(this::create).toList();
    }

    // =========================================================================
    // Update
    // =========================================================================

    public CriminalRecordResponseDto update(UUID publicId, CriminalRecordRequestDto dto) {
        CriminalRecord entity = findOrThrow(publicId);
        return doUpdate(entity, dto);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID publicId) {
        CriminalRecord entity = findOrThrow(publicId);
        log.info("FAUST_CRIMINAL_DELETE: publicId={}", publicId);
        repository.delete(entity);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public CriminalRecordResponseDto getByPublicId(UUID publicId) {
        return mapper.toResponse(findOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getBySubjectPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findAllBySubjectPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getActiveBySubjectPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findActiveBySubjectPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getBySubjectInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return repository.findAllBySubjectInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getByOffenseCategory(OffenseCategory category) {
        return repository.findByOffenseCategory(category)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getByStatus(CriminalRecordStatus status) {
        return repository.findByStatus(status)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CriminalRecordResponseDto> getAllWanted() {
        return repository.findAllWanted()
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private CriminalRecordResponseDto doUpdate(CriminalRecord entity, CriminalRecordRequestDto dto) {
        mapper.updateEntity(dto, entity);
        resolveAndLinkSubject(entity, dto);
        log.info("FAUST_CRIMINAL_UPDATE: publicId={} updated.", entity.getExternalId());
        return mapper.toResponse(entity);
    }

    private void resolveAndLinkSubject(CriminalRecord entity, CriminalRecordRequestDto dto) {
        if (dto.subjectType() == null) return;
        entity.setSubjectType(dto.subjectType());
        switch (dto.subjectType()) {
            case PERSON -> {
                if (dto.subjectPersonPublicId() != null) {
                    Person p = personRepository.findByExternalId(dto.subjectPersonPublicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Person not found: " + dto.subjectPersonPublicId()));
                    entity.setSubjectPerson(p);
                    entity.setSubjectInstitution(null);
                }
            }
            case INSTITUTION -> {
                if (dto.subjectInstitutionPublicId() != null) {
                    Institution i = institutionRepository.findByExternalId(dto.subjectInstitutionPublicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Institution not found: " + dto.subjectInstitutionPublicId()));
                    entity.setSubjectInstitution(i);
                    entity.setSubjectPerson(null);
                }
            }
        }
    }

    private CriminalRecord findOrThrow(UUID publicId) {
        return repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "CriminalRecord not found: " + publicId));
    }

    private void verifyPersonExists(UUID publicId) {
        if (!personRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Person not found: " + publicId);
    }

    private void verifyInstitutionExists(UUID publicId) {
        if (!institutionRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Institution not found: " + publicId);
    }
}
