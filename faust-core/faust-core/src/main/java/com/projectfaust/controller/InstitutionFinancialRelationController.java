package com.projectfaust.controller;

import com.projectfaust.entity.InstitutionAccountRelation;
import com.projectfaust.repository.InstitutionAccountRelationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/institution-financial-relations")
@RequiredArgsConstructor
public class InstitutionFinancialRelationController {

    private final InstitutionAccountRelationRepository relationRepository;

    /**
     * Okamžité odříznutí instituce od bankovního účtu (např. uzavření operačního fondu).
     */
    @PatchMapping("/{relationId}/deactivate")
    public ResponseEntity<Void> deactivateRelation(@PathVariable Long relationId) {
        log.info("FAUST_FIN: Deactivating institution account relation ID: {}", relationId);

        InstitutionAccountRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found: " + relationId));

        relation.setActive(false);
        relation.setValidTo(LocalDate.now());
        relationRepository.save(relation);

        return ResponseEntity.noContent().build();
    }

    /**
     * Změna časového určení, po které složka účet legálně či operačně využívala.
     */
    @PatchMapping("/{relationId}/temporal")
    public ResponseEntity<Void> updateTemporalValidity(
            @PathVariable Long relationId,
            @RequestParam(required = false) LocalDate validFrom,
            @RequestParam(required = false) LocalDate validTo) {

        log.info("FAUST_FIN: Updating temporal envelope for institution relation ID: {}", relationId);

        InstitutionAccountRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found: " + relationId));

        if (validFrom != null) relation.setValidFrom(validFrom);
        if (validTo != null) relation.setValidTo(validTo);

        relationRepository.save(relation);
        return ResponseEntity.ok().build();
    }
}