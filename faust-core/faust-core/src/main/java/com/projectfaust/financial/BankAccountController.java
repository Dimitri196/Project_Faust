package com.projectfaust.financial;

import com.projectfaust.financial.dto.BankAccountRequest;
import com.projectfaust.financial.dto.BankAccountResponse;
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
 * REST controller for bank account master records within Project Faust.
 *
 * <p>Base path: {@code /api/v1/bank-accounts}</p>
 *
 * <p>Manages the account entity itself. Relation management (person ↔ account,
 * institution ↔ account) is handled by dedicated controllers.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Accounts",
        description = "FININT — bank account master records. " +
                "Relation management via /person-financial-relations and " +
                "/institution-financial-relations.")
public class BankAccountController {

    private final BankAccountService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create or upsert a bank account",
            description = "Upserts on IBAN. If an account with the same (normalised) IBAN " +
                    "already exists, returns the existing record.")
    public ResponseEntity<BankAccountResponse> create(
            @Valid @RequestBody BankAccountRequest dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{publicId}")
    @Operation(summary = "Update a bank account")
    public ResponseEntity<BankAccountResponse> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody BankAccountRequest dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a bank account",
            description = "Cascades deletion to all linked person and institution relations " +
                    "(orphanRemoval = true on BankAccount).")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by ID ──────────────────────────────────────────────────────────

    @GetMapping("/{publicId}")
    @Operation(summary = "Get a bank account by public ID")
    public ResponseEntity<BankAccountResponse> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    @GetMapping("/by-iban")
    @Operation(summary = "Get a bank account by IBAN",
            description = "Case-insensitive, whitespace-normalised IBAN lookup.")
    public ResponseEntity<BankAccountResponse> getByIban(@RequestParam String iban) {
        return ResponseEntity.ok(service.getByIban(iban));
    }

    // ── Read — filtered ───────────────────────────────────────────────────────

    @GetMapping("/monitored")
    @Operation(summary = "Get all monitored accounts",
            description = "Returns accounts flagged for active FININT monitoring.")
    public ResponseEntity<List<BankAccountResponse>> getMonitored() {
        return ResponseEntity.ok(service.getMonitored());
    }

    @GetMapping("/by-currency/{currency}")
    @Operation(summary = "Get accounts by currency code (ISO 4217)")
    public ResponseEntity<List<BankAccountResponse>> getByCurrency(
            @PathVariable String currency) {
        return ResponseEntity.ok(service.getByCurrency(currency));
    }

    @GetMapping("/by-bank")
    @Operation(summary = "Get accounts by bank name (partial, case-insensitive)")
    public ResponseEntity<List<BankAccountResponse>> getByBankName(
            @RequestParam String bankName) {
        return ResponseEntity.ok(service.getByBankName(bankName));
    }
}
