package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonConnectionRequest;
import com.projectfaust.dto.response.PersonConnectionResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonConnection;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * Abstract mapper for managing interpersonal connections within the intelligence network.
 * Handles entity resolution from UUIDs and calculates real-time connectivity metrics
 * such as influence scores and active status.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class PersonConnectionMapper {

    @Autowired
    protected PersonRepository personRepository;

    /**
     * Transforms a connection request into a persistent entity.
     * Resolves source and target subjects and assigns default weights based on relationship types.
     */
    @Mapping(target = "sourcePerson", source = "sourcePersonId", qualifiedByName = "uuidToPerson")
    @Mapping(target = "targetPerson", source = "targetPersonId", qualifiedByName = "uuidToPerson")
    @Mapping(target = "influenceScore", expression = "java(request.influenceScore() != null ? request.influenceScore() : request.type().getDefaultWeight())")
    public abstract PersonConnection toEntity(PersonConnectionRequest request);

    /**
     * Converts a connection entity into a response DTO for network visualization.
     * Flattens the target's current professional standing and calculates if the link is still operational.
     */
    @Mapping(target = "connectionId", source = "externalId")
    @Mapping(target = "targetId", source = "targetPerson.externalId")
    @Mapping(target = "targetFullName", source = "targetPerson")
    @Mapping(target = "targetCurrentPosition", source = "targetPerson", qualifiedByName = "extractCurrentPosition")
    @Mapping(target = "isActive", expression = "java(entity.getEndDate() == null || entity.getEndDate().isAfter(java.time.LocalDate.now()))")
    @Mapping(target = "type", source = "connectionType")
    public abstract PersonConnectionResponse toResponse(PersonConnection entity);

    // --- HELPER METHODS (Dossier Resolution) ---

    /**
     * Look up a subject in the core database by their public UUID.
     */
    @Named("uuidToPerson")
    protected Person uuidToPerson(UUID uuid) {
        if (uuid == null) return null;
        return personRepository.findByExternalId(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Subject with ID " + uuid + " not found in core database."));
    }

    /**
     * Maps a Person entity to a displayable name string.
     */
    protected String mapPersonToFullName(Person person) {
        return person != null ? person.getFullName() : "REDACTED";
    }

    /**
     * Analyzes the subject's active appointments to determine their current primary role.
     */
    @Named("extractCurrentPosition")
    protected String extractCurrentPosition(Person person) {
        if (person == null || person.getAppointments() == null) return "UNKNOWN";
        return person.getAppointments().stream()
                .filter(a -> a.getEndDate() == null) // Filter for active deployments
                .map(a -> a.getOccupation().getTitle())
                .findFirst()
                .orElse("INACTIVE");
    }
}
