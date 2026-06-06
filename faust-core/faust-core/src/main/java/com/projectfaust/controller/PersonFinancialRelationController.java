package com.projectfaust.controller;

import com.projectfaust.entity.PersonAccountRelation;
import com.projectfaust.repository.PersonAccountRelationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/v1/person-financial-relations")
@RequiredArgsConstructor
public class PersonFinancialRelationController {

    private final PersonAccountRelationRepository relationRepository;

    /**
     * Taktická deaktivace vazby (např. zmrazení přístupu k účtu v UI).
     * Mění příznak active na false, data v DB zůstávají kvůli auditní stopě (Envers).
     */
    @PatchMapping("/{relationId}/deactivate")
    public ResponseEntity<Void> deactivateRelation(@PathVariable Long relationId) {
        log.info("FAUST_FIN: Deactivating person account relation ID: {}", relationId);

        PersonAccountRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found: " + relationId));

        relation.setActive(false);
        relation.setValidTo(LocalDate.now()); // Automaticky uzavřeme časové okno k dnešku
        relationRepository.save(relation);

        return ResponseEntity.noContent().build();
    }

    /**
     * Aktualizace časového okna nebo poznámky konkrétní relace přímo z linkového dashboardu.
     */
    @PatchMapping("/{relationId}/temporal")
    public ResponseEntity<Void> updateTemporalValidity(
            @PathVariable Long relationId,
            @RequestParam(required = false) LocalDate validFrom,
            @RequestParam(required = false) LocalDate validTo) {

        log.info("FAUST_FIN: Updating temporal envelope for relation ID: {}", relationId);

        PersonAccountRelation relation = relationRepository.findById(relationId)
                .orElseThrow(() -> new EntityNotFoundException("Relation not found: " + relationId));

        if (validFrom != null) relation.setValidFrom(validFrom);
        if (validTo != null) relation.setValidTo(validTo);

        relationRepository.save(relation);
        return ResponseEntity.ok().build();
    }
}