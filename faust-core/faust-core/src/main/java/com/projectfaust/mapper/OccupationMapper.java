package com.projectfaust.mapper;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationAscendedResponse;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.entity.Occupation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for the Occupation domain.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OccupationMapper {

    /**
     * Initializes a new Occupation entity from a request.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "institution", ignore = true)
    @Mapping(target = "reportsTo", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    Occupation toEntity(OccupationRequest request);

    /**
     * Maps an Occupation to a standard flat response.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "institution.name", target = "institutionName")
    @Mapping(source = "institution.externalId", target = "institutionPublicId")
    @Mapping(source = "reportsTo.title", target = "supervisorTitle")
    @Mapping(source = "reportsTo.externalId", target = "reportsToPublicId")
    OccupationResponse toResponse(Occupation entity);

    List<OccupationResponse> toResponseList(List<Occupation> entities);

    /**
     * Maps to a recursive tree structure (Downward hierarchy).
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "subordinates", target = "subordinates")
    @Mapping(target = "currentOccupantName", source = "entity")
    @Mapping(target = "personPublicId", source = "entity")
    OccupationTreeResponse toTreeResponse(Occupation entity);

    List<OccupationTreeResponse> toTreeResponseList(List<Occupation> entities);

    /**
     * Maps to a lightweight navigation object (Upward/Ascended hierarchy).
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(target = "currentOccupantName", source = "entity")
    OccupationAscendedResponse toAscendedResponse(Occupation entity);

    // TATO METODA CHYBĚLA - proto service svítila červeně
    List<OccupationAscendedResponse> toAscendedResponseList(List<Occupation> entities);

    /**
     * Resolves the identity of the individual currently holding this position.
     */
    default String mapCurrentOccupant(Occupation entity) {
        if (entity.getAppointments() == null || entity.getAppointments().isEmpty()) {
            return entity.isVacant() ? "VACANT" : "No active appointment";
        }

        return entity.findCurrentAppointment()
                .map(app -> {
                    String fullName = app.getPerson().getFullName();
                    return app.isActing() ? "(Acting) " + fullName : fullName;
                })
                .orElse(entity.isVacant() ? "VACANT" : "In transition");
    }

    /**
     * Resolves the UUID of the person currently holding the office.
     * Crucial for frontend navigation to person dossier.
     */
    default java.util.UUID mapPersonPublicId(Occupation entity) {
        if (entity.getAppointments() == null) return null;

        return entity.findCurrentAppointment()
                .map(app -> app.getPerson().getExternalId())
                .orElse(null);
    }
}