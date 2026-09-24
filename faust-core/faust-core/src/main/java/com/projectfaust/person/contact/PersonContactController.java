package com.projectfaust.person.contact;

import com.projectfaust.person.contact.dto.ContactOperationsRequest;
import com.projectfaust.person.contact.dto.ContactOperationsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacts")
@RequiredArgsConstructor
@Tag(name = "Telemetry & Contact Vectors", description = "Target communication footprints and interception routing management")
public class PersonContactController {

    private final PersonContactService contactService;

    @PostMapping
    @Operation(summary = "Attach new communication vector", description = "Appends a standalone technical trace (e.g. GSM, Threema ID, Satellite Terminal) to an existing target identity.")
    public ResponseEntity<ContactOperationsResponse> createContact(@Valid @RequestBody ContactOperationsRequest request) {
        return new ResponseEntity<>(contactService.addContact(request), HttpStatus.CREATED);
    }

    @PutMapping("/{contactPublicId}")
    @Operation(summary = "Update vector deployment parameters", description = "Modifies an existing communication vector metrics, confidence levels, validation state, or operational lifespans.")
    public ResponseEntity<ContactOperationsResponse> updateContact(
            @PathVariable UUID contactPublicId,
            @Valid @RequestBody ContactOperationsRequest request) {
        return ResponseEntity.ok(contactService.updateContact(contactPublicId, request));
    }

    @GetMapping("/subject/{personPublicId}")
    @Operation(summary = "Retrieve entire telemetry dossier for a subject")
    public ResponseEntity<List<ContactOperationsResponse>> getByPerson(@PathVariable UUID personPublicId) {
        return ResponseEntity.ok(contactService.getContactsByPerson(personPublicId));
    }

    @DeleteMapping("/{contactPublicId}")
    @Operation(summary = "Purge contact vector entity", description = "Removes the technical telemetry block entirely from active tracking state indexes.")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID contactPublicId) {
        contactService.revokeContact(contactPublicId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contactPublicId}/leak-matrix")
    @Operation(summary = "Cross-reference infrastructure leaks", description = "Scans global intelligence space for cross-contamination. Returns records where other suspects shared this specific communication identifier node.")
    public ResponseEntity<List<ContactOperationsResponse>> detectInfrastructureLeaks(@PathVariable UUID contactPublicId) {
        return ResponseEntity.ok(contactService.checkSharedInfrastructure(contactPublicId));
    }
}
