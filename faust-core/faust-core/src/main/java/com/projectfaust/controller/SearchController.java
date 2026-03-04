package com.projectfaust.controller;

import com.projectfaust.dto.response.GlobalSearchResponse;
import com.projectfaust.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
 */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Executes a weighted global search across people, institutions, and occupations.
     * Enforces a minimum character limit to optimize database performance and result relevance.
     *
     * @param query  The search string (minimum 3 characters required).
     * @param vector Filter for specific entity types (e.g., 'PERSON', 'INSTITUTION').
     * Defaults to 'ALL' for a cross-category search.
     * @return A ranked list of GlobalSearchResponse objects.
     */
    @GetMapping("/global")
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
