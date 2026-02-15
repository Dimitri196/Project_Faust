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

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

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
