package com.projectfaust.mobility;

import com.projectfaust.mobility.dto.MobilityEventRequestDto;
import com.projectfaust.mobility.dto.MobilityEventResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for mobility intelligence within Project Faust.
 *
 * <p>Base path: {@code /api/v1/mobility-events}</p>
 *
 * <p>Covers border crossings, parking and traffic violations, toll passages,
 * ANPR sightings, and checkpoint stops for persons and vehicles.</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/mobility-events")
@RequiredArgsConstructor
@Tag(name = "Mobility Events",
     description = "Movement intelligence — border crossings, traffic/parking violations, " +
                   "ANPR sightings, toll records, checkpoint stops.")
public class MobilityEventController {

    private final MobilityEventService service;

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Ingest a mobility event",
               description = "Upserts on (sourceSystem, sourceReferenceId). " +
                             "Subject person and vehicle are independently optional.")
    public ResponseEntity<MobilityEventResponseDto> create(
            @Valid @RequestBody MobilityEventRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk-ingest mobility events")
    public ResponseEntity<List<MobilityEventResponseDto>> createBulk(
            @Valid @RequestBody List<MobilityEventRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBulk(dtos));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{publicId}")
    @Operation(summary = "Update a mobility event",
               description = "Partial update. Useful for retroactive subject linking " +
                             "(e.g. linking a vehicle to a previously unresolved ANPR record).")
    public ResponseEntity<MobilityEventResponseDto> update(
            @PathVariable UUID publicId,
            @Valid @RequestBody MobilityEventRequestDto dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a mobility event")
    public ResponseEntity<Void> delete(@PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // ── Read — by ID ──────────────────────────────────────────────────────────

    @GetMapping("/{publicId}")
    @Operation(summary = "Get a mobility event by public ID")
    public ResponseEntity<MobilityEventResponseDto> getByPublicId(@PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // ── Read — by person ──────────────────────────────────────────────────────

    @GetMapping("/by-person/{personPublicId}")
    @Operation(summary = "Get all mobility events for a person (all types)")
    public ResponseEntity<List<MobilityEventResponseDto>> getByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getBySubjectPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/type/{eventType}")
    @Operation(summary = "Get mobility events for a person filtered by type")
    public ResponseEntity<List<MobilityEventResponseDto>> getByPersonAndType(
            @PathVariable UUID personPublicId,
            @PathVariable MobilityEventType eventType) {
        return ResponseEntity.ok(service.getBySubjectPersonAndType(personPublicId, eventType));
    }

    @GetMapping("/by-person/{personPublicId}/border-crossings")
    @Operation(summary = "Get all border crossing events for a person",
               description = "Reconstructs the subject's travel timeline across borders.")
    public ResponseEntity<List<MobilityEventResponseDto>> getBorderCrossingsByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getBorderCrossingsByPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/violations")
    @Operation(summary = "Get all traffic and parking violations for a person")
    public ResponseEntity<List<MobilityEventResponseDto>> getViolationsByPerson(
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getViolationsByPerson(personPublicId));
    }

    @GetMapping("/by-person/{personPublicId}/timeline")
    @Operation(summary = "Get a person's movement timeline within a time window",
               description = "Returns all event types in chronological order between `from` and `to`.")
    public ResponseEntity<List<MobilityEventResponseDto>> getTimeline(
            @PathVariable UUID personPublicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(service.getTimelineForPerson(personPublicId, from, to));
    }

    // ── Read — by vehicle ─────────────────────────────────────────────────────

    @GetMapping("/by-vehicle/{vehiclePublicId}")
    @Operation(summary = "Get all mobility events for a vehicle (all types)")
    public ResponseEntity<List<MobilityEventResponseDto>> getByVehicle(
            @PathVariable UUID vehiclePublicId) {
        return ResponseEntity.ok(service.getBySubjectVehicle(vehiclePublicId));
    }

    @GetMapping("/by-vehicle/{vehiclePublicId}/type/{eventType}")
    @Operation(summary = "Get mobility events for a vehicle filtered by type")
    public ResponseEntity<List<MobilityEventResponseDto>> getByVehicleAndType(
            @PathVariable UUID vehiclePublicId,
            @PathVariable MobilityEventType eventType) {
        return ResponseEntity.ok(service.getBySubjectVehicleAndType(vehiclePublicId, eventType));
    }

    // ── Read — by type ────────────────────────────────────────────────────────

    @GetMapping("/by-type/{eventType}")
    @Operation(summary = "Get mobility events by type across all subjects")
    public ResponseEntity<List<MobilityEventResponseDto>> getByType(
            @PathVariable MobilityEventType eventType) {
        return ResponseEntity.ok(service.getByEventType(eventType));
    }

    // ── Read — violations ─────────────────────────────────────────────────────

    @GetMapping("/violations/unpaid")
    @Operation(summary = "Get all unpaid fines (any subject)",
               description = "Returns parking and traffic violations where finePaid is false " +
                             "and fineAmount is set. Ordered by fineDueDate ascending.")
    public ResponseEntity<List<MobilityEventResponseDto>> getUnpaidViolations() {
        return ResponseEntity.ok(service.getUnpaidViolations());
    }

    // ── Read — ANPR / licence plate ───────────────────────────────────────────

    @GetMapping("/by-plate")
    @Operation(summary = "Get events by raw licence plate string",
               description = "Case-insensitive, space-normalised search on licensePlateRaw. " +
                             "Useful for retroactive vehicle linking after ANPR ingest.")
    public ResponseEntity<List<MobilityEventResponseDto>> getByLicensePlate(
            @RequestParam String licensePlate) {
        return ResponseEntity.ok(service.getByLicensePlate(licensePlate));
    }

    // ── Read — by location ────────────────────────────────────────────────────

    @GetMapping("/by-country/{countryCode}")
    @Operation(summary = "Get mobility events by location country code (ISO-3166-1 alpha-2)")
    public ResponseEntity<List<MobilityEventResponseDto>> getByCountry(
            @PathVariable String countryCode) {
        return ResponseEntity.ok(service.getByLocationCountry(countryCode));
    }
}
