package com.projectfaust.traces.controller;

import com.projectfaust.traces.TraceTimelineService;
import com.projectfaust.traces.dto.response.TraceTimelineResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Timeline endpoint — aggregates all trace types for a person into a
 * single chronological view.
 *
 * <p>This is the primary feed for the analyst dossier UI.</p>
 *
 * <pre>
 * GET /api/v1/traces/timeline/{personId}
 * GET /api/v1/traces/timeline/{personId}?from=2024-01-01T00:00:00&to=2024-12-31T23:59:59
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/traces/timeline")
@RequiredArgsConstructor
public class TraceTimelineController {

    private final TraceTimelineService timelineService;

    /**
     * Full timeline for a person — all types, all time.
     * Optionally filtered by {@code from} and {@code to} query parameters.
     *
     * @param personId externalId of the Person
     * @param from     optional start of date range (ISO 8601 local datetime)
     * @param to       optional end of date range (ISO 8601 local datetime)
     */
    @GetMapping("/{personId}")
    public ResponseEntity<TraceTimelineResponse> getTimeline(
            @PathVariable UUID personId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(timelineService.getTimeline(personId, from, to));
        }
        return ResponseEntity.ok(timelineService.getTimeline(personId));
    }
}
