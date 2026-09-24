package com.projectfaust.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * REST controller providing a unified search interface for the Faust registry.
 * Interfaces with the PostgreSQL Full-Text Search (FTS) engine to provide
 * ranked results across all data categories.
 *
 * <p>Base path: {@code /api/v1/search}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Global Search", description = "Cross-category full-text search")
public class SearchController {

    private final SearchService searchService;

    /**
     * Executes a weighted global search across people, institutions, and occupations.
     * Enforces a minimum character limit to optimize database performance and result relevance.
     *
     * <p><b>Access control note:</b> {@code hasRole('VIEWER')} is the same floor
     * applied to every other read endpoint in Project Faust
     * (Location, Institution, Occupation, Person controllers). Without this,
     * the endpoint relied solely on {@code anyRequest().authenticated()} in
     * {@link com.projectfaust.config.SecurityConfig} — any valid JWT, regardless
     * of role, could search the entire registry.</p>
     *
     * <p><b>Known limitation:</b> {@code global_search_view} does not currently
     * filter results by the requester's {@code clearance} against each record's
     * {@code clearanceLevel}/{@code requiredClearanceLevel}. A VIEWER-level
     * operator can therefore see the {@code displayName}/{@code category}/
     * {@code subLabel} of LEVEL_5_TOP_SECRET records in search results, even
     * though they couldn't open the full record via its dedicated endpoint
     * (which does enforce clearance). This is a separate, larger fix —
     * either the view needs a clearance column joined in and filtered here,
     * or results need post-filtering against the authenticated principal's
     * clearance before being returned.</p>
     *
     * @param query  The search string (minimum 3 characters required).
     * @param vector Filter for specific entity types (e.g., 'PERSON', 'INSTITUTION').
     *               Defaults to 'ALL' for a cross-category search.
     * @return A ranked list of GlobalSearchResponse objects.
     */
    @GetMapping("/global")
    @PreAuthorize("hasRole('VIEWER')")
    @Operation(summary = "Global cross-category search",
            description = "Ranked full-text search across institutions, personnel, and occupations. " +
                    "Requires a minimum 3-character query.")
    public ResponseEntity<List<GlobalSearchResponse>> executeGlobalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "ALL") String vector) {

        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<GlobalSearchResponse> results = searchService.performGlobalSearch(query, vector);
        return ResponseEntity.ok(results);
    }
}