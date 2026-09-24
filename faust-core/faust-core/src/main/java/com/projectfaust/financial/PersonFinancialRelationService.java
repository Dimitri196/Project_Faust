package com.projectfaust.financial;

import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.financial.dto.PersonFinancialRelationRequest;
import com.projectfaust.financial.dto.PersonFinancialRelationResponse;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.shared.enums.VerificationStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link PersonAccountRelation} entities.
 *
 * <p>Handles person ↔ bank account linkage including:
 * <ul>
 *   <li>Creating / deactivating / deleting relation records</li>
 *   <li>Duplicate-guard on (person, account, role) triple</li>
 *   <li>FININT aggregate view per person</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PersonFinancialRelationService {

    private final PersonAccountRelationRepository relationRepository;
    private final PersonRepository                personRepository;
    private final BankAccountRepository           accountRepository;
    private final BankAccountMapper               mapper;

    // =========================================================================
    // Link
    // =========================================================================

    public PersonFinancialRelationResponse link(PersonFinancialRelationRequest dto) {
        // Duplicate guard
        if (relationRepository.existsByPersonAccountAndRole(
                dto.personPublicId(), dto.accountPublicId(), dto.roleType())) {
            log.info("FAUST_FINANCIAL_PERSON_LINK_DUP: person={} account={} role={} — returning existing.",
                    dto.personPublicId(), dto.accountPublicId(), dto.roleType());
            return relationRepository
                    .findByPersonAndRole(dto.personPublicId(), dto.roleType())
                    .stream()
                    .filter(r -> r.getBankAccount().getExternalId().equals(dto.accountPublicId()))
                    .findFirst()
                    .map(mapper::toPersonRelationResponse)
                    .orElseThrow(() -> new IllegalStateException("Duplicate guard passed but relation not found"));
        }

        var person  = personRepository.findByExternalId(dto.personPublicId())
                .orElseThrow(() -> new EntityNotFoundException("Person not found: " + dto.personPublicId()));
        var account = accountRepository.findByExternalId(dto.accountPublicId())
                .orElseThrow(() -> new EntityNotFoundException("BankAccount not found: " + dto.accountPublicId()));

        PersonAccountRelation relation = PersonAccountRelation.builder()
                .person(person)
                .bankAccount(account)
                .roleType(dto.roleType())
                .validFrom(dto.validFrom())
                .validTo(dto.validTo())
                .active(dto.active() != null ? dto.active() : true)
                .verificationStatus(dto.verificationStatus() != null
                        ? dto.verificationStatus()
                        : VerificationStatus.PENDING_REVIEW)
                .build();

        PersonAccountRelation saved = relationRepository.save(relation);
        log.info("FAUST_FINANCIAL_PERSON_LINK: person={} account={} role={} publicId={}",
                dto.personPublicId(), dto.accountPublicId(), dto.roleType(), saved.getExternalId());
        return mapper.toPersonRelationResponse(saved);
    }

    // =========================================================================
    // Deactivate (soft delete)
    // =========================================================================

    public PersonFinancialRelationResponse deactivate(UUID relationPublicId) {
        PersonAccountRelation relation = findOrThrow(relationPublicId);
        relation.setActive(false);
        log.info("FAUST_FINANCIAL_PERSON_DEACTIVATE: relation publicId={}", relationPublicId);
        return mapper.toPersonRelationResponse(relation);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID relationPublicId) {
        PersonAccountRelation relation = findOrThrow(relationPublicId);
        log.info("FAUST_FINANCIAL_PERSON_DELETE: relation publicId={}", relationPublicId);
        relationRepository.delete(relation);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PersonFinancialRelationResponse> getAllByPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return relationRepository.findAllByPersonExternalId(personPublicId)
                .stream().map(mapper::toPersonRelationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonFinancialRelationResponse> getActiveByPerson(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return relationRepository.findActiveByPersonExternalId(personPublicId)
                .stream().map(mapper::toPersonRelationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PersonFinancialRelationResponse> getByAccount(UUID accountPublicId) {
        verifyAccountExists(accountPublicId);
        return relationRepository.findAllByAccountExternalId(accountPublicId)
                .stream().map(mapper::toPersonRelationResponse).toList();
    }

    /**
     * FININT aggregate — all financial relations for a person as flat response list.
     * Each entry represents one person ↔ account relation with full account detail.
     */
    @Transactional(readOnly = true)
    public List<FinancialOperationsResponse> getFinancialProfile(UUID personPublicId) {
        verifyPersonExists(personPublicId);
        return relationRepository.findAllByPersonExternalId(personPublicId)
                .stream()
                .map(r -> new FinancialOperationsResponse(
                        r.getId(),
                        r.getBankAccount().getExternalId(),
                        r.getBankAccount().getIban(),
                        r.getBankAccount().getBic(),
                        r.getBankAccount().getBankName(),
                        r.getBankAccount().getCurrency(),
                        r.getBankAccount().isMonitored(),
                        // person side
                        r.getPerson().getExternalId(),
                        r.getPerson().getFirstName() + " " + r.getPerson().getLastName(),
                        r.getRoleType(),
                        // institution side — null for person relations
                        null, null, null,
                        // temporal
                        r.getValidFrom(),
                        r.getValidTo(),
                        r.isActive(),
                        r.getBankAccount().getAnalyticalNote()))
                .toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private PersonAccountRelation findOrThrow(UUID publicId) {
        return relationRepository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("PersonAccountRelation not found: " + publicId));
    }

    private void verifyPersonExists(UUID publicId) {
        if (!personRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Person not found: " + publicId);
    }

    private void verifyAccountExists(UUID publicId) {
        if (!accountRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("BankAccount not found: " + publicId);
    }
}
