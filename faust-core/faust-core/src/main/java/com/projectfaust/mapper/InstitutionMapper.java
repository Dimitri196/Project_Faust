package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.entity.Institution;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface InstitutionMapper {

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent.externalId", target = "parentId")
    @Mapping(source = "location.externalId", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(target = "fullLocationPath", ignore = true)
    @Mapping(source = "stateOwned", target = "isStateOwned") // PŘIDEJ TENTO ŘÁDEK
    InstitutionResponse toResponse(Institution entity);


    // List mapping will produce responses with null paths,
    // which we will enrich in the Service if needed.
    List<InstitutionResponse> toResponseList(List<Institution> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "location", ignore = true)
    Institution toEntity(InstitutionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromRequest(InstitutionRequest request, @MappingTarget Institution entity);

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "children", target = "children")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionTreeResponse toTreeResponse(Institution entity);

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent", target = "parent")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionAscendedResponse toAscendedResponse(Institution entity);

    List<InstitutionTreeResponse> toTreeResponseList(List<Institution> entities);
}
