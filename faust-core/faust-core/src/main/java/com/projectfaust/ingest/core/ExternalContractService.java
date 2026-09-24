package com.projectfaust.ingest.core;

import com.projectfaust.ingest.dto.ExternalContractResponse;
import com.projectfaust.ingest.hlidacstatu.ExternalContractFetchService;
import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionIdentifier;
import com.projectfaust.institution.InstitutionIdentifierRepository;
import com.projectfaust.institution.InstitutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Read-side service for public contract intelligence within Project Faust.
 *
 * <p>FIXED: pagination was applied twice — first at repository level, then
 * again manually with subList. This caused incorrect result counts (e.g.
 * 24 instead of 25). Now fetches all results unpaginated from the DB,
 * merges and sorts them in memory, then applies a single pagination step.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalContractService {

    private final ExternalContractRepository contractRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionIdentifierRepository identifierRepository;
    private final ExternalContractFetchService fetchService;

    // -------------------------------------------------------------------------
    // Institution-scoped query
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ExternalContractResponse> findByInstitution(
            UUID institutionPublicId,
            String supplierSearch,
            boolean fetchIfEmpty,
            Pageable pageable
    ) {
        Institution institution = institutionRepository.findByExternalId(institutionPublicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Institution not found: " + institutionPublicId));

        List<InstitutionIdentifier> identifiers = identifierRepository
                .findActiveNationalRegistrationsByInstitutionAndCountry(
                        institution.getId(), "CZ");

        if (identifiers.isEmpty()) {
            log.debug("CONTRACT_QUERY: No national registration identifier for institution {}.",
                    institutionPublicId);
            return Page.empty(pageable);
        }

        List<String> registrationNumbers = identifiers.stream()
                .map(InstitutionIdentifier::getValue)
                .toList();

        // Fetch-on-demand — trigger a fresh Hlidač Státu pull when no contracts exist
        if (fetchIfEmpty) {
            for (String registrationNumber : registrationNumbers) {
                if (contractRepository.countByRegistrationNumber(registrationNumber) == 0) {
                    log.info("CONTRACT_QUERY: No contracts for {} — triggering fetch.",
                            registrationNumber);
                    fetchService.fetchContractsForIco(registrationNumber);
                }
            }
        }

        boolean hasSupplierFilter = supplierSearch != null && !supplierSearch.isBlank();

        // Use Pageable.unpaged() to fetch ALL results without DB-level truncation.
        // Integer.MAX_VALUE causes issues with some JPA providers and PostgreSQL
        // query planners. Pageable.unpaged() is the correct way to fetch everything.
        Pageable unpaged = org.springframework.data.domain.Pageable.unpaged();

        List<ExternalContractResponse> allResults = new ArrayList<>();

        for (String registrationNumber : registrationNumbers) {
            List<ExternalContract> contracts = hasSupplierFilter
                    ? contractRepository.findByBuyerAndSupplierSearch(
                    registrationNumber, supplierSearch.trim(), unpaged).getContent()
                    : contractRepository.findByBuyerIcoOrSupplierIco(
                    registrationNumber, registrationNumber, unpaged).getContent();

            contracts.stream()
                    .map(c -> toResponse(c, registrationNumber))
                    .forEach(allResults::add);
        }

        // Deduplicate by externalId — same contract can appear from both
        // buyer and supplier queries if the institution appears on both sides
        List<ExternalContractResponse> deduplicated = allResults.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ExternalContractResponse::externalId,
                        r -> r,
                        (a, b) -> a))  // keep first occurrence
                .values()
                .stream()
                .sorted(Comparator.comparing(
                        ExternalContractResponse::contractDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        // Apply pagination once on the fully merged and deduplicated list
        int total = deduplicated.size();
        int start = (int) pageable.getOffset();
        int end   = Math.min(start + pageable.getPageSize(), total);

        List<ExternalContractResponse> pageContent =
                start >= total ? List.of() : deduplicated.subList(start, end);

        log.debug("CONTRACT_QUERY: Institution {} — {} total contracts, page {}/{}.",
                institutionPublicId, total,
                pageable.getPageNumber() + 1,
                (total + pageable.getPageSize() - 1) / pageable.getPageSize());

        return new PageImpl<>(pageContent, pageable, total);
    }

    // -------------------------------------------------------------------------
    // Cross-institution supplier search
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<ExternalContractResponse> searchBySupplier(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }
        log.info("CONTRACT_QUERY: Cross-institution supplier search for '{}'.", query);
        return contractRepository.searchBySupplier(query.trim(), pageable)
                .map(c -> toResponse(c, c.getBuyerIco()));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private ExternalContractResponse toResponse(
            ExternalContract contract, String registrationNumber) {

        boolean isBuyer    = registrationNumber != null
                && registrationNumber.equals(contract.getBuyerIco());
        boolean isSupplier = registrationNumber != null
                && registrationNumber.equals(contract.getSupplierIco());

        ExternalContractResponse.ContractRole role =
                (isBuyer && isSupplier) ? ExternalContractResponse.ContractRole.BOTH
                        : isBuyer       ? ExternalContractResponse.ContractRole.BUYER
                        :                 ExternalContractResponse.ContractRole.SUPPLIER;

        return new ExternalContractResponse(
                contract.getId(),
                contract.getSourceSystem(),
                contract.getExternalId(),
                contract.getBuyerIco(),
                contract.getBuyerName(),
                contract.getSupplierIco(),
                contract.getSupplierName(),
                contract.getAmountTotal(),
                contract.getContractDate(),
                contract.getSubjectText(),
                contract.getVerificationStatus(),
                contract.getIngestedAt(),
                role
        );
    }
}