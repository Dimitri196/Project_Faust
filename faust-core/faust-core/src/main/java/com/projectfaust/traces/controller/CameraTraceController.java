package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceService;
import com.projectfaust.traces.dto.request.CameraTraceRequest;
import com.projectfaust.traces.dto.response.CameraTraceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces/camera")
@RequiredArgsConstructor
public class CameraTraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<CameraTraceResponse> create(@Valid @RequestBody CameraTraceRequest request) {
        CameraTraceResponse created = traceService.createCamera(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.externalId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<CameraTraceResponse> get(@PathVariable UUID traceId) {
        return ResponseEntity.ok(traceService.getCamera(traceId));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<CameraTraceResponse>> getByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(traceService.getCameraByPerson(personId));
    }

    @PutMapping("/{traceId}")
    public ResponseEntity<CameraTraceResponse> update(
            @PathVariable UUID traceId,
            @Valid @RequestBody CameraTraceRequest request) {
        return ResponseEntity.ok(traceService.updateCamera(traceId, request));
    }

    @DeleteMapping("/{traceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID traceId) {
        traceService.deleteCamera(traceId);
        return ResponseEntity.noContent().build();
    }
}
