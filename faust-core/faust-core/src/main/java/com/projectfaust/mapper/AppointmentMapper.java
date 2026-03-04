package com.projectfaust.mapper;

import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for transforming Appointment entities into flattened Data Transfer Objects.
 * Handles the resolution of nested Subject (Person) and Position (Occupation) metadata
 * for simplified consumption by the frontend and intelligence reporting modules.
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AppointmentMapper {

    /**
     * Converts a domain Appointment entity into a flattened response DTO.
     * Maps biological identity data and institutional role titles into a single-level object.
     *
     * @param entity The persistence-layer Appointment entity.
     * @return A flattened AppointmentResponse suitable for public API exposure.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "person.externalId", target = "personPublicId")
    @Mapping(source = "person.fullName", target = "personDisplayName")
    @Mapping(source = "occupation.title", target = "occupationTitle")
    @Mapping(source = "occupation.externalId", target = "occupationPublicId")
    @Mapping(source = "acting", target = "isActing")
    @Mapping(source = "appointmentNote", target = "appointmentNote")
    @Mapping(source = "person.photoUrl", target = "personPhotoUrl")
    @Mapping(source = "exOffoAccess", target = "isExOffoAccess")
    AppointmentResponse toResponse(Appointment entity);

    /**
     * Transforms a collection of Appointment entities into a list of response DTOs.
     * Useful for historical career dossiers and institutional occupancy logs.
     *
     * @param entities A list of domain entities.
     * @return A list of flattened response objects.
     */
    List<AppointmentResponse> toResponseList(List<Appointment> entities);
}
