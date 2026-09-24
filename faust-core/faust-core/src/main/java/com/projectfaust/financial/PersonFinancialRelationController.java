package com.projectfaust.financial;

import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.financial.dto.PersonFinancialRelationRequest;
import com.projectfaust.financial.dto.PersonFinancialRelationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for person ↔ bank account financial relations within Project Faust.
 *
 * <p>Base path: {@code /api/v1/person-financial-relations}</p>
 *
 * <p>Manages the link between a {@code Person} and a {@code BankAccount},
 * including role assignment, validity periods, and the consolidated
 * FININT financial profile for a person.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/person-financial-relations")
@RequiredArgsConstructor
@Tag(name = "Person Financial Relations",
        description = "FININT — links between persons and bank accounts. " +
                "Includes financial profile aggregation per person.")
public class PersonFinancialRelationController {

    private final PersonFinancialRelationService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Link a person to a bank account",
            description = "Duplicate-guarded on (personPublicId, accountPublicId, roleType). " +
                    "Returns existing record if the triple already exists.")
    public ResponseEntity<PersonFinancialRelationResponse> link(
            @Valid @RequestBody PersonFinancialRelationRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.link(dto));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PatchMapping("/{relationPublicId}/deactivate")
    @Operation(summary = "Deactivate a person–account relation (soft delete)",
            description = "Sets active = false. The record is retained for audit and history.")
    public ResponseEntity<PersonFinancialRelationResponse> deactivate(
            @PathVariable UUID relationPublicId) {
        return ResponseEntity.ok(service.deactivate(relationPublicId));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{relationPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hard-delete a person–account relation",
            description = "Permanently removes the relation record. " +
                    "Prefer PATCH /deactivate for audit-trail preservation.")
    public ResponseEntity<Void> delete(@PathVariable UUID relationPublicId) {
        service.delete(relationPublicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by person ──────────────────────────────────────────────────────

    @GetMapping("/by-person/{personPublicId}")
    @Operation(summary = "Get all financial relations for a person (including inactive)")
    public ResponseEntity<List<PersonFinancialRelationResponse>> getAllByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getAllByPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/active")
    @Operation(summary = "Get active financial relations for a person")
    public ResponseEntity<List<PersonFinancialRelationResponse>> getActiveByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getActiveByPerson(personPublicId));
    }

    // ── Read — by account ─────────────────────────────────────────────────────

    @GetMapping("/by-account/{accountPublicId}")
    @Operation(summary = "Get all persons linked to a bank account",
            description = "Returns all relation records for the given account, " +
                    "useful for identifying co-holders and beneficial owners.")
    public ResponseEntity<List<PersonFinancialRelationResponse>> getByAccount(
            @PathVariable UUID accountPublicId) {
        return ResponseEntity.ok(service.getByAccount(accountPublicId));
    }

    // ── FININT aggregate ──────────────────────────────────────────────────────

    @GetMapping("/by-person/{personPublicId}/profile")
    @Operation(summary = "Get consolidated FININT financial profile for a person",
            description = "Returns all accounts with role, active status, monitored flag, " +
                    "and aggregate counts. Used for the intelligence dossier view.")
    public ResponseEntity<List<FinancialOperationsResponse>> getFinancialProfile(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getFinancialProfile(personPublicId));
    }
}
