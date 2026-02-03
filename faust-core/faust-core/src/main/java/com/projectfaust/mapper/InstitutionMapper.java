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

    /**
     * Converts a Request Record into an Entity.
     * We ignore ID and ExternalId because they are handled by the DB/Entity initialization.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true) // Handled manually in the Service layer
    @Mapping(target = "children", ignore = true)
    @Mapping(source = "isStateOwned", target = "isStateOwned")
    Institution toEntity(InstitutionRequest request);

    /**
     * Converts an Entity into a Response Record.
     * Maps the parent's name and the UUID for the frontend.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent.externalId", target = "parentId") // Mapování UUID rodiče do DTO
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionResponse toResponse(Institution entity);

    List<InstitutionResponse> toResponseList(List<Institution> entities);

    /**
     * Updates an existing Entity from a Request Record.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromRequest(InstitutionRequest request, @MappingTarget Institution entity);


    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "children", target = "children") // MapStruct detects recursion here
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionTreeResponse toTreeResponse(Institution entity);

    // BOTTOM-UP: MapStruct vidí "parent" v entitě i v DTO a rekurzivně zavolá toAscendedResponse
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent", target = "parent")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionAscendedResponse toAscendedResponse(Institution entity);

    List<InstitutionTreeResponse> toTreeResponseList(List<Institution> entities);
}
