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

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointment Management", description = "Operations for assigning people to specific positions")
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping
    @Operation(summary = "Appoint a person to a position",
            description = "Links a person to an occupation. If the position is currently occupied, the previous appointment is automatically closed.")
    public ResponseEntity<Void> appoint(@Valid @RequestBody AppointmentRequest request) {
        service.appointPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/occupation/{occupationId}/history")
    @Operation(summary = "Get appointment history for a position",
            description = "Returns all past and current occupants of a specific chair.")
    public ResponseEntity<List<AppointmentResponse>> getHistory(@PathVariable UUID occupationId) {
        return ResponseEntity.ok(service.getHistoryByOccupation(occupationId));
    }
}
