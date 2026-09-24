package com.projectfaust.ingest.theses;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned by the Thesis/Academic module endpoints.
 * Represents a normalised academic publication linked to a FAUST Person.
 *
 * @param personPublicId    the FAUST Person this publication was fetched for.
 * @param semanticScholarId Semantic Scholar's internal paper ID — used as externalId.
 * @param title             publication/thesis title.
 * @param year              publication year.
 * @param venue             journal, conference, or awarding university.
 * @param publicationTypes  e.g. "Thesis", "JournalArticle", "Book".
 * @param citationCount     number of citations.
 * @param abstract_         abstract text.
 * @param doi               DOI if available.
 * @param semanticScholarUrl direct link to the paper on semanticscholar.org.
 * @param authorName        matched author name as indexed by Semantic Scholar.
 * @param authorId          Semantic Scholar author ID.
 *
 * @author Dimitri / Project Faust
 */
public record AcademicPublicationResponse(
        UUID personPublicId,
        String semanticScholarId,
        String title,
        Integer year,
        String venue,
        List<String> publicationTypes,
        Integer citationCount,
        String abstract_,
        String doi,
        String semanticScholarUrl,
        String authorName,
        String authorId
) {}