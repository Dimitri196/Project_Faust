package com.projectfaust.controller;

import com.projectfaust.dto.response.FinancialOperationsResponse;
import com.projectfaust.entity.enums.PersonAccountRole;
import com.projectfaust.mapper.InstitutionMapper;
import com.projectfaust.mapper.PersonMapper;
import com.projectfaust.repository.InstitutionAccountRelationRepository;
import com.projectfaust.repository.PersonAccountRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/financial-intelligence")
@RequiredArgsConstructor
public class BankAccountController {

    private final PersonAccountRelationRepository personRelationRepository;
    private final InstitutionAccountRelationRepository institutionRelationRepository;
    private final PersonMapper personMapper;
    private final InstitutionMapper institutionMapper;

    /**
     * Globální FININT vyhledávací vektor.
     * Slouží pro plošné prohledávání finančních vazeb napříč celým systémem Faust.
     */
    @GetMapping("/operations/search")
    public ResponseEntity<List<FinancialOperationsResponse>> searchFinancialOperations() {
        log.info("FAUST_FININT: Executing global financial operations dump");

        List<FinancialOperationsResponse> aggregatedResponses = new ArrayList<>();

        // 1. Vytáhneme personální finanční vazby a namapujeme je
        personRelationRepository.findAll().stream()
                .map(personMapper::toOperationsResponse)
                .forEach(aggregatedResponses::add);

        // 2. Vytáhneme institucionální finanční vazby a namapujeme je

        institutionRelationRepository.findAll().stream()
                .map(institutionMapper::toOperationsResponse)
                .forEach(aggregatedResponses::add);

        return ResponseEntity.ok(aggregatedResponses);
    }

    /**
     * Časová křížová analýza podezřelých rolí (např. skrytých vlastníků BENEFICIARY) v daném okně.
     * /From LYNCHY/ please, test all methods with different dates (takes all the timezones into account_based on the postreSQL project-faust)
     */
    @GetMapping("/operations/intersections")
    public ResponseEntity<List<FinancialOperationsResponse>> findIntersections(
            @RequestParam PersonAccountRole role,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        log.info("FAUST_FININT: Analyzing role intersections for role: {} between {} and {}", role, start, end);

        List<FinancialOperationsResponse> results = personRelationRepository.findIntersectionsByRoleAndTime(role, start, end).stream()
                .map(personMapper::toOperationsResponse)
                .toList();

        return ResponseEntity.ok(results);
    }
}