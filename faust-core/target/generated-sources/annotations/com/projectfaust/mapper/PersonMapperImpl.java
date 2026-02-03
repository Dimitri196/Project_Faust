package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.enums.EducationLevel;
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
public class PersonMapperImpl implements PersonMapper {

    @Override
    public Person toEntity(PersonRequest request) {
        if ( request == null ) {
            return null;
        }

        Person.PersonBuilder person = Person.builder();

        person.firstName( request.firstName() );
        person.lastName( request.lastName() );
        person.titleBefore( request.titleBefore() );
        person.titleAfter( request.titleAfter() );
        person.educationLevel( request.educationLevel() );
        person.fieldOfStudy( request.fieldOfStudy() );
        person.email( request.email() );
        person.phone( request.phone() );
        person.biography( request.biography() );

        return person.build();
    }

    @Override
    public PersonResponse toResponse(Person entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        String firstName = null;
        String lastName = null;
        String titleBefore = null;
        String titleAfter = null;
        EducationLevel educationLevel = null;
        String fieldOfStudy = null;
        String email = null;
        String phone = null;
        String biography = null;

        publicId = entity.getExternalId();
        firstName = entity.getFirstName();
        lastName = entity.getLastName();
        titleBefore = entity.getTitleBefore();
        titleAfter = entity.getTitleAfter();
        educationLevel = entity.getEducationLevel();
        fieldOfStudy = entity.getFieldOfStudy();
        email = entity.getEmail();
        phone = entity.getPhone();
        biography = entity.getBiography();

        String displayName = entity.getFullName();

        PersonResponse personResponse = new PersonResponse( publicId, firstName, lastName, titleBefore, titleAfter, displayName, educationLevel, fieldOfStudy, email, phone, biography );

        return personResponse;
    }

    @Override
    public List<PersonResponse> toResponseList(List<Person> entities) {
        if ( entities == null ) {
            return null;
        }

        List<PersonResponse> list = new ArrayList<PersonResponse>( entities.size() );
        for ( Person person : entities ) {
            list.add( toResponse( person ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromRequest(PersonRequest request, Person entity) {
        if ( request == null ) {
            return;
        }

        entity.setFirstName( request.firstName() );
        entity.setLastName( request.lastName() );
        entity.setTitleBefore( request.titleBefore() );
        entity.setTitleAfter( request.titleAfter() );
        entity.setEducationLevel( request.educationLevel() );
        entity.setFieldOfStudy( request.fieldOfStudy() );
        entity.setEmail( request.email() );
        entity.setPhone( request.phone() );
        entity.setBiography( request.biography() );
    }
}
