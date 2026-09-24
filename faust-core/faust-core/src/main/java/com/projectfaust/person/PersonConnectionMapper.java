package com.projectfaust.person;

import com.projectfaust.person.dto.PersonConnectionRequest;
import com.projectfaust.person.dto.PersonConnectionResponse;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * MapStruct mapper for {@link PersonConnection} — the edges of the HUMINT graph.
 *
 * <p>Uses an abstract class (rather than an interface) to allow Spring injection
 * of {@link PersonRepository}, which is needed to resolve person UUIDs to entities
 * during {@link #toEntity} mapping. This is an accepted MapStruct pattern for
 * mappers that require external lookups.</p>
 *
 * <p><b>DB lookup in toEntity:</b> the {@link #uuidToPerson} helper performs a
 * database lookup inside the mapper. This couples the mapper to the persistence
 * layer, which is a tradeoff — it simplifies the service layer at the cost of
 * mapper purity. Acceptable here because the mapper is Spring-managed and always
 * called within a {@code @Transactional} boundary.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PersonConnectionMapper {

    @Autowired
    protected PersonRepository personRepository;

    // -------------------------------------------------------------------------
    // Entity creation mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link PersonConnectionRequest} to a {@link PersonConnection} entity.
     *
     * <p>Resolves source and target person UUIDs to entities via
     * {@link #uuidToPerson}. Assigns the influence score from the request,
     * falling back to the connection type's default weight if not provided.</p>
     *
     * @param request the connection creation request.
     * @return a connection entity ready for persistence.
     */
    @Mapping(target = "sourcePerson",   source = "sourcePersonId",  qualifiedByName = "uuidToPerson")
    @Mapping(target = "targetPerson",   source = "targetPersonId",  qualifiedByName = "uuidToPerson")
    @Mapping(target = "influenceScore",
            expression = "java(request.influenceScore() != null ? request.influenceScore() : request.connectionType().getDefaultWeight())")
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    public abstract PersonConnection toEntity(PersonConnectionRequest request);

    // -------------------------------------------------------------------------
    // Response mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link PersonConnection} entity to a {@link PersonConnectionResponse}.
     *
     * <p>Flattens the target person's identity and current professional role into
     * scalar fields for graph visualisation. The {@code active} flag is derived
     * from the {@code endDate} — a connection with no end date or a future end
     * date is considered active.</p>
     *
     * @param entity the connection entity.
     * @return a response DTO for network visualisation.
     */
    @Mapping(target = "connectionId",           source = "externalId")
    @Mapping(target = "targetId",               source = "targetPerson.externalId")
    @Mapping(target = "targetFullName",         source = "targetPerson")
    @Mapping(target = "targetCurrentPosition",  source = "targetPerson",
            qualifiedByName = "extractCurrentPosition")
    @Mapping(target = "active",
            expression = "java(entity.getEndDate() == null || entity.getEndDate().isAfter(java.time.LocalDate.now()))")
    @Mapping(target = "connectionType",                   source = "connectionType")
    public abstract PersonConnectionResponse toResponse(PersonConnection entity);

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Resolves a person public UUID to a {@link Person} entity.
     *
     * <p>Performs a database lookup — must be called within a {@code @Transactional}
     * boundary to avoid lazy-load exceptions on the returned entity.</p>
     *
     * @param uuid the public UUID of the person.
     * @return the matching {@link Person} entity.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Named("uuidToPerson")
    protected Person uuidToPerson(UUID uuid) {
        if (uuid == null) return null;
        return personRepository.findByExternalId(uuid)
                .orElseThrow(() -> new EntityNotFoundException(
                        "PERSON_NOT_FOUND: " + uuid));
    }

    /**
     * Maps a {@link Person} entity to their full display name.
     *
     * @param person the person entity.
     * @return the formatted full name, or {@code "REDACTED"} if null.
     */
    protected String mapPersonToFullName(Person person) {
        return person != null ? person.getFullName() : "REDACTED";
    }

    /**
     * Resolves the current primary role title for a person by inspecting
     * their active appointments.
     *
     * <p>An appointment is considered active when its {@code endDate} is null.
     * Returns {@code "INACTIVE"} if no active appointment exists.</p>
     *
     * @param person the person entity.
     * @return the current occupation title, or a status string.
     */
    @Named("extractCurrentPosition")
    protected String extractCurrentPosition(Person person) {
        if (person == null || person.getAppointments() == null) return "UNKNOWN";
        return person.getAppointments().stream()
                .filter(a -> a.getEndDate() == null)
                .map(a -> a.getOccupation().getTitle())
                .findFirst()
                .orElse("INACTIVE");
    }
}