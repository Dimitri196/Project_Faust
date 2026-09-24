package com.projectfaust.person.contact;

import com.projectfaust.person.contact.dto.ContactOperationsRequest;   // or PersonContactRequest
import com.projectfaust.person.contact.dto.ContactOperationsResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PersonContactMapper {

    @Mapping(source = "entity.externalId", target = "publicId")
    @Mapping(source = "entity.person.externalId", target = "personPublicId")
    @Mapping(target = "subjectDisplayName", expression = "java(entity.getPerson().getFullName())")
    ContactOperationsResponse toResponse(PersonContact entity);

    List<ContactOperationsResponse> toResponseList(List<PersonContact> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "contactValueNormalized", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    PersonContact toEntity(ContactOperationsRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "person", ignore = true)
    @Mapping(target = "contactValueNormalized", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(ContactOperationsRequest request, @MappingTarget PersonContact entity);
}
