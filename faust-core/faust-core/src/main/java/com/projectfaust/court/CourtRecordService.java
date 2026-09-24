package com.projectfaust.court;

import com.projectfaust.court.dto.CourtRecordRequestDto;
import com.projectfaust.court.dto.CourtRecordRequestDto.PartyDto;
import com.projectfaust.court.dto.CourtRecordResponseDto;
import com.projectfaust.criminal.CriminalRecord;
import com.projectfaust.criminal.CriminalRecordRepository;
import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link CourtRecord} entities.
 *
 * <p><b>Party management:</b> parties are replaced in their entirety on every
 * update (orphanRemoval = true on the collection). This avoids partial-update
 * complexity at the cost of deleting and re-inserting party rows on each PUT —
 * acceptable given the low cardinality of parties per proceeding.</p>
 *
 * <p><b>Cross-reference:</b> the optional {@code linkedCriminalRecord} is
 * resolved from a public UUID supplied in the request.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourtRecordService {

    private final CourtRecordRepository      courtRecordRepository;
    private final CourtRecordPartyRepository partyRepository;
    private final PersonRepository           personRepository;
    private final InstitutionRepository      institutionRepository;
    private final CriminalRecordRepository   criminalRecordRepository;
    private final CourtRecordMapper          mapper;

    // =========================================================================
    // Create
    // =========================================================================

    public CourtRecordResponseDto create(CourtRecordRequestDto dto) {
        if (dto.sourceReferenceId() != null) {
            var existing = courtRecordRepository
                    .findBySourceSystemAndSourceReferenceId(dto.sourceSystem(), dto.sourceReferenceId());
            if (existing.isPresent()) {
                log.info("FAUST_COURT_UPSERT: existing record source={} ref={}; updating.",
                        dto.sourceSystem(), dto.sourceReferenceId());
                return doUpdate(existing.get(), dto);
            }
        }

        CourtRecord entity = mapper.toEntity(dto);
        resolveAndLinkCriminalRecord(entity, dto);
        entity.setParties(buildParties(dto.parties(), entity));

        CourtRecord saved = courtRecordRepository.save(entity);
        log.info("FAUST_COURT_CREATE: publicId={} case={} type={} parties={}",
                saved.getExternalId(), saved.getCaseNumber(),
                saved.getProceedingType(), saved.getParties().size());
        return toFullResponse(saved);
    }

    public List<CourtRecordResponseDto> createBulk(List<CourtRecordRequestDto> dtos) {
        log.info("FAUST_COURT_BULK: processing {} records.", dtos.size());
        return dtos.stream().map(this::create).toList();
    }

    // =========================================================================
    // Update
    // =========================================================================

    public CourtRecordResponseDto update(UUID publicId, CourtRecordRequestDto dto) {
        CourtRecord entity = findOrThrow(publicId);
        return doUpdate(entity, dto);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID publicId) {
        CourtRecord entity = findOrThrow(publicId);
        log.info("FAUST_COURT_DELETE: publicId={}", publicId);
        courtRecordRepository.delete(entity);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public CourtRecordResponseDto getByPublicId(UUID publicId) {
        return toFullResponse(findOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByPartyPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return courtRecordRepository.findAllByPartyPersonExternalId(personPublicId)
                .stream().map(this::toFullResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByPartyPersonAndRole(UUID personPublicId, PartyRole role) {
        verifyPersonExists(personPublicId);
        return courtRecordRepository.findAllByPartyPersonAndRole(personPublicId, role)
                .stream().map(this::toFullResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByPartyInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return courtRecordRepository.findAllByPartyInstitutionExternalId(institutionPublicId)
                .stream().map(this::toFullResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByPartyInstitutionAndRole(UUID institutionPublicId, PartyRole role) {
        verifyInstitutionExists(institutionPublicId);
        return courtRecordRepository.findAllByPartyInstitutionAndRole(institutionPublicId, role)
                .stream().map(this::toFullResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByProceedingType(ProceedingType type) {
        return courtRecordRepository.findByProceedingType(type)
                .stream().map(this::toFullResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getByOutcome(CourtOutcome outcome) {
        return courtRecordRepository.findByOutcome(outcome)
                .stream().map(this::toFullResponse).toList();
    }

    /**
     * Intelligence query: finds all proceedings in which two persons
     * appeared together (co-defendants, co-plaintiffs, or any mix of roles).
     */
    @Transactional(readOnly = true)
    public List<CourtRecordResponseDto> getSharedProceedings(UUID personAPublicId, UUID personBPublicId) {
        verifyPersonExists(personAPublicId);
        verifyPersonExists(personBPublicId);
        return partyRepository.findSharedProceedings(personAPublicId, personBPublicId)
                .stream().map(this::toFullResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private CourtRecordResponseDto doUpdate(CourtRecord entity, CourtRecordRequestDto dto) {
        mapper.updateEntity(dto, entity);
        resolveAndLinkCriminalRecord(entity, dto);

        // Replace party list
        if (dto.parties() != null) {
            entity.getParties().clear();
            entity.getParties().addAll(buildParties(dto.parties(), entity));
        }

        log.info("FAUST_COURT_UPDATE: publicId={} updated.", entity.getExternalId());
        return toFullResponse(entity);
    }

    /**
     * Builds the {@link CourtRecordParty} children from the DTO list,
     * resolving each party's person or institution FK.
     */
    private List<CourtRecordParty> buildParties(List<PartyDto> partyDtos, CourtRecord parent) {
        if (partyDtos == null || partyDtos.isEmpty()) return new ArrayList<>();

        return partyDtos.stream().map(dto -> {
            CourtRecordParty party = CourtRecordParty.builder()
                    .courtRecord(parent)
                    .partySubjectType(dto.partySubjectType())
                    .partyRole(dto.partyRole())
                    .partyNameRaw(dto.partyNameRaw())
                    .partyNationalId(dto.partyNationalId())
                    .roleDetail(dto.roleDetail())
                    .legallyRepresented(dto.legallyRepresented())
                    .legalRepresentativeName(dto.legalRepresentativeName())
                    .analyticalNote(dto.analyticalNote())
                    .build();

            switch (dto.partySubjectType()) {
                case PERSON -> {
                    if (dto.personPublicId() != null) {
                        Person p = personRepository.findByExternalId(dto.personPublicId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Person not found for party: " + dto.personPublicId()));
                        party.setPerson(p);
                    }
                }
                case INSTITUTION -> {
                    if (dto.institutionPublicId() != null) {
                        Institution i = institutionRepository.findByExternalId(dto.institutionPublicId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                        "Institution not found for party: " + dto.institutionPublicId()));
                        party.setInstitution(i);
                    }
                }
            }
            return party;
        }).toList();
    }

    private void resolveAndLinkCriminalRecord(CourtRecord entity, CourtRecordRequestDto dto) {
        if (dto.linkedCriminalRecordPublicId() != null) {
            CriminalRecord cr = criminalRecordRepository
                    .findByExternalId(dto.linkedCriminalRecordPublicId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "CriminalRecord not found: " + dto.linkedCriminalRecordPublicId()));
            entity.setLinkedCriminalRecord(cr);
        }
    }

    /**
     * Builds a full response DTO including resolved parties.
     * Separate from mapper.toResponse() because party mapping requires
     * the @Named helper — done inline here for clarity.
     */
    private CourtRecordResponseDto toFullResponse(CourtRecord entity) {
        var partyDtos = entity.getParties().stream()
                .map(mapper::toPartyResponse)
                .toList();

        var base = mapper.toResponse(entity);
        // Reconstruct with parties — Java records are immutable, so we rebuild.
        return new CourtRecordResponseDto(
                base.publicId(),
                base.caseNumber(),
                base.courtName(),
                base.courtLevel(),
                base.countryCode(),
                base.proceedingType(),
                base.subjectMatter(),
                base.statute(),
                base.filedDate(),
                base.firstHearingDate(),
                base.judgmentDate(),
                base.appealDeadline(),
                base.closedDate(),
                base.outcome(),
                base.appealed(),
                base.judgmentSummary(),
                partyDtos,
                base.linkedCriminalRecordPublicId(),
                base.sourceSystem(),
                base.sourceReferenceId(),
                base.registryUrl(),
                base.verificationStatus(),
                base.confidenceScore(),
                base.clearanceLevel(),
                base.analyticalNote(),
                base.ingestedAt()
        );
    }

    private CourtRecord findOrThrow(UUID publicId) {
        return courtRecordRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "CourtRecord not found: " + publicId));
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
