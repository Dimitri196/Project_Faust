package com.projectfaust.financial;

import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.financial.dto.InstitutionFinancialRelationRequest;
import com.projectfaust.financial.dto.InstitutionFinancialRelationResponse;
import com.projectfaust.institution.InstitutionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link InstitutionAccountRelation} entities.
 *
 * <p>Handles institution ↔ bank account linkage including:
 * <ul>
 *   <li>Creating / deactivating / deleting relation records</li>
 *   <li>Duplicate-guard on (institution, account, role) triple</li>
 *   <li>FININT aggregate view per institution</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InstitutionFinancialRelationService {

    private final InstitutionAccountRelationRepository relationRepository;
    private final InstitutionRepository                institutionRepository;
    private final BankAccountRepository                accountRepository;
    private final BankAccountMapper                    mapper;

    // =========================================================================
    // Link
    // =========================================================================

    public InstitutionFinancialRelationResponse link(InstitutionFinancialRelationRequest dto) {
        // Duplicate guard
        if (relationRepository.existsByInstitutionAccountAndRole(
                dto.institutionPublicId(), dto.accountPublicId(), dto.roleType())) {
            log.info("FAUST_FINANCIAL_INST_LINK_DUP: institution={} account={} role={} — returning existing.",
                    dto.institutionPublicId(), dto.accountPublicId(), dto.roleType());
            return relationRepository
                    .findByInstitutionAndRole(dto.institutionPublicId(), dto.roleType())
                    .stream()
                    .filter(r -> r.getBankAccount().getExternalId().equals(dto.accountPublicId()))
                    .findFirst()
                    .map(mapper::toInstitutionRelationResponse)
                    .orElseThrow(() -> new IllegalStateException("Duplicate guard passed but relation not found"));
        }

        var institution = institutionRepository.findByExternalId(dto.institutionPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Institution not found: " + dto.institutionPublicId()));
        var account = accountRepository.findByExternalId(dto.accountPublicId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "BankAccount not found: " + dto.accountPublicId()));

        InstitutionAccountRelation relation = InstitutionAccountRelation.builder()
                .institution(institution)
                .bankAccount(account)
                .roleType(dto.roleType())
                .validFrom(dto.validFrom())
                .validTo(dto.validTo())
                .active(dto.active() != null ? dto.active() : true)
                .build();

        InstitutionAccountRelation saved = relationRepository.save(relation);
        log.info("FAUST_FINANCIAL_INST_LINK: institution={} account={} role={}",
                dto.institutionPublicId(), dto.accountPublicId(), dto.roleType());
        return mapper.toInstitutionRelationResponse(saved);
    }

    // =========================================================================
    // Deactivate (soft delete)
    // =========================================================================

    public InstitutionFinancialRelationResponse deactivate(UUID institutionPublicId, UUID accountPublicId) {
        InstitutionAccountRelation relation = findByInstitutionAndAccount(institutionPublicId, accountPublicId);
        relation.setActive(false);
        log.info("FAUST_FINANCIAL_INST_DEACTIVATE: institution={} account={}", institutionPublicId, accountPublicId);
        return mapper.toInstitutionRelationResponse(relation);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID institutionPublicId, UUID accountPublicId) {
        InstitutionAccountRelation relation = findByInstitutionAndAccount(institutionPublicId, accountPublicId);
        log.info("FAUST_FINANCIAL_INST_DELETE: institution={} account={}", institutionPublicId, accountPublicId);
        relationRepository.delete(relation);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public List<InstitutionFinancialRelationResponse> getAllByInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return relationRepository.findAllByInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toInstitutionRelationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionFinancialRelationResponse> getActiveByInstitution(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return relationRepository.findActiveByInstitutionExternalId(institutionPublicId)
                .stream().map(mapper::toInstitutionRelationResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<InstitutionFinancialRelationResponse> getByAccount(UUID accountPublicId) {
        verifyAccountExists(accountPublicId);
        return relationRepository.findAllByAccountExternalId(accountPublicId)
                .stream().map(mapper::toInstitutionRelationResponse).toList();
    }

    /**
     * FININT aggregate — all financial relations for an institution as flat response list.
     * Each entry represents one institution ↔ account relation with full account detail.
     */
    @Transactional(readOnly = true)
    public List<FinancialOperationsResponse> getFinancialProfile(UUID institutionPublicId) {
        verifyInstitutionExists(institutionPublicId);
        return relationRepository.findAllByInstitutionExternalId(institutionPublicId)
                .stream()
                .map(r -> new FinancialOperationsResponse(
                        r.getId(),
                        r.getBankAccount().getExternalId(),
                        r.getBankAccount().getIban(),
                        r.getBankAccount().getBic(),
                        r.getBankAccount().getBankName(),
                        r.getBankAccount().getCurrency(),
                        r.getBankAccount().isMonitored(),
                        // person side — null for institution relations
                        null, null, null,
                        // institution side
                        r.getInstitution().getExternalId(),
                        r.getInstitution().getName(),
                        r.getRoleType(),
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

    private InstitutionAccountRelation findByInstitutionAndAccount(
            UUID institutionPublicId, UUID accountPublicId) {
        return relationRepository
                .findAllByInstitutionExternalId(institutionPublicId)
                .stream()
                .filter(r -> r.getBankAccount().getExternalId().equals(accountPublicId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "InstitutionAccountRelation not found: institution=" +
                                institutionPublicId + " account=" + accountPublicId));
    }

    private void verifyInstitutionExists(UUID publicId) {
        if (!institutionRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("Institution not found: " + publicId);
    }

    private void verifyAccountExists(UUID publicId) {
        if (!accountRepository.existsByExternalId(publicId))
            throw new EntityNotFoundException("BankAccount not found: " + publicId);
    }
}
