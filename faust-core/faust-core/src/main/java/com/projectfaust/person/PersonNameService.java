package com.projectfaust.person;

import com.projectfaust.person.dto.AdditionalNameRequest;
import com.projectfaust.person.dto.PersonNameResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Dedicated service for managing individual name records on a person —
 * additional names (aliases, cover names, maiden names) — without
 * resynchronising the entire person profile.
 *
 * <p><b>Why this exists:</b> {@link PersonService#update} performs a full
 * resynchronisation of names, contacts, and financial accounts on every
 * call ({@code .clear()} then rebuild from the request body). Adding a
 * single alias therefore required resending the person's complete profile
 * — every contact, every financial account, every biographical field —
 * or risk silently wiping data omitted from the request. This service
 * provides a true partial-update path: appending one name touches only
 * the {@code person_names} table, nothing else on the person.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonNameService {

    private final PersonRepository personRepository;
    private final PersonNameRepository nameRepository;

    /**
     * Returns all name records for a person, primary first then by creation order.
     *
     * @param personPublicId the public UUID of the person.
     * @return list of name records.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public List<PersonNameResponse> getAll(UUID personPublicId) {
        if (!personRepository.existsByExternalId(personPublicId)) {
            throw new EntityNotFoundException("PERSON_NOT_FOUND: " + personPublicId);
        }
        return nameRepository.findAllByPersonExternalIdOrderByPrimaryDescCreatedAtAsc(personPublicId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Appends a single additional name (alias, cover name, maiden name, etc.)
     * to a person without touching contacts, financial accounts, or any
     * biographical field.
     *
     * <p>Never affects the primary name — {@code primary} is always set to
     * {@code false} here. Changing the primary legal name is a distinct,
     * more sensitive operation and should go through a dedicated endpoint
     * with its own validation once needed.</p>
     *
     * @param personPublicId the public UUID of the person.
     * @param request        the additional name to append.
     * @return the persisted name record.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional
    public PersonNameResponse addName(UUID personPublicId, AdditionalNameRequest request) {
        Person person = personRepository.findByExternalId(personPublicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NOT_FOUND: " + personPublicId));

        PersonName name = PersonName.builder()
                .person(person)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .type(request.type())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .note(request.note())
                .primary(false)
                .build();

        PersonName saved = nameRepository.save(name);
        log.info("FAUST_PERSON: Added name '{} {}' ({}) to person {}.",
                saved.getFirstName(), saved.getLastName(), saved.getType(), personPublicId);

        return toResponse(saved);
    }

    /**
     * Deactivates (soft-deletes) a name record by setting {@code validTo}
     * to today if not already set. The record is retained for audit purposes
     * via Hibernate Envers — never physically deleted.
     *
     * @param nameExternalId the public UUID of the name record to deactivate.
     * @throws EntityNotFoundException if no name record matches the given UUID.
     * @throws IllegalArgumentException if attempting to deactivate the primary name.
     */
    @Transactional
    public void deactivate(UUID nameExternalId) {
        PersonName name = nameRepository.findByExternalId(nameExternalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NAME_NOT_FOUND: " + nameExternalId));

        if (name.isPrimary()) {
            throw new IllegalArgumentException(
                    "Cannot deactivate the primary name — use a legal name change operation instead.");
        }

        if (name.getValidTo() == null) {
            name.setValidTo(java.time.LocalDate.now());
            nameRepository.save(name);
            log.info("FAUST_PERSON: Deactivated name record {}.", nameExternalId);
        }
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private PersonNameResponse toResponse(PersonName name) {
        return new PersonNameResponse(
                name.getExternalId(),
                name.getFirstName(),
                name.getLastName(),
                name.getType(),
                name.isPrimary(),
                name.getValidFrom(),
                name.getValidTo(),
                name.getNote()
        );
    }
}