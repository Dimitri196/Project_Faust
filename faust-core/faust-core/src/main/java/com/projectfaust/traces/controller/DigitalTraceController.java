package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceService;
import com.projectfaust.traces.dto.request.DigitalTraceRequest;
import com.projectfaust.traces.dto.response.DigitalTraceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces/digital")
@RequiredArgsConstructor
public class DigitalTraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<DigitalTraceResponse> create(@Valid @RequestBody DigitalTraceRequest request) {
        DigitalTraceResponse created = traceService.createDigital(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.externalId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<DigitalTraceResponse> get(@PathVariable UUID traceId) {
        return ResponseEntity.ok(traceService.getDigital(traceId));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<DigitalTraceResponse>> getByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(traceService.getDigitalByPerson(personId));
    }

    @PutMapping("/{traceId}")
    public ResponseEntity<DigitalTraceResponse> update(
            @PathVariable UUID traceId,
            @Valid @RequestBody DigitalTraceRequest request) {
        return ResponseEntity.ok(traceService.updateDigital(traceId, request));
    }

    @DeleteMapping("/{traceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID traceId) {
        traceService.deleteDigital(traceId);
        return ResponseEntity.noContent().build();
    }
}
