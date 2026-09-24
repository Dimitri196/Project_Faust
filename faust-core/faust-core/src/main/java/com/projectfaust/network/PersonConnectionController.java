package com.projectfaust.network;


import com.projectfaust.person.dto.PersonConnectionRequest;
import com.projectfaust.person.dto.PersonConnectionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for HUMINT connection CRUD — directed edges of the
 * Project Faust relationship graph.
 *
 * <p>Base path: {@code /api/v1/connections}</p>
 *
 * <p>Graph analytics (shortest path, influence map, network analysis)
 * are served by {@link NetworkAnalyticsController} at
 * {@code /api/v1/network}.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/connections")
@RequiredArgsConstructor
@Tag(name = "HUMINT Connections",
     description = "Directed relationship edges between persons in the intelligence graph. " +
                   "CRUD operations — for analytics see /api/v1/network.")
public class PersonConnectionController {

    private final PersonConnectionService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Create a directed connection between two persons",
            description = "Duplicate-guarded on (sourcePersonId, targetPersonId, connectionType). " +
                          "Influence score defaults to the connection type's built-in weight if not provided.")
    public ResponseEntity<PersonConnectionResponse> create(
            @Valid @RequestBody PersonConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{connectionId}")
    @Operation(
            summary = "Update a connection's mutable fields",
            description = "Updates description, influence score, dates, and verification status. " +
                          "Source, target, and connection type are immutable — delete and recreate to change them.")
    public ResponseEntity<PersonConnectionResponse> update(
            @PathVariable UUID connectionId,
            @Valid @RequestBody PersonConnectionRequest request) {
        return ResponseEntity.ok(service.update(connectionId, request));
    }

    // ── Soft deactivate ───────────────────────────────────────────────────────

    @PatchMapping("/{connectionId}/deactivate")
    @Operation(
            summary = "Deactivate a connection (soft delete)",
            description = "Sets endDate = today. The record is retained for audit and historical graph analysis. " +
                          "Prefer this over DELETE for intelligence continuity.")
    public ResponseEntity<PersonConnectionResponse> deactivate(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(service.deactivate(connectionId));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{connectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Hard-delete a connection",
            description = "Permanently removes the connection record. " +
                          "Prefer PATCH /deactivate for audit-trail preservation.")
    public ResponseEntity<Void> delete(@PathVariable UUID connectionId) {
        service.delete(connectionId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — single ─────────────────────────────────────────────────────────

    @GetMapping("/{connectionId}")
    @Operation(summary = "Get a connection by its public UUID")
    public ResponseEntity<PersonConnectionResponse> getById(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(service.getById(connectionId));
    }

    // ── Read — by person ─────────────────────────────────────────────────────

    @GetMapping("/by-person/{personId}")
    @Operation(
            summary = "Get all connections for a person (outgoing + incoming)",
            description = "Returns the full set of directed connections where this person " +
                          "appears as source or target.")
    public ResponseEntity<List<PersonConnectionResponse>> getAllByPerson(
            @PathVariable UUID personId) {
        return ResponseEntity.ok(service.getAllByPerson(personId));
    }

    @GetMapping("/by-person/{personId}/outgoing")
    @Operation(
            summary = "Get outgoing connections from a person",
            description = "Returns connections where this person is the source node.")
    public ResponseEntity<List<PersonConnectionResponse>> getOutgoing(
            @PathVariable UUID personId) {
        return ResponseEntity.ok(service.getOutgoing(personId));
    }

    @GetMapping("/by-person/{personId}/incoming")
    @Operation(
            summary = "Get incoming connections to a person",
            description = "Returns connections where this person is the target node.")
    public ResponseEntity<List<PersonConnectionResponse>> getIncoming(
            @PathVariable UUID personId) {
        return ResponseEntity.ok(service.getIncoming(personId));
    }
}
