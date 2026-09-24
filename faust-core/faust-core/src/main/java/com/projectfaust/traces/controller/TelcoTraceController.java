package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceService;
import com.projectfaust.traces.dto.request.TelcoTraceRequest;
import com.projectfaust.traces.dto.response.TelcoTraceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces/telco")
@RequiredArgsConstructor
public class TelcoTraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<TelcoTraceResponse> create(@Valid @RequestBody TelcoTraceRequest request) {
        TelcoTraceResponse created = traceService.createTelco(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.externalId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<TelcoTraceResponse> get(@PathVariable UUID traceId) {
        return ResponseEntity.ok(traceService.getTelco(traceId));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<TelcoTraceResponse>> getByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(traceService.getTelcoByPerson(personId));
    }

    @PutMapping("/{traceId}")
    public ResponseEntity<TelcoTraceResponse> update(
            @PathVariable UUID traceId,
            @Valid @RequestBody TelcoTraceRequest request) {
        return ResponseEntity.ok(traceService.updateTelco(traceId, request));
    }

    @DeleteMapping("/{traceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID traceId) {
        traceService.deleteTelco(traceId);
        return ResponseEntity.noContent().build();
    }
}
