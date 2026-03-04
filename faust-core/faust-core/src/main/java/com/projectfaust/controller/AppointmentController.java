package com.projectfaust.controller;

import com.projectfaust.dto.request.AppointmentRequest;
import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.service.AppointmentService;
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
 * REST controller managing the lifecycle of formal appointments within Project Faust.
 * Provides endpoints for personnel deployment, bulk synchronization, and historical career tracking.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "Operations for assigning individuals to institutional positions")
public class AppointmentController {

    private final AppointmentService service;

    /**
     * Executes a single appointment sequence.
     * Links a subject to a specific functional role within an institution.
     *
     * @param request The appointment details including person and occupation identifiers.
     * @return 201 Created on successful deployment.
     */
    @PostMapping
    @Operation(summary = "Appoint a person to a position",
            description = "Links a person to an occupation. If the position is currently occupied, the previous appointment logic is triggered.")
    public ResponseEntity<Void> appoint(@Valid @RequestBody AppointmentRequest request) {
        service.appointPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Facilitates mass data ingestion of appointment records.
     * Optimized for synchronizing historical datasets from external intelligence sources.
     *
     * @param requests A list of appointment records to be processed in a bulk transaction.
     * @return 201 Created on successful batch processing.
     */
    @PostMapping("/bulk")
    @Operation(summary = "Bulk appointment import",
            description = "Registers multiple historical or current appointments in a single optimized transaction.")
    public ResponseEntity<Void> bulkAppoint(@Valid @RequestBody List<AppointmentRequest> requests) {
        service.bulkAppoint(requests);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Retrieves the chronological lineage of a specific institutional position.
     *
     * @param occupationId The unique identifier of the target occupation node.
     * @return A list of all historical and current occupants of the specified role.
     */
    @GetMapping("/occupation/{occupationId}/history")
    @Operation(summary = "Get appointment history for a position",
            description = "Returns a chronological log of all past and current occupants of a specific institutional chair.")
    public ResponseEntity<List<AppointmentResponse>> getHistory(@PathVariable UUID occupationId) {
        return ResponseEntity.ok(service.getHistoryByOccupation(occupationId));
    }

    /**
     * Retrieves the complete professional history (career dossier) of a specific subject.
     *
     * @param personId The unique identifier of the subject.
     * @return A list of all positions held by the individual across the system's timeline.
     */
    @GetMapping("/person/{personId}")
    @Operation(summary = "Get career history for a person",
            description = "Returns a detailed dossier of all positions held by a specific subject across the historical registry.")
    public ResponseEntity<List<AppointmentResponse>> getPersonHistory(@PathVariable UUID personId) {
        return ResponseEntity.ok(service.getHistoryByPerson(personId));
    }
}
