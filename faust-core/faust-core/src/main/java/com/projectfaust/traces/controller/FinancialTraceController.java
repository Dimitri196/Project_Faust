package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceService;
import com.projectfaust.traces.dto.request.FinancialTraceRequest;
import com.projectfaust.traces.dto.response.FinancialTraceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/traces/financial")
@RequiredArgsConstructor
public class FinancialTraceController {

    private final TraceService traceService;

    @PostMapping
    public ResponseEntity<FinancialTraceResponse> create(@Valid @RequestBody FinancialTraceRequest request) {
        FinancialTraceResponse created = traceService.createFinancial(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.externalId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{traceId}")
    public ResponseEntity<FinancialTraceResponse> get(@PathVariable UUID traceId) {
        return ResponseEntity.ok(traceService.getFinancial(traceId));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<FinancialTraceResponse>> getByPerson(@PathVariable UUID personId) {
        return ResponseEntity.ok(traceService.getFinancialByPerson(personId));
    }

    @PutMapping("/{traceId}")
    public ResponseEntity<FinancialTraceResponse> update(
            @PathVariable UUID traceId,
            @Valid @RequestBody FinancialTraceRequest request) {
        return ResponseEntity.ok(traceService.updateFinancial(traceId, request));
    }

    @DeleteMapping("/{traceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID traceId) {
        traceService.deleteFinancial(traceId);
        return ResponseEntity.noContent().build();
    }
}
