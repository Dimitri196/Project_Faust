package com.projectfaust.ingest.theses;

import com.projectfaust.person.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates academic publication lookup for a FAUST Person.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li>Resolves the {@link com.projectfaust.person.Person} by public UUID.</li>
 *   <li>Delegates the Semantic Scholar API call to {@link ThesisFetchService}.</li>
 *   <li>Maps raw {@link SemanticScholarAuthorDto.PaperSummary} results to
 *       {@link AcademicPublicationResponse} for the controller to return.</li>
 * </ul>
 *
 * <p><b>Persistence:</b> handled by {@link ThesisConsumerService} via Kafka,
 * not here. This service is the synchronous query-side; the consumer is the
 * async write-side. Keeping them separate follows the same pattern as
 * {@link com.projectfaust.ingest.hlidacstatu.ExternalContractFetchService}
 * (fetch → Kafka) vs
 * {@link com.projectfaust.ingest.hlidacstatu.ContractConsumerService}
 * (consume → persist).</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisIngestService {

    private final ThesisFetchService fetchService;
    private final PersonRepository personRepository;

    /**
     * Fetches and returns all academic publications found on Semantic Scholar
     * for the person identified by {@code personPublicId}.
     *
     * @param personPublicId the public UUID of the FAUST Person.
     * @return list of matched publications, empty if none found.
     * @throws jakarta.persistence.EntityNotFoundException if person not found.
     */
    public List<AcademicPublicationResponse> fetchAndReturnPublications(UUID personPublicId) {
        var person = personRepository.findByExternalId(personPublicId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "THESIS_INGEST: Person not found: " + personPublicId));

        List<SemanticScholarAuthorDto.PaperSummary> papers =
                fetchService.fetchPublicationsForPerson(person.getFirstName(), person.getLastName());

        if (papers.isEmpty()) {
            log.info("THESIS_INGEST: No publications found for {} {}.",
                    person.getFirstName(), person.getLastName());
            return List.of();
        }

        return papers.stream()
                .map(paper -> toResponse(paper, personPublicId))
                .toList();
    }

    /**
     * Fire-and-forget variant used by the trigger endpoint.
     * Fetches publications and logs results without returning them to the caller.
     * In a future iteration this will dispatch to Kafka instead of calling
     * {@link ThesisFetchService} directly, making the trigger truly async.
     *
     * @param personPublicId the public UUID of the FAUST Person.
     * @param firstName      person's first name (passed to avoid a second DB lookup).
     * @param lastName       person's last name.
     */
    public void fetchThesesForPerson(UUID personPublicId, String firstName, String lastName) {
        log.info("THESIS_INGEST: Trigger received for {} {} (id={}).",
                firstName, lastName, personPublicId);
        fetchService.fetchPublicationsForPerson(firstName, lastName);
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private AcademicPublicationResponse toResponse(
            SemanticScholarAuthorDto.PaperSummary paper, UUID personPublicId) {
        String doi = paper.externalIds() != null ? paper.externalIds().doi() : null;
        String url = paper.paperId() != null
                ? "https://www.semanticscholar.org/paper/" + paper.paperId()
                : null;

        return new AcademicPublicationResponse(
                personPublicId,
                paper.paperId(),
                paper.title(),
                paper.year(),
                paper.venue(),
                paper.publicationTypes(),
                paper.citationCount(),
                paper.abstract_(),
                doi,
                url,
                null,
                null
        );
    }
}