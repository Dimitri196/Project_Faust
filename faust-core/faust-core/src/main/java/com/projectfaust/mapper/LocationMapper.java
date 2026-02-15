package com.projectfaust.mapper;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "parentExternalId", source = "parent.externalId")
    @Mapping(target = "parentName", source = "parent.name")
    LocationResponse toResponse(Location location);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
        // ODSTRANĚNO: @Mapping(target = "institutions", ignore = true)
        // Pokud institutions v entitě nejsou, MapStruct je mapovat nebude.
    Location toEntity(LocationRequest request);
}