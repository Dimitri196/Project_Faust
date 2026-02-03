package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.Person;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PersonMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    Person toEntity(PersonRequest request);

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(target = "displayName", expression = "java(entity.getFullName())")
    PersonResponse toResponse(Person entity);

    List<PersonResponse> toResponseList(List<Person> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    void updateEntityFromRequest(PersonRequest request, @MappingTarget Person entity);
}
