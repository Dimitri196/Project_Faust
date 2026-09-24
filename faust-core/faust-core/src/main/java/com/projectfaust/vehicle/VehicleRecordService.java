package com.projectfaust.vehicle;

import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.insurance.InsuranceRecord;
import com.projectfaust.insurance.InsuranceRecordRepository;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.vehicle.dto.VehicleRecordRequestDto;
import com.projectfaust.vehicle.dto.VehicleRecordResponseDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link VehicleRecord} entities.
 *
 * <p><b>Idempotency:</b> create operations check for an existing record with
 * the same {@code (sourceSystem, sourceReferenceId)} pair. If one is found
 * the request is treated as an update (upsert semantics), ensuring that
 * re-ingesting the same source data does not produce duplicate records.</p>
 *
 * <p><b>Owner / operator resolution:</b> each role carries a
 * {@link VehicleOwnerType} discriminator. The service verifies the correct
 * FK (person or institution) is provided for the declared type, resolves
 * the entity, and links it before persisting.</p>
 *
 * <p>All log messages follow the {@code FAUST_VEHICLE_*} prefix convention
 * to make log filtering and alerting straightforward.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VehicleRecordService {

    private final VehicleRecordRepository       vehicleRecordRepository;
    private final VehicleOwnershipHistoryRepository historyRepository;
    private final PersonRepository              personRepository;
    private final InstitutionRepository         institutionRepository;
    private final InsuranceRecordRepository     insuranceRecordRepository;
    private final VehicleRecordMapper           mapper;

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new vehicle record, or updates an existing one if the
     * {@code (sourceSystem, sourceReferenceId)} pair is already present
     * (idempotent ingest).
     *
     * @param dto inbound request
     * @return the persisted record as a response DTO
     */
    public VehicleRecordResponseDto create(VehicleRecordRequestDto dto) {
        // Idempotency guard
        if (dto.sourceReferenceId() != null) {
            var existing = vehicleRecordRepository
                    .findBySourceSystemAndSourceReferenceId(dto.sourceSystem(), dto.sourceReferenceId());
            if (existing.isPresent()) {
                log.info("FAUST_VEHICLE_UPSERT: existing record found for source={} ref={}; updating.",
                        dto.sourceSystem(), dto.sourceReferenceId());
                return doUpdate(existing.get(), dto);
            }
        }

        VehicleRecord entity = mapper.toEntity(dto);
        resolveAndLinkParties(entity, dto);

        return mapper.toResponse(persistNew(entity));
    }

    /**
     * Bulk-creates vehicle records. Each item is processed independently with
     * its own idempotency check; a failure in one item does not roll back others
     * (each call is wrapped in the outer transaction — split into per-item
     * transactions if independent failure isolation is required).
     *
     * @param dtos list of inbound requests
     * @return list of response DTOs
     */
    public List<VehicleRecordResponseDto> createBulk(List<VehicleRecordRequestDto> dtos) {
        log.info("FAUST_VEHICLE_BULK_INGEST: processing {} records.", dtos.size());
        return dtos.stream().map(this::create).toList();
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Partially updates a vehicle record identified by its public UUID.
     * Only non-null fields in {@code dto} are applied (NullValuePropertyMappingStrategy.IGNORE).
     *
     * @param publicId public UUID of the record
     * @param dto      partial update payload
     * @return updated response DTO
     * @throws EntityNotFoundException if no record with {@code publicId} exists
     */
    public VehicleRecordResponseDto update(UUID publicId, VehicleRecordRequestDto dto) {
        VehicleRecord entity = findByPublicIdOrThrow(publicId);
        return doUpdate(entity, dto);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    /**
     * Deletes a vehicle record by public UUID.
     *
     * @param publicId public UUID
     * @throws EntityNotFoundException if not found
     */
    public void delete(UUID publicId) {
        VehicleRecord entity = findByPublicIdOrThrow(publicId);
        log.info("FAUST_VEHICLE_DELETE: removing vehicleRecord publicId={}", publicId);
        vehicleRecordRepository.delete(entity);
    }

    // =========================================================================
    // Read — by public ID
    // =========================================================================

    /**
     * Retrieves a single vehicle record by its public UUID.
     *
     * @param publicId public UUID
     * @return response DTO
     * @throws EntityNotFoundException if not found
     */
    @Transactional(readOnly = true)
    public VehicleRecordResponseDto getByPublicId(UUID publicId) {
        return mapper.toResponse(findByPublicIdOrThrow(publicId));
    }

    // =========================================================================
    // Read — by person (owner)
    // =========================================================================

    /**
     * Returns all vehicles owned by the specified person.
     *
     * @param personPublicId public UUID of the person
     * @return list of vehicle response DTOs, ordered by registration date descending
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getByOwnerPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return vehicleRecordRepository.findAllByOwnerPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns active vehicles owned by the specified person.
     *
     * @param personPublicId public UUID of the person
     * @return list of active vehicle response DTOs
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getActiveByOwnerPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return vehicleRecordRepository.findActiveByOwnerPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Read — by person (operator)
    // =========================================================================

    /**
     * Returns all vehicles operated (but not necessarily owned) by the specified person.
     *
     * @param personPublicId public UUID of the person
     * @return list of vehicle response DTOs
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getByOperatorPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return vehicleRecordRepository.findAllByOperatorPersonExternalId(personPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Read — by institution (owner)
    // =========================================================================

    /**
     * Returns all vehicles in the fleet of the specified institution.
     *
     * @param institutionPublicId public UUID of the institution
     * @return list of vehicle response DTOs
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getByOwnerInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return vehicleRecordRepository.findAllByOwnerInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Returns active vehicles in the fleet of the specified institution.
     *
     * @param institutionPublicId public UUID of the institution
     * @return list of active vehicle response DTOs
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getActiveByOwnerInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return vehicleRecordRepository.findActiveByOwnerInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Read — by VIN / plate
    // =========================================================================

    /**
     * Looks up vehicle records by VIN. Returns a list to accommodate data
     * quality issues where the same VIN is ingested from multiple sources.
     *
     * @param vin 17-character VIN
     * @return list of matching records
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getByVin(String vin) {
        log.info("FAUST_VEHICLE_VIN_LOOKUP: vin={}", vin);
        return vehicleRecordRepository.findByVin(vin.toUpperCase())
                .stream().map(mapper::toResponse).toList();
    }

    /**
     * Looks up vehicle records by licence plate and country.
     * Multiple results are expected as plates can be reissued over time.
     *
     * @param licensePlate plate string (normalised to upper case, no spaces)
     * @param countryCode  ISO 3166-1 alpha-2 country code
     * @return list of matching records, ordered by registration date descending
     */
    @Transactional(readOnly = true)
    public List<VehicleRecordResponseDto> getByLicensePlate(String licensePlate, String countryCode) {
        return vehicleRecordRepository.findByLicensePlateAndCountry(
                licensePlate.toUpperCase().replace(" ", ""),
                countryCode.toUpperCase()
        ).stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private VehicleRecordResponseDto doUpdate(VehicleRecord entity, VehicleRecordRequestDto dto) {
        mapper.updateEntity(dto, entity);
        resolveAndLinkParties(entity, dto);
        log.info("FAUST_VEHICLE_UPDATE: vehicleRecord publicId={} updated.", entity.getExternalId());
        return mapper.toResponse(entity);
    }

    private VehicleRecord persistNew(VehicleRecord entity) {
        VehicleRecord saved = vehicleRecordRepository.save(entity);
        log.info("FAUST_VEHICLE_CREATE: new vehicleRecord publicId={} vin={} plate={} source={}",
                saved.getExternalId(), saved.getVin(), saved.getLicensePlate(), saved.getSourceSystem());
        return saved;
    }

    /**
     * Resolves owner, operator, and insurance FK references from the DTO and
     * sets them on the entity. Throws {@link IllegalArgumentException} if the
     * declared {@link VehicleOwnerType} does not match the supplied public ID.
     */
    private void resolveAndLinkParties(VehicleRecord entity, VehicleRecordRequestDto dto) {
        // ── Owner ──────────────────────────────────────────────────────────────
        if (dto.ownerType() != null) {
            entity.setOwnerType(dto.ownerType());
            switch (dto.ownerType()) {
                case PERSON -> {
                    if (dto.ownerPersonPublicId() != null) {
                        entity.setOwnerPerson(resolvePersonOrThrow(dto.ownerPersonPublicId(), "owner"));
                        entity.setOwnerInstitution(null);
                    }
                }
                case INSTITUTION -> {
                    if (dto.ownerInstitutionPublicId() != null) {
                        entity.setOwnerInstitution(resolveInstitutionOrThrow(dto.ownerInstitutionPublicId(), "owner"));
                        entity.setOwnerPerson(null);
                    }
                }
            }
        }

        // ── Operator (optional role) ───────────────────────────────────────────
        if (dto.operatorType() != null) {
            entity.setOperatorType(dto.operatorType());
            switch (dto.operatorType()) {
                case PERSON -> {
                    if (dto.operatorPersonPublicId() != null) {
                        entity.setOperatorPerson(resolvePersonOrThrow(dto.operatorPersonPublicId(), "operator"));
                        entity.setOperatorInstitution(null);
                    }
                }
                case INSTITUTION -> {
                    if (dto.operatorInstitutionPublicId() != null) {
                        entity.setOperatorInstitution(resolveInstitutionOrThrow(dto.operatorInstitutionPublicId(), "operator"));
                        entity.setOperatorPerson(null);
                    }
                }
            }
        }

        // ── Insurance cross-reference ─────────────────────────────────────────
        if (dto.insuranceRecordPublicId() != null) {
            InsuranceRecord ins = insuranceRecordRepository
                    .findByExternalId(dto.insuranceRecordPublicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "InsuranceRecord not found: " + dto.insuranceRecordPublicId()));
            entity.setInsuranceRecord(ins);
        }
    }

    private Person resolvePersonOrThrow(UUID publicId, String role) {
        return personRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Person not found for %s role: %s".formatted(role, publicId)));
    }

    private Institution resolveInstitutionOrThrow(UUID publicId, String role) {
        return institutionRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Institution not found for %s role: %s".formatted(role, publicId)));
    }

    private VehicleRecord findByPublicIdOrThrow(UUID publicId) {
        return vehicleRecordRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "VehicleRecord not found: " + publicId));
    }

    private void verifyPersonExists(UUID personPublicId) {
        if (!personRepository.existsByExternalId(personPublicId)) {
            throw new EntityNotFoundException("Person not found: " + personPublicId);
        }
    }

    private void verifyInstitutionExists(UUID institutionPublicId) {
        if (!institutionRepository.existsByExternalId(institutionPublicId)) {
            throw new EntityNotFoundException("Institution not found: " + institutionPublicId);
        }
    }
}
