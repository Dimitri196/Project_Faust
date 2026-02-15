package com.projectfaust.mapper;

import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AppointmentMapper {

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "person.externalId", target = "personPublicId")
    @Mapping(source = "person.fullName", target = "personDisplayName")
    @Mapping(source = "occupation.title", target = "occupationTitle")
    // PŘIDAT TENTO ŘÁDEK:
    @Mapping(source = "occupation.externalId", target = "occupationPublicId")
    @Mapping(source = "acting", target = "isActing")
    @Mapping(source = "appointmentNote", target = "appointmentNote")
    @Mapping(source = "person.photoUrl", target = "personPhotoUrl")
    AppointmentResponse toResponse(Appointment entity);

    List<AppointmentResponse> toResponseList(List<Appointment> entities);
}