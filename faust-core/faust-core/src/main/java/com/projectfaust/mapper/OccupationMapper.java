package com.projectfaust.mapper;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.entity.Occupation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OccupationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "institution", ignore = true)
    @Mapping(target = "reportsTo", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    Occupation toEntity(OccupationRequest request);

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "institution.name", target = "institutionName")
    @Mapping(source = "institution.externalId", target = "institutionPublicId")
    @Mapping(source = "reportsTo.title", target = "supervisorTitle")
    @Mapping(source = "reportsTo.externalId", target = "reportsToPublicId")
    OccupationResponse toResponse(Occupation entity);

    List<OccupationResponse> toResponseList(List<Occupation> entities);

    /**
     * Single Tree Mapping: Handles recursion AND current occupant name
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "subordinates", target = "subordinates")
    @Mapping(target = "currentOccupantName", expression = "java(mapCurrentOccupant(entity))")
    OccupationTreeResponse toTreeResponse(Occupation entity);

    List<OccupationTreeResponse> toTreeResponseList(List<Occupation> entities);

    /**
     * Logic to extract the name of the person currently in the role.
     */
    default String mapCurrentOccupant(Occupation entity) {
        if (entity.getAppointments() == null || entity.getAppointments().isEmpty()) {
            return entity.isVacant() ? "VACANT" : "No active appointment";
        }

        return entity.findCurrentAppointment()
                .map(app -> {
                    String fullName = app.getPerson().getFullName();
                    // If the person is only acting, we prepend the Czech administrative label
                    return app.isActing() ? "(Pověřen/a řízením) " + fullName : fullName;
                })
                .orElse(entity.isVacant() ? "VACANT" : "In transition");
    }
}
