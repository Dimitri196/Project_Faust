package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.entity.Institution;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for the Institution domain.
 * Orchestrates the transformation between persistence entities and multiple
 * hierarchical DTO projections (Flat, Tree, and Ascended).
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface InstitutionMapper {

    /**
     * Maps an entity to a standard flat response.
     * Includes logic to detect child nodes and extract spatial metadata.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent.externalId", target = "parentId")
    @Mapping(source = "location.externalId", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(target = "fullLocationPath", ignore = true) // Handled by service layer if required
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionResponse toResponse(Institution entity);

    /**
     * Converts a collection of entities into standard responses.
     */
    List<InstitutionResponse> toResponseList(List<Institution> entities);

    /**
     * Initializes a new Institution entity from a request DTO.
     * Technical IDs and relational links are ignored to be handled by the service layer.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "location", ignore = true)
    Institution toEntity(InstitutionRequest request);

    /**
     * Updates an existing managed entity with data from a request.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    void updateEntityFromRequest(InstitutionRequest request, @MappingTarget Institution entity);

    /**
     * Maps an entity to a recursive tree structure.
     * Used for downward hierarchy visualization (Parent -> Children).
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "children", target = "children")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    @Mapping(source = "active", target = "active")
    InstitutionTreeResponse toTreeResponse(Institution entity);

    /**
     * Maps an entity to an ascended lineage structure.
     * Used for upward hierarchy visualization (Child -> Parent).
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent", target = "parent")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    @Mapping(source = "active", target = "active")
    InstitutionAscendedResponse toAscendedResponse(Institution entity);

    /**
     * Transforms a list of entities into a list of tree-ready projections.
     */
    List<InstitutionTreeResponse> toTreeResponseList(List<Institution> entities);
}
