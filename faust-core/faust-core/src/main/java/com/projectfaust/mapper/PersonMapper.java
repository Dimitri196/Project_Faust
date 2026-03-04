package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.Person;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for the Person domain.
 * Responsible for transforming biographical data between persistence entities
 * and DTO projections, supporting identity normalization and subject dossiers.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.UUID.class }
)
public interface PersonMapper {

    /**
     * Initializes a new Person entity from a request.
     * Strategic fields like externalId and normalized search strings are ignored
     * to be handled by the service layer's business logic.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    Person toEntity(PersonRequest request);

    /**
     * Transforms a Person entity into a public Response DTO.
     * Calculates derived fields like age and display name on-the-fly.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(target = "displayName", expression = "java(entity.getFullName())")
    @Mapping(target = "age", expression = "java(entity.getAge())")
    PersonResponse toResponse(Person entity);

    /**
     * Converts a collection of person entities into a list of responses.
     */
    List<PersonResponse> toResponseList(List<Person> entities);

    /**
     * Updates an existing subject's record with new request data.
     * Ensures that core identifiers remain immutable during the update process.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    void updateEntityFromRequest(PersonRequest request, @MappingTarget Person entity);
}
