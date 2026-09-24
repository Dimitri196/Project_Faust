package com.projectfaust.financial;

import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.financial.dto.InstitutionFinancialRelationRequest;
import com.projectfaust.financial.dto.InstitutionFinancialRelationResponse;
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
 * REST controller for institution ↔ bank account financial relations within Project Faust.
 *
 * <p>Base path: {@code /api/v1/institution-financial-relations}</p>
 *
 * <p>Manages the link between an {@code Institution} and a {@code BankAccount},
 * covering operational accounts, discretionary funds, and budgetary accounts
 * for public institutions.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/institution-financial-relations")
@RequiredArgsConstructor
@Tag(name = "Institution Financial Relations",
        description = "FININT — links between institutions and bank accounts. " +
                "Covers operational, discretionary, and budgetary account roles.")
public class InstitutionFinancialRelationController {

    private final InstitutionFinancialRelationService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Link an institution to a bank account",
            description = "Duplicate-guarded on (institutionPublicId, accountPublicId, roleType).")
    public ResponseEntity<InstitutionFinancialRelationResponse> link(
            @Valid @RequestBody InstitutionFinancialRelationRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.link(dto));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PatchMapping("/by-institution/{institutionPublicId}/account/{accountPublicId}/deactivate")
    @Operation(summary = "Deactivate an institution–account relation (soft delete)",
            description = "Sets active = false. The record is retained for audit and history.")
    public ResponseEntity<InstitutionFinancialRelationResponse> deactivate(
            @PathVariable UUID institutionPublicId,
            @PathVariable UUID accountPublicId) {
        return ResponseEntity.ok(service.deactivate(institutionPublicId, accountPublicId));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/by-institution/{institutionPublicId}/account/{accountPublicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hard-delete an institution–account relation",
            description = "Permanently removes the relation record. " +
                    "Prefer PATCH /deactivate for audit-trail preservation.")
    public ResponseEntity<Void> delete(
            @PathVariable UUID institutionPublicId,
            @PathVariable UUID accountPublicId) {
        service.delete(institutionPublicId, accountPublicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by institution ─────────────────────────────────────────────────

    @GetMapping("/by-institution/{institutionPublicId}")
    @Operation(summary = "Get all financial relations for an institution (including inactive)")
    public ResponseEntity<List<InstitutionFinancialRelationResponse>> getAllByInstitution(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getAllByInstitution(institutionPublicId));
    }

    @GetMapping("/by-institution/{institutionPublicId}/active")
    @Operation(summary = "Get active financial relations for an institution")
    public ResponseEntity<List<InstitutionFinancialRelationResponse>> getActiveByInstitution(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getActiveByInstitution(institutionPublicId));
    }

    // ── Read — by account ─────────────────────────────────────────────────────

    @GetMapping("/by-account/{accountPublicId}")
    @Operation(summary = "Get all institutions linked to a bank account",
            description = "Returns all institution relation records for the given account. " +
                    "Useful for identifying institutional account networks.")
    public ResponseEntity<List<InstitutionFinancialRelationResponse>> getByAccount(
            @PathVariable UUID accountPublicId) {
        return ResponseEntity.ok(service.getByAccount(accountPublicId));
    }

    // ── FININT aggregate ──────────────────────────────────────────────────────

    @GetMapping("/by-institution/{institutionPublicId}/profile")
    @Operation(summary = "Get FININT financial profile for an institution",
            description = "Returns all institution ↔ account relations with full account detail, " +
                    "role type, validity, and monitoring flag.")
    public ResponseEntity<List<FinancialOperationsResponse>> getFinancialProfile(
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getFinancialProfile(institutionPublicId));
    }
}
