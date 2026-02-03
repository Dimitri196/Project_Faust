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
    @Mapping(source = "person.fullName", target = "personDisplayName") // Uses Person.getFullName()
    @Mapping(source = "occupation.title", target = "occupationTitle")
    @Mapping(source = "acting", target = "isActing") // Explicitní namapování
    AppointmentResponse toResponse(Appointment entity);

    List<AppointmentResponse> toResponseList(List<Appointment> entities);
}
