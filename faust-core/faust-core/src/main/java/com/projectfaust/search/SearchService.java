package com.projectfaust.search;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service providing high-performance hybrid search capabilities across the Project Faust registry.
 * Combines PostgreSQL Full-Text Search (FTS) with weighted pattern matching.
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;

    /**
     * Executes a hybrid search across institutions, personnel, and occupations.
     * Utilizes TSVECTOR for linguistics and trigram-inspired LIKE patterns for partial matches.
     *
     * @param query  The raw search string provided by the user.
     * @param vector Filter for a specific data category (e.g., 'PERSON', 'INSTITUTION') or 'ALL'.
     * @return A ranked list of search results.
     */
    @SuppressWarnings("unchecked")
    public List<GlobalSearchResponse> performGlobalSearch(String query, String vector) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String rawQuery = normalizeString(query.trim());

        // Build a tsquery: "John Doe" -> "John:* & Doe:*" for partial word matching.
        //
        // NEW: each word is stripped of characters that have special meaning
        // in PostgreSQL's tsquery syntax (& | ! ( ) : *) before the ":*"
        // suffix is appended. Without this, input like "John & (Doe" or a
        // lone "&" produces a malformed tsquery string (e.g. "&:* & (Doe:*"),
        // and to_tsquery() throws a syntax error at the database level —
        // an uncaught PSQLException that bubbles up as a generic 500 via
        // GlobalExceptionHandler. Stripping these characters means the
        // worst case is an empty word (filtered out by the length check),
        // never a syntax error.
        String tsQuery = Arrays.stream(rawQuery.split("\\s+"))
                .map(SearchService::sanitizeTsQueryWord)
                .filter(word -> word.length() >= 2)
                .map(word -> word + ":*")
                .collect(Collectors.joining(" & "));

        if (tsQuery.isEmpty()) {
            String sanitizedWhole = sanitizeTsQueryWord(rawQuery);
            tsQuery = sanitizedWhole.isEmpty() ? null : sanitizedWhole + ":*";
        }

        // If sanitization removed everything (e.g. query was only symbols
        // like "***" or "&&&"), there's nothing meaningful to search for.
        if (tsQuery == null) {
            return List.of();
        }

        // SQL logic: Ranks results based on FTS relevance (2.0), Prefix (1.5), and Substring (0.5)
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

        List<Object[]> results = nativeQuery.getResultList();

        return results.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Maps raw database records to the GlobalSearchResponse DTO.
     * Ensures defensive type conversion for diverse database return types.
     */
    private GlobalSearchResponse mapToResponse(Object[] row) {
        return new GlobalSearchResponse(
                convertToUUID(row[0]),                       // ID
                (String) row[1],                             // Display Name
                (String) row[2],                             // Category
                (String) row[3],                             // Sub Label
                row[4] != null ? ((Number) row[4]).doubleValue() : 0.0 // Relevance Rank
        );
    }

    /**
     * Facilitates type-safe conversion of ID objects to java.util.UUID.
     * Handles variations in JDBC driver behavior (Strings vs. UUID vs. byte[]).
     */
    private UUID convertToUUID(Object val) {
        if (val == null) return null;
        if (val instanceof UUID) return (UUID) val;
        if (val instanceof String) return UUID.fromString((String) val);
        if (val instanceof byte[]) {
            return UUID.nameUUIDFromBytes((byte[]) val);
        }

        throw new IllegalArgumentException(
                "SearchService Integrity Error: Expected UUID-compatible type, but received " + val.getClass().getName() +
                        ". Verify that the 'global_search_view' casts the 'id' column to UUID."
        );
    }

    /**
     * Normalizes strings by removing diacritics and combining marks (e.g., "Dvořák" -> "Dvorak").
     * Crucial for consistent searching across phonetic variations.
     */
    private String normalizeString(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * NEW: strips characters with special meaning in PostgreSQL's tsquery
     * syntax — {@code & | ! ( ) : *} — from a single word before the
     * {@code :*} prefix-match suffix is appended.
     *
     * <p>Without this, a query containing these characters (e.g. "AT&T",
     * "(classified)", or a lone "&") produces a malformed tsquery string
     * that {@code to_tsquery()} rejects with a syntax error, surfaced as
     * an uncaught {@code PSQLException} -> generic 500.</p>
     *
     * <p>Only alphanumeric characters (including Unicode letters/digits,
     * since {@code rawQuery} may contain non-Latin scripts) are retained.</p>
     *
     * @param word a single whitespace-delimited token from the raw query.
     * @return the word with tsquery-special characters removed; may be empty.
     */
    private static String sanitizeTsQueryWord(String word) {
        return word.replaceAll("[^\\p{L}\\p{N}]", "");
    }
}