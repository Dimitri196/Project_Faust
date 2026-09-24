package com.projectfaust.ingest.theses;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.core.ParameterizedTypeReference;

import java.util.Collections;
import java.util.List;

/**
 * Fetches academic publications linked to a FAUST Person from the
 * Semantic Scholar Academic Graph API v1.
 *
 * <p><b>Why Semantic Scholar:</b> theses.cz has no REST API (OAI-PMH bulk only,
 * no author name search). Semantic Scholar provides a free, authenticated REST
 * API covering 200M+ papers across all disciplines and all Czech universities,
 * searchable by author name — the only viable option for targeted per-person
 * lookup without bulk local indexing.</p>
 *
 * <p><b>Two-step fetch strategy:</b></p>
 * <ol>
 *   <li>Search author by {@code firstName + " " + lastName} —
 *       returns a list of candidate authors.</li>
 *   <li>For the best-matching candidate (first result), fetch their full
 *       paper list including theses, journal articles, and conference papers.</li>
 * </ol>
 *
 * <p><b>Disambiguation caveat:</b> author name search is inherently ambiguous
 * for common names. The service takes the first result from Semantic Scholar's
 * ranked search — for uncommon Czech names this is generally reliable. A future
 * improvement would accept an optional {@code semanticScholarAuthorId} stored
 * on the Person entity to bypass the search step entirely for verified matches.</p>
 *
 * <p><b>Rate limits:</b> unauthenticated: shared 1,000 RPS pool.
 * Authenticated (API key): dedicated 1 RPS. Always use the API key
 * in production via {@code faust.semantic-scholar.api-key} in
 * {@code application.properties}. Leave blank for development/testing.</p>
 *
 * <p><b>Attribution:</b> when displaying results publicly, Semantic Scholar
 * requires attribution: "Data source: Semantic Scholar API".</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisFetchService {

    private static final String BASE_URL = "https://api.semanticscholar.org/graph/v1";

    // Fields requested from the author search — minimal for fast response.
    private static final String AUTHOR_FIELDS = "authorId,name,paperCount,hIndex,affiliations";

    // Fields requested for each paper — covers everything AcademicPublicationResponse needs.
    private static final String PAPER_FIELDS =
            "paperId,title,year,publicationTypes,venue,citationCount,abstract,externalIds";

    private final RestTemplate restTemplate;

    /**
     * Optional Semantic Scholar API key — provides a dedicated 1 RPS rate limit
     * rather than the shared public pool. Configure in application.properties:
     * {@code faust.semantic-scholar.api-key=your_key_here}
     * Leave empty for development/anonymous access.
     */
    @Value("${faust.semantic-scholar.api-key:}")
    private String apiKey;

    /**
     * Searches Semantic Scholar for an author matching the given name and
     * returns all their academic publications.
     *
     * @param firstName person's first name.
     * @param lastName  person's last name.
     * @return list of matched publications, empty if no author found or on error.
     */
    public List<SemanticScholarAuthorDto.PaperSummary> fetchPublicationsForPerson(
            String firstName, String lastName) {

        String fullName = firstName.trim() + " " + lastName.trim();
        log.info("THESIS_FETCH: Searching Semantic Scholar for author '{}'.", fullName);

        // Step 1 — search author by name
        SemanticScholarAuthorDto.AuthorSummary author = searchAuthor(fullName);
        if (author == null) {
            log.warn("THESIS_FETCH: No author found on Semantic Scholar for '{}'.", fullName);
            return Collections.emptyList();
        }

        log.info("THESIS_FETCH: Matched author '{}' (id={}, papers={}, hIndex={}) " +
                        "for query '{}'.",
                author.name(), author.authorId(), author.paperCount(), author.hIndex(), fullName);

        // Step 2 — fetch their papers
        return fetchAuthorPapers(author.authorId(), fullName);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private SemanticScholarAuthorDto.AuthorSummary searchAuthor(String fullName) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(BASE_URL + "/author/search")
                    .queryParam("query", fullName)
                    .queryParam("fields", AUTHOR_FIELDS)
                    .queryParam("limit", 3)   // take top 3 candidates max
                    .toUriString();

            ResponseEntity<SemanticScholarAuthorDto.AuthorSearchResponse> response =
                    restTemplate.exchange(
                            url, HttpMethod.GET,
                            buildEntity(),
                            SemanticScholarAuthorDto.AuthorSearchResponse.class);

            if (response.getBody() == null
                    || response.getBody().data() == null
                    || response.getBody().data().isEmpty()) {
                return null;
            }

            // Return the first (highest-ranked) match.
            // For uncommon Czech names this is almost always correct.
            return response.getBody().data().get(0);

        } catch (Exception e) {
            log.error("THESIS_FETCH: Author search failed for '{}' — {}", fullName, e.getMessage());
            return null;
        }
    }

    private List<SemanticScholarAuthorDto.PaperSummary> fetchAuthorPapers(
            String authorId, String fullName) {
        try {
            String url = UriComponentsBuilder
                    .fromUriString(BASE_URL + "/author/" + authorId + "/papers")
                    .queryParam("fields", PAPER_FIELDS)
                    .queryParam("limit", 100)
                    .toUriString();

            ResponseEntity<SemanticScholarAuthorDto.AuthorPapersResponse> response =
                    restTemplate.exchange(
                            url, HttpMethod.GET,
                            buildEntity(),
                            SemanticScholarAuthorDto.AuthorPapersResponse.class);

            if (response.getBody() == null || response.getBody().data() == null) {
                log.warn("THESIS_FETCH: No papers returned for author id={} ({}).",
                        authorId, fullName);
                return Collections.emptyList();
            }

            List<SemanticScholarAuthorDto.PaperSummary> papers = response.getBody().data();
            log.info("THESIS_FETCH: Retrieved {} publications for author id={} ({}).",
                    papers.size(), authorId, fullName);
            return papers;

        } catch (Exception e) {
            log.error("THESIS_FETCH: Paper fetch failed for author id={} ({}) — {}",
                    authorId, fullName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Builds an HTTP entity with the API key header if configured,
     * or no auth header for anonymous access.
     */
    private HttpEntity<Void> buildEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (apiKey != null && !apiKey.isBlank()) {
            headers.set("x-api-key", apiKey);
        }
        return new HttpEntity<>(headers);
    }
}