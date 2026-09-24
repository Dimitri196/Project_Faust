package com.projectfaust.vehicle;

import com.projectfaust.vehicle.dto.VehicleRecordRequestDto;
import com.projectfaust.vehicle.dto.VehicleRecordResponseDto;
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
 * REST controller for vehicle record management within Project Faust.
 *
 * <p>Exposes CRUD operations plus intelligence-oriented queries by owner,
 * operator, VIN, and licence plate. All responses use the public
 * {@code externalId} (UUID) — internal primary keys are never exposed.</p>
 *
 * <p>Base path: {@code /api/v1/vehicle-records}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/vehicle-records")
@RequiredArgsConstructor
@Tag(name = "Vehicle Records", description = "HUMINT vehicle intelligence — ownership, operator linkage, STK status, and financial profile.")
public class VehicleRecordController {

    private final VehicleRecordService service;

    // =========================================================================
    // Create
    // =========================================================================

    @PostMapping
    @Operation(
            summary     = "Ingest a vehicle record",
            description = "Creates a new vehicle record. If the (sourceSystem, sourceReferenceId) pair " +
                          "already exists, the existing record is updated (upsert)."
    )
    public ResponseEntity<VehicleRecordResponseDto> create(
            @Valid @RequestBody VehicleRecordRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(dto));
    }

    @PostMapping("/bulk")
    @Operation(
            summary     = "Bulk-ingest vehicle records",
            description = "Processes a list of vehicle records. Each item is handled independently " +
                          "with its own idempotency guard."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> createBulk(
            @Valid @RequestBody List<VehicleRecordRequestDto> dtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBulk(dtos));
    }

    // =========================================================================
    // Update
    // =========================================================================

    @PutMapping("/{publicId}")
    @Operation(
            summary     = "Partially update a vehicle record",
            description = "Updates only the non-null fields supplied in the request body."
    )
    public ResponseEntity<VehicleRecordResponseDto> update(
            @Parameter(description = "Public UUID of the vehicle record.")
            @PathVariable UUID publicId,
            @Valid @RequestBody VehicleRecordRequestDto dto) {
        return ResponseEntity.ok(service.update(publicId, dto));
    }

    // =========================================================================
    // Delete
    // =========================================================================

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary     = "Delete a vehicle record",
            description = "Permanently removes the record and its audit trail from the active dataset."
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "Public UUID of the vehicle record.")
            @PathVariable UUID publicId) {
        service.delete(publicId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // Read — by public ID
    // =========================================================================

    @GetMapping("/{publicId}")
    @Operation(
            summary = "Get a vehicle record by public ID",
            description = "Returns the full HUMINT profile of a vehicle, including resolved owner, " +
                          "operator, STK status, financial flags, and ownership history."
    )
    public ResponseEntity<VehicleRecordResponseDto> getByPublicId(
            @Parameter(description = "Public UUID of the vehicle record.")
            @PathVariable UUID publicId) {
        return ResponseEntity.ok(service.getByPublicId(publicId));
    }

    // =========================================================================
    // Read — by owner (person)
    // =========================================================================

    @GetMapping("/by-owner-person/{personPublicId}")
    @Operation(
            summary     = "Get all vehicles owned by a person",
            description = "Returns all vehicle records where the registered owner is the specified person, " +
                          "ordered by registration date descending."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getByOwnerPerson(
            @Parameter(description = "Public UUID of the owning person.")
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getByOwnerPerson(personPublicId));
    }

    @GetMapping("/by-owner-person/{personPublicId}/active")
    @Operation(
            summary = "Get active vehicles owned by a person",
            description = "Returns only currently registered (active) vehicles owned by the specified person."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getActiveByOwnerPerson(
            @Parameter(description = "Public UUID of the owning person.")
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getActiveByOwnerPerson(personPublicId));
    }

    // =========================================================================
    // Read — by operator (person)
    // =========================================================================

    @GetMapping("/by-operator-person/{personPublicId}")
    @Operation(
            summary     = "Get all vehicles operated by a person",
            description = "Returns vehicles where the specified person is the registered operator " +
                          "(may differ from the owner — common in corporate fleet and leasing structures)."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getByOperatorPerson(
            @Parameter(description = "Public UUID of the operating person.")
            @PathVariable UUID personPublicId) {
        return ResponseEntity.ok(service.getByOperatorPerson(personPublicId));
    }

    // =========================================================================
    // Read — by owner (institution)
    // =========================================================================

    @GetMapping("/by-owner-institution/{institutionPublicId}")
    @Operation(
            summary     = "Get fleet owned by an institution",
            description = "Returns all vehicles in the fleet of the specified institution."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getByOwnerInstitution(
            @Parameter(description = "Public UUID of the owning institution.")
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getByOwnerInstitution(institutionPublicId));
    }

    @GetMapping("/by-owner-institution/{institutionPublicId}/active")
    @Operation(
            summary = "Get active fleet owned by an institution",
            description = "Returns only currently registered vehicles in the institution's fleet."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getActiveByOwnerInstitution(
            @Parameter(description = "Public UUID of the owning institution.")
            @PathVariable UUID institutionPublicId) {
        return ResponseEntity.ok(service.getActiveByOwnerInstitution(institutionPublicId));
    }

    // =========================================================================
    // Read — by VIN / licence plate
    // =========================================================================

    @GetMapping("/by-vin/{vin}")
    @Operation(
            summary     = "Look up vehicles by VIN",
            description = "Returns all records matching the given 17-character VIN. " +
                          "Multiple results indicate data-quality collisions across source systems."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getByVin(
            @Parameter(description = "17-character Vehicle Identification Number (VIN). Case-insensitive.")
            @PathVariable String vin) {
        return ResponseEntity.ok(service.getByVin(vin));
    }

    @GetMapping("/by-plate")
    @Operation(
            summary     = "Look up vehicles by licence plate and country",
            description = "Returns all records matching the plate within the given country. " +
                          "Multiple results are expected for plates reissued over time."
    )
    public ResponseEntity<List<VehicleRecordResponseDto>> getByLicensePlate(
            @Parameter(description = "Licence plate (normalised to upper case, spaces removed).")
            @RequestParam String licensePlate,
            @Parameter(description = "ISO 3166-1 alpha-2 country code of the issuing registry.")
            @RequestParam String countryCode) {
        return ResponseEntity.ok(service.getByLicensePlate(licensePlate, countryCode));
    }
}
