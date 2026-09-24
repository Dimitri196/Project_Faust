package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceService;
import com.projectfaust.traces.dto.request.SurveillanceEventRequest;
import com.projectfaust.traces.dto.response.SurveillanceEventResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces/surveillance")
@RequiredArgsConstructor
public class SurveillanceEventController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<SurveillanceEventResponse> create(
            @Valid @RequestBody SurveillanceEventRequest request) {
        SurveillanceEventResponse created = traceService.createSurveillance(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.externalId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<SurveillanceEventResponse> get(@PathVariable UUID eventId) {
        return ResponseEntity.ok(traceService.getSurveillance(eventId));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<SurveillanceEventResponse>> getByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(traceService.getSurveillanceByPerson(personId));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<SurveillanceEventResponse> update(
            @PathVariable UUID eventId,
            @Valid @RequestBody SurveillanceEventRequest request) {
        return ResponseEntity.ok(traceService.updateSurveillance(eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@PathVariable UUID eventId) {
        traceService.deleteSurveillance(eventId);
        return ResponseEntity.noContent().build();
    }
}
