package com.projectfaust.appointment;

import com.projectfaust.appointment.dto.AppointmentRequest;
import com.projectfaust.appointment.dto.AppointmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller managing the lifecycle of formal appointments within Project Faust.
 *
 * <p>An appointment is the temporal link between a {@link com.projectfaust.person.Person}
 * and an {@link com.projectfaust.occupation.Occupation}. This controller exposes
 * endpoints for creating appointments, bulk ingestion of historical records,
 * and querying career history by person or position.</p>
 *
 * <p>Security tiers: {@code VIEWER} for all reads, {@code ANALYST} for single
 * appointments, {@code ADMIN} for bulk import.</p>
 *
 * <p>Base path: {@code /api/v1/appointments}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management",
        description = "Operations for assigning individuals to institutional positions")
public class AppointmentController {

    private final AppointmentService service;

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Creates a single appointment — assigns a person to a positional slot.
     *
     * <p>Validates security clearance before persisting. The position's vacancy
     * status is updated automatically if the appointment is ongoing.</p>
     *
     * @param request the appointment details including person and occupation UUIDs.
     * @return the created appointment with HTTP 201.
     */
    @PostMapping
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(summary = "Appoint a person to a position",
            description = "Links a person to an occupation with clearance validation. Updates position vacancy status.")
    public ResponseEntity<AppointmentResponse> appoint(
            @Valid @RequestBody AppointmentRequest request) {
        return new ResponseEntity<>(service.appointPerson(request), HttpStatus.CREATED);
    }

    /**
     * Bulk-imports multiple appointment records within a single transaction.
     *
     * <p>Optimised for historical dataset ingestion. Records that fail clearance
     * validation are skipped and logged — remaining valid records are committed.
     * Maximum 1000 records per request.</p>
     *
     * @param requests list of appointment records (max 1000).
     * @return HTTP 201 on successful batch processing.
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk appointment import",
            description = "Registers multiple appointments atomically. Clearance failures are skipped, not rolled back. Max 1000.")
    public ResponseEntity<Void> bulkAppoint(
            @Valid @RequestBody
            @Size(max = 1000, message = "Bulk import is limited to 1000 appointments per request.")
            List<@Valid AppointmentRequest> requests) {
        service.bulkAppoint(requests);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    /**
     * Returns the chronological history of all persons who held a specific position.
     *
     * @param occupationId the public UUID of the target position.
     * @return chronological list of appointments for the position, newest first.
     */
    @GetMapping("/occupation/{occupationId}/history")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get appointment history for a position",
            description = "Returns a chronological log of all past and current occupants of a specific position.")
    public ResponseEntity<List<AppointmentResponse>> getOccupationHistory(
            @PathVariable UUID occupationId) {
        return ResponseEntity.ok(service.getHistoryByOccupation(occupationId));
    }

    /**
     * Returns the full career dossier of a specific person — all positions they have held.
     *
     * @param personId the public UUID of the target person.
     * @return chronological career history, newest first.
     */
    @GetMapping("/person/{personId}/history")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get career history for a person",
            description = "Returns all positions held by a specific person across the historical registry.")
    public ResponseEntity<List<AppointmentResponse>> getPersonHistory(
            @PathVariable UUID personId) {
        return ResponseEntity.ok(service.getHistoryByPerson(personId));
    }

    /**
     * Returns all appointments currently active system-wide where the holder
     * is in an acting (temporary) capacity.
     *
     * <p>Acting appointments signal power transitions and structural instability
     * in the command chain. Used by the intelligence dashboard.</p>
     *
     * @return list of all active acting appointments.
     */
    @GetMapping("/acting")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get all active acting appointments",
            description = "Surfaces all positions currently held in a temporary/acting capacity. Key intelligence signal.")
    public ResponseEntity<List<AppointmentResponse>> getActiveActingAppointments() {
        return ResponseEntity.ok(service.getActiveActingAppointments());
    }

    // =========================================================================
// PATCH: AppointmentController.java
// Add three new endpoints for the org chart timeline.
// =========================================================================

    /**
     * Returns all appointments active at a specific point in time for an institution.
     * The frontend org chart uses this to render the structure at any historical date.
     *
     * @param institutionId the public UUID of the institution.
     * @param atDate        ISO date string (e.g. "1975-03-01"). Defaults to today.
     * @return appointments active at that date, with occupation and person details.
     */
    @GetMapping("/institution/{institutionId}")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get institution structure at a point in time",
            description = "Returns all active appointments within an institution at the given date. " +
                    "Used by the org chart timeline component. Defaults to today if atDate omitted.")
    public ResponseEntity<List<AppointmentResponse>> getByInstitutionAtDate(
            @PathVariable UUID institutionId,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate atDate) {
        return ResponseEntity.ok(service.getByInstitutionAtDate(institutionId, atDate));
    }

    /**
     * Returns all significant dates (appointment start dates) for an institution.
     * Used to populate timeline scrubber tick marks.
     *
     * @param institutionId the public UUID of the institution.
     * @return sorted list of ISO date strings.
     */
    @GetMapping("/institution/{institutionId}/dates")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Get significant dates for institution timeline",
            description = "Returns all appointment start dates — used to place tick marks on the timeline scrubber.")
    public ResponseEntity<List<LocalDate>> getSignificantDates(
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(service.getSignificantDates(institutionId));
    }

}
