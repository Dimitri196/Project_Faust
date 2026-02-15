package com.projectfaust.mapper;

import com.projectfaust.dto.response.AppointmentResponse;
import com.projectfaust.entity.Appointment;
import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.Person;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T19:01:07+0100",
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
        String occupationPublicId = null;
        boolean isActing = false;
        String appointmentNote = null;
        String personPhotoUrl = null;
        LocalDate startDate = null;
        LocalDate endDate = null;
        BigDecimal monthlySalary = null;
        BigDecimal monthlyLumpSumAllowance = null;
        String currency = null;
        Appointment.BenefitDetails benefitDetails = null;

        publicId = entity.getExternalId();
        personPublicId = entityPersonExternalId( entity );
        personDisplayName = entityPersonFullName( entity );
        occupationTitle = entityOccupationTitle( entity );
        UUID externalId1 = entityOccupationExternalId( entity );
        if ( externalId1 != null ) {
            occupationPublicId = externalId1.toString();
        }
        isActing = entity.isActing();
        appointmentNote = entity.getAppointmentNote();
        personPhotoUrl = entityPersonPhotoUrl( entity );
        startDate = entity.getStartDate();
        endDate = entity.getEndDate();
        monthlySalary = entity.getMonthlySalary();
        monthlyLumpSumAllowance = entity.getMonthlyLumpSumAllowance();
        currency = entity.getCurrency();
        benefitDetails = entity.getBenefitDetails();

        AppointmentResponse appointmentResponse = new AppointmentResponse( publicId, personDisplayName, personPublicId, occupationTitle, occupationPublicId, startDate, endDate, monthlySalary, monthlyLumpSumAllowance, currency, isActing, benefitDetails, appointmentNote, personPhotoUrl );

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

    private UUID entityOccupationExternalId(Appointment appointment) {
        Occupation occupation = appointment.getOccupation();
        if ( occupation == null ) {
            return null;
        }
        return occupation.getExternalId();
    }

    private String entityPersonPhotoUrl(Appointment appointment) {
        Person person = appointment.getPerson();
        if ( person == null ) {
            return null;
        }
        return person.getPhotoUrl();
    }
}
