package com.projectfaust.mapper;

import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.entity.Appointment;
import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.Person;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-02T20:38:30+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class AppointmentMapperImpl implements AppointmentMapper {

    @Override
    public AppointmentResponse toResponse(Appointment entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        UUID personPublicId = null;
        String personDisplayName = null;
        String occupationTitle = null;
        boolean isActing = false;
        LocalDate startDate = null;
        LocalDate endDate = null;

        publicId = entity.getExternalId();
        personPublicId = entityPersonExternalId( entity );
        personDisplayName = entityPersonFullName( entity );
        occupationTitle = entityOccupationTitle( entity );
        isActing = entity.isActing();
        startDate = entity.getStartDate();
        endDate = entity.getEndDate();

        AppointmentResponse appointmentResponse = new AppointmentResponse( publicId, personDisplayName, personPublicId, occupationTitle, startDate, endDate, isActing );

        return appointmentResponse;
    }

    @Override
    public List<AppointmentResponse> toResponseList(List<Appointment> entities) {
        if ( entities == null ) {
            return null;
        }

        List<AppointmentResponse> list = new ArrayList<AppointmentResponse>( entities.size() );
        for ( Appointment appointment : entities ) {
            list.add( toResponse( appointment ) );
        }

        return list;
    }

    private UUID entityPersonExternalId(Appointment appointment) {
        Person person = appointment.getPerson();
        if ( person == null ) {
            return null;
        }
        return person.getExternalId();
    }

    private String entityPersonFullName(Appointment appointment) {
        Person person = appointment.getPerson();
        if ( person == null ) {
            return null;
        }
        return person.getFullName();
    }

    private String entityOccupationTitle(Appointment appointment) {
        Occupation occupation = appointment.getOccupation();
        if ( occupation == null ) {
            return null;
        }
        return occupation.getTitle();
    }
}
