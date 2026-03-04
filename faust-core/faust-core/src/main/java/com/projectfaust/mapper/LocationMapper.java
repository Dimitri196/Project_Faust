package com.projectfaust.mapper;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for the Location domain.
 * Facilitates the transformation between geographic persistence entities and
 * their corresponding DTO projections, maintaining hierarchical integrity.
 */
@Mapper(componentModel = "spring")
public interface LocationMapper {

    /**
     * Maps a Location entity to a response DTO.
     * Resolves the parent relationship into flattened identifiers for client consumption.
     *
     * @param location The geographic persistence entity.
     * @return A flattened LocationResponse containing primary and parent metadata.
     */
    @Mapping(target = "parentExternalId", source = "parent.externalId")
    @Mapping(target = "parentName", source = "parent.name")
    LocationResponse toResponse(Location location);

    /**
     * Initializes a new Location entity from a request DTO.
     * Technical IDs and relational collections are ignored to be handled by
     * the service layer and persistence provider.
     *
     * @param request The data transfer object containing location attributes.
     * @return A Location entity prepared for persistence.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "clearanceLevel", source = "clearance")
    Location toEntity(LocationRequest request);
}
