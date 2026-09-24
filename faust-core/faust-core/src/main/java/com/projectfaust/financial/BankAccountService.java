package com.projectfaust.financial;

import com.projectfaust.financial.dto.BankAccountRequest;
import com.projectfaust.financial.dto.BankAccountResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application service for managing {@link BankAccount} entities.
 *
 * <p>Handles CRUD for the account master record itself. Relation management
 * (person ↔ account, institution ↔ account) is delegated to
 * {@link PersonFinancialRelationService} and {@link InstitutionFinancialRelationService}.</p>
 *
 * <p>Upsert: if an account with the same IBAN already exists on create,
 * the existing record is returned without duplication.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BankAccountService {

    private final BankAccountRepository repository;
    private final BankAccountMapper     mapper;

    // =========================================================================
    // Create
    // =========================================================================

    public BankAccountResponse create(BankAccountRequest dto) {
        String normalizedIban = normalizeIban(dto.iban());

        return repository.findByIban(normalizedIban)
                .map(existing -> {
                    log.info("FAUST_FINANCIAL_UPSERT: BankAccount with IBAN {} already exists — returning existing publicId={}",
                            normalizedIban, existing.getExternalId());
                    return mapper.toResponse(existing);
                })
                .orElseGet(() -> {
                    BankAccount entity = mapper.toEntity(dto);
                    entity.setIban(normalizedIban);
                    BankAccount saved = repository.save(entity);
                    log.info("FAUST_FINANCIAL_CREATE: BankAccount publicId={} IBAN={} bank={}",
                            saved.getExternalId(), saved.getIban(), saved.getBankName());
                    return mapper.toResponse(saved);
                });
    }

    // =========================================================================
    // Update
    // =========================================================================

    public BankAccountResponse update(UUID publicId, BankAccountRequest dto) {
        BankAccount entity = findOrThrow(publicId);
        mapper.updateEntity(dto, entity);
        if (dto.iban() != null) {
            entity.setIban(normalizeIban(dto.iban()));
        }
        log.info("FAUST_FINANCIAL_UPDATE: BankAccount publicId={} updated.", publicId);
        return mapper.toResponse(entity);
    }

    // =========================================================================
    // Delete
    // =========================================================================

    public void delete(UUID publicId) {
        BankAccount entity = findOrThrow(publicId);
        log.info("FAUST_FINANCIAL_DELETE: BankAccount publicId={} IBAN={}", publicId, entity.getIban());
        repository.delete(entity);
    }

    // =========================================================================
    // Read
    // =========================================================================

    @Transactional(readOnly = true)
    public BankAccountResponse getByPublicId(UUID publicId) {
        return mapper.toResponse(findOrThrow(publicId));
    }

    @Transactional(readOnly = true)
    public BankAccountResponse getByIban(String iban) {
        String normalized = normalizeIban(iban);
        return repository.findByIban(normalized)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("BankAccount not found for IBAN: " + normalized));
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getMonitored() {
        return repository.findByMonitoredTrue()
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getByCurrency(String currency) {
        return repository.findByCurrencyIgnoreCase(currency)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getByBankName(String bankName) {
        return repository.findByBankNameContainingIgnoreCase(bankName)
                .stream().map(mapper::toResponse).toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    BankAccount findOrThrow(UUID publicId) {
        return repository.findByExternalId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("BankAccount not found: " + publicId));
    }

    /**
     * Normalises IBAN to uppercase with whitespace removed.
     * e.g. "cz65 0800 0000 1920 0014 5399" → "CZ6508000000192000145399"
     */
    private String normalizeIban(String iban) {
        return iban.toUpperCase().replace(" ", "");
    }
}
