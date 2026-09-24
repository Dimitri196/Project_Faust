package com.projectfaust.ingest.theses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTOs for the Semantic Scholar Academic Graph API v1 responses.
 *
 * <p>Two response shapes are covered:</p>
 * <ul>
 *   <li>{@link AuthorSearchResponse} — from {@code GET /graph/v1/author/search?query=}</li>
 *   <li>{@link AuthorPapersResponse} — from {@code GET /graph/v1/author/{id}/papers}</li>
 * </ul>
 *
 * <p>All records are {@code @JsonIgnoreProperties(ignoreUnknown = true)} —
 * Semantic Scholar returns many additional fields we don't request.
 * Only fields explicitly listed in the {@code ?fields=} query parameter
 * will be populated; others will be null.</p>
 *
 * @author Dimitri / Project Faust
 */
public class SemanticScholarAuthorDto {

    /**
     * Top-level wrapper returned by the author search endpoint.
     * {@code data} contains the matched author candidates.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorSearchResponse(
            @JsonProperty("data") List<AuthorSummary> data
    ) {}

    /**
     * A single author match from the search results.
     *
     * @param authorId  Semantic Scholar's internal author ID — used for subsequent paper lookups.
     * @param name      display name as indexed by Semantic Scholar.
     * @param paperCount total number of papers attributed to this author.
     * @param hIndex    h-index — useful as a secondary signal for identity disambiguation.
     * @param affiliations list of institutions the author is associated with.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorSummary(
            @JsonProperty("authorId")    String authorId,
            @JsonProperty("name")        String name,
            @JsonProperty("paperCount")  Integer paperCount,
            @JsonProperty("hIndex")      Integer hIndex,
            @JsonProperty("affiliations") List<String> affiliations
    ) {}

    /**
     * Top-level wrapper returned by the author papers endpoint.
     * {@code data} contains the paginated list of papers.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthorPapersResponse(
            @JsonProperty("data")   List<PaperSummary> data,
            @JsonProperty("next")   Integer next,   // pagination offset — null if last page
            @JsonProperty("offset") Integer offset
    ) {}

    /**
     * A single paper attributed to an author.
     *
     * @param paperId       Semantic Scholar paper ID.
     * @param title         paper/thesis title.
     * @param year          publication year.
     * @param publicationTypes list of types — e.g. "JournalArticle", "Thesis", "Book".
     * @param venue         publication venue name (journal, conference, or university for theses).
     * @param citationCount number of citations — useful as a relevance/importance signal.
     * @param abstract_     abstract text (mapped from JSON key "abstract").
     * @param externalIds   external identifiers (DOI, ArXiv, etc.).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaperSummary(
            @JsonProperty("paperId")          String paperId,
            @JsonProperty("title")            String title,
            @JsonProperty("year")             Integer year,
            @JsonProperty("publicationTypes") List<String> publicationTypes,
            @JsonProperty("venue")            String venue,
            @JsonProperty("citationCount")    Integer citationCount,
            @JsonProperty("abstract")         String abstract_,
            @JsonProperty("externalIds")      ExternalIds externalIds
    ) {}

    /**
     * External identifiers for a paper — used for deduplication and linking.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalIds(
            @JsonProperty("DOI")    String doi,
            @JsonProperty("ArXiv")  String arxiv,
            @JsonProperty("MAG")    String mag
    ) {}
}
