package com.projectfaust.medical;

import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.medical.dto.MedicalRecordRequestDto;
import com.projectfaust.medical.dto.MedicalRecordResponseDto;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link MedicalRecord} entities.
 *
 * <p><b>Clearance:</b> this service does not enforce clearance-level access
 * control — that is the responsibility of the API gateway / security layer.
 * All records default to {@code LEVEL_4_SECRET}; any downgrade must be
 * explicitly supplied in the request DTO.</p>
 *
 * <p><b>Idempotency:</b> upserts on {@code (sourceSystem, sourceReferenceId)}
 * follow the same pattern as all other Faust modules.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordService {

    private final MedicalRecordRepository repository;
    private final PersonRepository        personRepository;
    private final InstitutionRepository   institutionRepository;
    private final MedicalRecordMapper     mapper;

    // =========================================================================
    // Create
    // =========================================================================

    public MedicalRecordResponseDto create(MedicalRecordRequestDto dto) {
        if (dto.sourceReferenceId() != null) {
            var existing = repository.findBySourceSystemAndSourceReferenceId(
                    dto.sourceSystem(), dto.sourceReferenceId());
            if (existing.isPresent()) {
                log.info("FAUST_MEDICAL_UPSERT: existing record source={} ref={}; updating.",
                        dto.sourceSystem(), dto.sourceReferenceId());
                return doUpdate(existing.get(), dto);
            }
        }

        MedicalRecord entity = mapper.toEntity(dto);
        resolveAndLinkSubject(entity, dto);

        MedicalRecord saved = repository.save(entity);
        log.info("FAUST_MEDICAL_CREATE: publicId={} type={} subject={} source={}",
                saved.getExternalId(), saved.getRecordType(),
                saved.getSubjectType(), saved.getSourceSystem());
        return mapper.toResponse(saved);
    }

    public List<MedicalRecordResponseDto> createBulk(List<MedicalRecordRequestDto> dtos) {
        log.info("FAUST_MEDICAL_BULK: processing {} records.", dtos.size());
        return dtos.stream().map(this::create).toList();
    }

    // =========================================================================
    // Update
    // =========================================================================

    public MedicalRecordResponseDto update(UUID publicId, MedicalRecordRequestDto dto) {
        MedicalRecord entity = findOrThrow(publicId);
        return doUpdate(entity, dto);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID publicId) {
        MedicalRecord entity = findOrThrow(publicId);
        log.info("FAUST_MEDICAL_DELETE: publicId={}", publicId);
        repository.delete(entity);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public MedicalRecordResponseDto getByPublicId(UUID publicId) {
        return mapper.toResponse(findOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getBySubjectPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findAllBySubjectPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getBySubjectPersonAndType(
            UUID personPublicId, MedicalRecordType type) {
        verifyPersonExists(personPublicId);
        return repository.findBySubjectPersonAndType(personPublicId, type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getBySubjectInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return repository.findAllBySubjectInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getByRecordType(MedicalRecordType type) {
        return repository.findByRecordType(type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getByConditionCategory(MedicalConditionCategory category) {
        return repository.findByConditionCategory(category)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns fitness-for-duty records for a person that have not yet expired.
     * Used to answer "does this person currently hold a valid fitness clearance?".
     */
    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getActiveFitnessAssessments(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findActiveFitnessAssessmentsByPerson(personPublicId, LocalDate.now())
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordResponseDto> getByIcdCodePrefix(String icdPrefix) {
        return repository.findByIcdCodePrefix(icdPrefix.toUpperCase())
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private MedicalRecordResponseDto doUpdate(MedicalRecord entity, MedicalRecordRequestDto dto) {
        mapper.updateEntity(dto, entity);
        resolveAndLinkSubject(entity, dto);
        log.info("FAUST_MEDICAL_UPDATE: publicId={} updated.", entity.getExternalId());
        return mapper.toResponse(entity);
    }

    private void resolveAndLinkSubject(MedicalRecord entity, MedicalRecordRequestDto dto) {
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

    private MedicalRecord findOrThrow(UUID publicId) {
        return repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "MedicalRecord not found: " + publicId));
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
