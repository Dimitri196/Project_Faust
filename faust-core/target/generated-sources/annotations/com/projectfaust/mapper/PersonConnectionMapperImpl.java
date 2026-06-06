package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonConnectionRequest;
import com.projectfaust.dto.response.PersonConnectionResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonConnection;
import com.projectfaust.entity.enums.ConnectionType;
import java.time.LocalDate;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-07T11:11:06+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class PersonConnectionMapperImpl extends PersonConnectionMapper {

    @Override
    public PersonConnection toEntity(PersonConnectionRequest request) {
        if ( request == null ) {
            return null;
        }

        PersonConnection.PersonConnectionBuilder personConnection = PersonConnection.builder();

        personConnection.sourcePerson( uuidToPerson( request.sourcePersonId() ) );
        personConnection.targetPerson( uuidToPerson( request.targetPersonId() ) );
        personConnection.description( request.description() );
        personConnection.startDate( request.startDate() );
        personConnection.endDate( request.endDate() );

        personConnection.influenceScore( request.influenceScore() != null ? request.influenceScore() : request.type().getDefaultWeight() );

        return personConnection.build();
    }

    @Override
    public PersonConnectionResponse toResponse(PersonConnection entity) {
        if ( entity == null ) {
            return null;
        }

        UUID connectionId = null;
        UUID targetId = null;
        String targetFullName = null;
        String targetCurrentPosition = null;
        ConnectionType type = null;
        Double influenceScore = null;
        String description = null;
        LocalDate startDate = null;

        connectionId = entity.getExternalId();
        targetId = entityTargetPersonExternalId( entity );
        targetFullName = mapPersonToFullName( entity.getTargetPerson() );
        targetCurrentPosition = extractCurrentPosition( entity.getTargetPerson() );
        type = entity.getConnectionType();
        influenceScore = entity.getInfluenceScore();
        description = entity.getDescription();
        startDate = entity.getStartDate();

        boolean isActive = entity.getEndDate() == null || entity.getEndDate().isAfter(java.time.LocalDate.now());

        PersonConnectionResponse personConnectionResponse = new PersonConnectionResponse( connectionId, targetId, targetFullName, type, influenceScore, description, targetCurrentPosition, startDate, isActive );

        return personConnectionResponse;
    }

    private UUID entityTargetPersonExternalId(PersonConnection personConnection) {
        Person targetPerson = personConnection.getTargetPerson();
        if ( targetPerson == null ) {
            return null;
        }
        return targetPerson.getExternalId();
    }
}
