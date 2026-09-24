package com.projectfaust.traces;

import com.projectfaust.financial.BankAccount;
import com.projectfaust.financial.BankAccountRepository;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.traces.dto.request.*;
import com.projectfaust.traces.dto.response.*;
import com.projectfaust.traces.mapper.*;
import com.projectfaust.traces.repository.*;
import com.projectfaust.vehicle.VehicleRecord;
import com.projectfaust.vehicle.VehicleRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unified service for all five trace subtypes.
 *
 * <p>Each type has its own create/update/get/delete block.
 * Person, vehicle, and bank account resolution is done here
 * so the mappers stay pure (no Spring injection needed in mappers).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TraceService {

    private final PersonRepository             personRepository;
    private final VehicleRecordRepository      vehicleRecordRepository;
    private final BankAccountRepository        bankAccountRepository;

    private final DigitalTraceRepository       digitalRepo;
    private final FinancialTraceRepository     financialRepo;
    private final CameraTraceRepository        cameraRepo;
    private final TelcoTraceRepository         telcoRepo;
    private final SurveillanceEventRepository  surveillanceRepo;

    private final DigitalTraceMapper           digitalMapper;
    private final FinancialTraceMapper         financialMapper;
    private final CameraTraceMapper            cameraMapper;
    private final TelcoTraceMapper             telcoMapper;
    private final SurveillanceEventMapper      surveillanceMapper;

    // ══════════════════════════════════════════════════════════════════════════
    // DIGITAL TRACES
    // ══════════════════════════════════════════════════════════════════════════

    public DigitalTraceResponse createDigital(DigitalTraceRequest request) {
        log.info("FAUST_TRACES_DIGITAL_CREATE person={}", request.personPublicId());
        DigitalTrace entity = digitalMapper.toEntity(request);
        entity.setPerson(resolvePerson(request.personPublicId()));
        return digitalMapper.toResponse(digitalRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public DigitalTraceResponse getDigital(UUID externalId) {
        return digitalMapper.toResponse(requireDigital(externalId));
    }

    @Transactional(readOnly = true)
    public List<DigitalTraceResponse> getDigitalByPerson(UUID personId) {
        return digitalRepo.findAllByPersonExternalId(personId)
                .stream().map(digitalMapper::toResponse).toList();
    }

    public DigitalTraceResponse updateDigital(UUID externalId, DigitalTraceRequest request) {
        log.info("FAUST_TRACES_DIGITAL_UPDATE id={}", externalId);
        DigitalTrace entity = requireDigital(externalId);
        digitalMapper.updateEntity(request, entity);
        return digitalMapper.toResponse(entity);
    }

    public void deleteDigital(UUID externalId) {
        log.warn("FAUST_TRACES_DIGITAL_DELETE id={}", externalId);
        digitalRepo.delete(requireDigital(externalId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FINANCIAL TRACES
    // ══════════════════════════════════════════════════════════════════════════

    public FinancialTraceResponse createFinancial(FinancialTraceRequest request) {
        log.info("FAUST_TRACES_FINANCIAL_CREATE person={}", request.personPublicId());
        FinancialTrace entity = financialMapper.toEntity(request);
        entity.setPerson(resolvePerson(request.personPublicId()));
        if (request.bankAccountPublicId() != null) {
            entity.setBankAccount(resolveBankAccount(request.bankAccountPublicId()));
        }
        return financialMapper.toResponse(financialRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public FinancialTraceResponse getFinancial(UUID externalId) {
        return financialMapper.toResponse(requireFinancial(externalId));
    }

    @Transactional(readOnly = true)
    public List<FinancialTraceResponse> getFinancialByPerson(UUID personId) {
        return financialRepo.findAllByPersonExternalId(personId)
                .stream().map(financialMapper::toResponse).toList();
    }

    public FinancialTraceResponse updateFinancial(UUID externalId, FinancialTraceRequest request) {
        log.info("FAUST_TRACES_FINANCIAL_UPDATE id={}", externalId);
        FinancialTrace entity = requireFinancial(externalId);
        financialMapper.updateEntity(request, entity);
        if (request.bankAccountPublicId() != null) {
            entity.setBankAccount(resolveBankAccount(request.bankAccountPublicId()));
        }
        return financialMapper.toResponse(entity);
    }

    public void deleteFinancial(UUID externalId) {
        log.warn("FAUST_TRACES_FINANCIAL_DELETE id={}", externalId);
        financialRepo.delete(requireFinancial(externalId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CAMERA TRACES
    // ══════════════════════════════════════════════════════════════════════════

    public CameraTraceResponse createCamera(CameraTraceRequest request) {
        log.info("FAUST_TRACES_CAMERA_CREATE person={}", request.personPublicId());
        CameraTrace entity = cameraMapper.toEntity(request);
        entity.setPerson(resolvePerson(request.personPublicId()));
        if (request.vehicleRecordPublicId() != null) {
            entity.setVehicleRecord(resolveVehicle(request.vehicleRecordPublicId()));
        }
        return cameraMapper.toResponse(cameraRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public CameraTraceResponse getCamera(UUID externalId) {
        return cameraMapper.toResponse(requireCamera(externalId));
    }

    @Transactional(readOnly = true)
    public List<CameraTraceResponse> getCameraByPerson(UUID personId) {
        return cameraRepo.findAllByPersonExternalId(personId)
                .stream().map(cameraMapper::toResponse).toList();
    }

    public CameraTraceResponse updateCamera(UUID externalId, CameraTraceRequest request) {
        log.info("FAUST_TRACES_CAMERA_UPDATE id={}", externalId);
        CameraTrace entity = requireCamera(externalId);
        cameraMapper.updateEntity(request, entity);
        if (request.vehicleRecordPublicId() != null) {
            entity.setVehicleRecord(resolveVehicle(request.vehicleRecordPublicId()));
        }
        return cameraMapper.toResponse(entity);
    }

    public void deleteCamera(UUID externalId) {
        log.warn("FAUST_TRACES_CAMERA_DELETE id={}", externalId);
        cameraRepo.delete(requireCamera(externalId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TELCO TRACES
    // ══════════════════════════════════════════════════════════════════════════

    public TelcoTraceResponse createTelco(TelcoTraceRequest request) {
        log.info("FAUST_TRACES_TELCO_CREATE person={}", request.personPublicId());
        TelcoTrace entity = telcoMapper.toEntity(request);
        entity.setPerson(resolvePerson(request.personPublicId()));
        return telcoMapper.toResponse(telcoRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public TelcoTraceResponse getTelco(UUID externalId) {
        return telcoMapper.toResponse(requireTelco(externalId));
    }

    @Transactional(readOnly = true)
    public List<TelcoTraceResponse> getTelcoByPerson(UUID personId) {
        return telcoRepo.findAllByPersonExternalId(personId)
                .stream().map(telcoMapper::toResponse).toList();
    }

    public TelcoTraceResponse updateTelco(UUID externalId, TelcoTraceRequest request) {
        log.info("FAUST_TRACES_TELCO_UPDATE id={}", externalId);
        TelcoTrace entity = requireTelco(externalId);
        telcoMapper.updateEntity(request, entity);
        return telcoMapper.toResponse(entity);
    }

    public void deleteTelco(UUID externalId) {
        log.warn("FAUST_TRACES_TELCO_DELETE id={}", externalId);
        telcoRepo.delete(requireTelco(externalId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SURVEILLANCE EVENTS
    // ══════════════════════════════════════════════════════════════════════════

    public SurveillanceEventResponse createSurveillance(SurveillanceEventRequest request) {
        log.info("FAUST_TRACES_SURVEILLANCE_CREATE person={}", request.personPublicId());
        SurveillanceEvent entity = surveillanceMapper.toEntity(request);
        entity.setPerson(resolvePerson(request.personPublicId()));
        if (request.vehicleRecordPublicId() != null) {
            entity.setVehicleRecord(resolveVehicle(request.vehicleRecordPublicId()));
        }
        resolveParticipants(request.meetingParticipantPublicIds(), entity);
        return surveillanceMapper.toResponse(surveillanceRepo.save(entity));
    }

    @Transactional(readOnly = true)
    public SurveillanceEventResponse getSurveillance(UUID externalId) {
        return surveillanceMapper.toResponse(requireSurveillance(externalId));
    }

    @Transactional(readOnly = true)
    public List<SurveillanceEventResponse> getSurveillanceByPerson(UUID personId) {
        return surveillanceRepo.findAllByPersonExternalId(personId)
                .stream().map(surveillanceMapper::toResponse).toList();
    }

    public SurveillanceEventResponse updateSurveillance(UUID externalId, SurveillanceEventRequest request) {
        log.info("FAUST_TRACES_SURVEILLANCE_UPDATE id={}", externalId);
        SurveillanceEvent entity = requireSurveillance(externalId);
        surveillanceMapper.updateEntity(request, entity);
        if (request.vehicleRecordPublicId() != null) {
            entity.setVehicleRecord(resolveVehicle(request.vehicleRecordPublicId()));
        }
        if (request.meetingParticipantPublicIds() != null) {
            entity.getMeetingParticipants().clear();
            resolveParticipants(request.meetingParticipantPublicIds(), entity);
        }
        return surveillanceMapper.toResponse(entity);
    }

    public void deleteSurveillance(UUID externalId) {
        log.warn("FAUST_TRACES_SURVEILLANCE_DELETE id={}", externalId);
        surveillanceRepo.delete(requireSurveillance(externalId));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Internal resolution helpers
    // ══════════════════════════════════════════════════════════════════════════

    private Person resolvePerson(UUID publicId) {
        if (publicId == null) return null;
        return personRepository.findByExternalId(publicId)
                .orElseThrow(() -> new NoSuchElementException("Person not found: " + publicId));
    }

    private VehicleRecord resolveVehicle(UUID publicId) {
        return vehicleRecordRepository.findByExternalId(publicId)
                .orElseThrow(() -> new NoSuchElementException("VehicleRecord not found: " + publicId));
    }

    private BankAccount resolveBankAccount(UUID publicId) {
        return bankAccountRepository.findByExternalId(publicId)
                .orElseThrow(() -> new NoSuchElementException("BankAccount not found: " + publicId));
    }

    private void resolveParticipants(Set<UUID> publicIds, SurveillanceEvent entity) {
        if (publicIds == null || publicIds.isEmpty()) return;
        Set<Person> participants = publicIds.stream()
                .map(id -> personRepository.findByExternalId(id)
                        .orElseThrow(() -> new NoSuchElementException("Participant not found: " + id)))
                .collect(Collectors.toSet());
        entity.getMeetingParticipants().addAll(participants);
    }

    // ── Require-or-throw ──────────────────────────────────────────────────────

    private DigitalTrace requireDigital(UUID id) {
        return digitalRepo.findByExternalId(id)
                .orElseThrow(() -> new NoSuchElementException("DigitalTrace not found: " + id));
    }

    private FinancialTrace requireFinancial(UUID id) {
        return financialRepo.findByExternalId(id)
                .orElseThrow(() -> new NoSuchElementException("FinancialTrace not found: " + id));
    }

    private CameraTrace requireCamera(UUID id) {
        return cameraRepo.findByExternalId(id)
                .orElseThrow(() -> new NoSuchElementException("CameraTrace not found: " + id));
    }

    private TelcoTrace requireTelco(UUID id) {
        return telcoRepo.findByExternalId(id)
                .orElseThrow(() -> new NoSuchElementException("TelcoTrace not found: " + id));
    }

    private SurveillanceEvent requireSurveillance(UUID id) {
        return surveillanceRepo.findByExternalId(id)
                .orElseThrow(() -> new NoSuchElementException("SurveillanceEvent not found: " + id));
    }
}
