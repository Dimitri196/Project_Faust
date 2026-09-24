package com.projectfaust.mobility;

import com.projectfaust.mobility.dto.MobilityEventRequestDto;
import com.projectfaust.mobility.dto.MobilityEventResponseDto;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.vehicle.VehicleRecordRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link MobilityEvent} entities.
 *
 * <p><b>Subject linking:</b> unlike the XOR modules, both person and vehicle
 * FKs are resolved independently. Either may be null, and neither blocks
 * the other from being linked.</p>
 *
 * <p><b>ANPR retroactive linking:</b> when a vehicle cannot be resolved at
 * ingest time, {@code licensePlateRaw} is stored for later manual linking via
 * {@link #update(UUID, MobilityEventRequestDto)} once the vehicle is ingested.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MobilityEventService {

    private final MobilityEventRepository repository;
    private final PersonRepository        personRepository;
    private final VehicleRecordRepository vehicleRepository;
    private final MobilityEventMapper     mapper;

    // =========================================================================
    // Create
    // =========================================================================

    public MobilityEventResponseDto create(MobilityEventRequestDto dto) {
        if (dto.sourceReferenceId() != null) {
            var existing = repository.findBySourceSystemAndSourceReferenceId(
                    dto.sourceSystem(), dto.sourceReferenceId());
            if (existing.isPresent()) {
                log.info("FAUST_MOBILITY_UPSERT: existing event source={} ref={}; updating.",
                        dto.sourceSystem(), dto.sourceReferenceId());
                return doUpdate(existing.get(), dto);
            }
        }

        MobilityEvent entity = mapper.toEntity(dto);
        resolveAndLinkSubjects(entity, dto);

        MobilityEvent saved = repository.save(entity);
        log.info("FAUST_MOBILITY_CREATE: publicId={} type={} ts={} source={}",
                saved.getExternalId(), saved.getEventType(),
                saved.getEventTimestamp(), saved.getSourceSystem());
        return mapper.toResponse(saved);
    }

    public List<MobilityEventResponseDto> createBulk(List<MobilityEventRequestDto> dtos) {
        log.info("FAUST_MOBILITY_BULK: processing {} events.", dtos.size());
        return dtos.stream().map(this::create).toList();
    }

    // =========================================================================
    // Update
    // =========================================================================

    public MobilityEventResponseDto update(UUID publicId, MobilityEventRequestDto dto) {
        MobilityEvent entity = findOrThrow(publicId);
        return doUpdate(entity, dto);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID publicId) {
        MobilityEvent entity = findOrThrow(publicId);
        log.info("FAUST_MOBILITY_DELETE: publicId={}", publicId);
        repository.delete(entity);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public MobilityEventResponseDto getByPublicId(UUID publicId) {
        return mapper.toResponse(findOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getBySubjectPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findAllBySubjectPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getBySubjectPersonAndType(
            UUID personPublicId, MobilityEventType type) {
        verifyPersonExists(personPublicId);
        return repository.findBySubjectPersonAndType(personPublicId, type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getBorderCrossingsByPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findBorderCrossingsByPerson(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getViolationsByPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return repository.findViolationsByPerson(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getBySubjectVehicle(UUID vehiclePublicId) {
        verifyVehicleExists(vehiclePublicId);
        return repository.findAllBySubjectVehicleExternalId(vehiclePublicId)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getBySubjectVehicleAndType(
            UUID vehiclePublicId, MobilityEventType type) {
        verifyVehicleExists(vehiclePublicId);
        return repository.findBySubjectVehicleAndType(vehiclePublicId, type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getByEventType(MobilityEventType type) {
        return repository.findByEventType(type)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getUnpaidViolations() {
        return repository.findUnpaidViolations()
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getByLicensePlate(String licensePlate) {
        return repository.findByLicensePlateRaw(licensePlate.toUpperCase().replace(" ", ""))
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getByLocationCountry(String countryCode) {
        return repository.findByLocationCountryCode(countryCode.toUpperCase())
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Movement timeline for a person between two timestamps.
     * Returns events in chronological (ascending) order.
     */
    @Transactional(readOnly = true)
    public List<MobilityEventResponseDto> getTimelineForPerson(
            UUID personPublicId, LocalDateTime from, LocalDateTime to) {
        verifyPersonExists(personPublicId);
        return repository.findTimelineForPerson(personPublicId, from, to)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private MobilityEventResponseDto doUpdate(MobilityEvent entity, MobilityEventRequestDto dto) {
        mapper.updateEntity(dto, entity);
        resolveAndLinkSubjects(entity, dto);
        log.info("FAUST_MOBILITY_UPDATE: publicId={} updated.", entity.getExternalId());
        return mapper.toResponse(entity);
    }

    /**
     * Resolves person and vehicle FKs independently.
     * A null public ID leaves the existing FK unchanged (IGNORE strategy).
     * An explicit public ID that does not resolve throws EntityNotFoundException.
     */
    private void resolveAndLinkSubjects(MobilityEvent entity, MobilityEventRequestDto dto) {
        if (dto.subjectPersonPublicId() != null) {
            entity.setSubjectPerson(
                    personRepository.findByExternalId(dto.subjectPersonPublicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Person not found: " + dto.subjectPersonPublicId())));
        }
        if (dto.subjectVehiclePublicId() != null) {
            entity.setSubjectVehicle(
                    vehicleRepository.findByExternalId(dto.subjectVehiclePublicId())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "VehicleRecord not found: " + dto.subjectVehiclePublicId())));
        }
    }

    private MobilityEvent findOrThrow(UUID publicId) {
        return repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "MobilityEvent not found: " + publicId));
    }

    private void verifyPersonExists(UUID publicId) {
        if (!personRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Person not found: " + publicId);
    }

    private void verifyVehicleExists(UUID publicId) {
        if (!vehicleRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("VehicleRecord not found: " + publicId);
    }
}
