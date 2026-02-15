package com.projectfaust.service;

import com.projectfaust.dto.response.GlobalSearchResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;

    /**
     * Performs a high-performance hybrid search across institutions, people, and occupations.
     * Combines Postgres Full-Text Search (TSVECTOR) with Trigram-like fuzzy matching.
     */
    public List<GlobalSearchResponse> performGlobalSearch(String query, String vector) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        // 1. Normalize query (remove diacritics for the 'simple' dictionary)
        String rawQuery = normalizeString(query.trim());

        // 2. Format for Full Text Search (Prefix matching for each word)
        // Example: "Jaroslav Hrbek" -> "Jaroslav:* & Hrbek:*"
        String tsQuery = Arrays.stream(rawQuery.split("\\s+"))
                .filter(word -> word.length() >= 2)
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        // If the query is too short, we fall back to a simpler pattern
        if (tsQuery.isEmpty()) tsQuery = rawQuery + ":*";

        // 3. The Cinematic Hybrid SQL
        // We calculate rank based on: TS_RANK (40%), Prefix Match (40%), Substring Match (20%)
        String sql = """
            SELECT id, display_name, category, sub_label, 
                   (ts_rank(search_vector, to_tsquery('simple', unaccent(:tsQuery))) * 2.0) + 
                   (CASE WHEN lower(unaccent(display_name)) LIKE lower(unaccent(:prefixQuery)) THEN 1.5 ELSE 0 END) +
                   (CASE WHEN lower(unaccent(display_name)) LIKE lower(unaccent(:likeQuery)) THEN 0.5 ELSE 0 END) as rank
            FROM global_search_view
            WHERE (
                search_vector @@ to_tsquery('simple', unaccent(:tsQuery))
                OR lower(unaccent(display_name)) LIKE lower(unaccent(:likeQuery))
                OR lower(unaccent(display_name)) LIKE lower(unaccent(:prefixQuery))
            )
            AND (:vectorFilter = 'ALL' OR category = :vectorFilter)
            ORDER BY rank DESC
            LIMIT 50
        """;

        Query nativeQuery = entityManager.createNativeQuery(sql);
        nativeQuery.setParameter("tsQuery", tsQuery);
        nativeQuery.setParameter("likeQuery", "%" + rawQuery + "%");
        nativeQuery.setParameter("prefixQuery", rawQuery + "%");
        nativeQuery.setParameter("vectorFilter", vector == null ? "ALL" : vector.toUpperCase());

        @SuppressWarnings("unchecked")
        List<Object[]> results = nativeQuery.getResultList();

        return results.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Safely maps a raw database row to a GlobalSearchResponse.
     * Handles potential ClassCastExceptions for UUID and Numeric types.
     */
    private GlobalSearchResponse mapToResponse(Object[] row) {
        return new GlobalSearchResponse(
                convertToUUID(row[0]),                       // ID (UUID)
                (String) row[1],                             // Display Name
                (String) row[2],                             // Category
                (String) row[3],                             // Sub Label
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0 // Rank
        );
    }

    /**
     * Converts various DB types (String, UUID, byte[]) to java.util.UUID.
     * Prevents ClassCastException: Long cannot be cast to UUID.
     */
    private UUID convertToUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID) return (UUID) val;
        if (val instanceof String) return UUID.fromString((String) val);
        if (val instanceof byte[]) {
            // Some drivers return UUID as bytes
            return UUID.nameUUIDFromBytes((byte[]) val);
        }

        // If we still get a Long here, it means the VIEW is returning a numeric ID
        // instead of a UUID. Check the global_search_view definition!
        throw new IllegalArgumentException(
                "SearchService Error: Expected UUID compatible type, but got " + val.getClass().getName() +
                        ". Ensure your database view casts 'id' to UUID."
        );
    }

    /**
     * Standardizes string for matching (e.g., "Dvořák" -> "Dvorak")
     */
    private String normalizeString(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
}
