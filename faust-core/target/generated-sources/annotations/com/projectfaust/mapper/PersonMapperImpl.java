package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonName;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.EducationLevel;
import com.projectfaust.entity.enums.Gender;
import com.projectfaust.entity.enums.NameType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-07T11:11:06+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class PersonMapperImpl implements PersonMapper {

    @Override
    public PersonResponse toResponse(Person entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        List<PersonResponse.PersonNameDto> nameHistory = null;
        String titleBefore = null;
        String titleAfter = null;
        EducationLevel educationLevel = null;
        String fieldOfStudy = null;
        String email = null;
        String phone = null;
        String biography = null;
        String photoUrl = null;
        String politicalAffiliation = null;
        LocalDate birthDate = null;
        Gender gender = null;
        String nationality = null;
        String placeOfBirth = null;
        LocalDate deathDate = null;
        ClearanceLevel clearanceLevel = null;

        publicId = entity.getExternalId();
        nameHistory = toNameDtoList( entity.getNames() );
        titleBefore = entity.getTitleBefore();
        titleAfter = entity.getTitleAfter();
        educationLevel = entity.getEducationLevel();
        fieldOfStudy = entity.getFieldOfStudy();
        email = entity.getEmail();
        phone = entity.getPhone();
        biography = entity.getBiography();
        photoUrl = entity.getPhotoUrl();
        politicalAffiliation = entity.getPoliticalAffiliation();
        birthDate = entity.getBirthDate();
        gender = entity.getGender();
        nationality = entity.getNationality();
        placeOfBirth = entity.getPlaceOfBirth();
        deathDate = entity.getDeathDate();
        clearanceLevel = entity.getClearanceLevel();

        String displayName = entity.getFullName();
        Integer age = entity.getAge();
        String firstName = getPrimaryFirstName(entity);
        String lastName = getPrimaryLastName(entity);

        PersonResponse personResponse = new PersonResponse( publicId, firstName, lastName, nameHistory, titleBefore, titleAfter, displayName, age, educationLevel, fieldOfStudy, email, phone, biography, photoUrl, politicalAffiliation, birthDate, gender, nationality, placeOfBirth, deathDate, clearanceLevel );

        return personResponse;
    }

    @Override
    public PersonResponse.PersonNameDto toNameDto(PersonName name) {
        if ( name == null ) {
            return null;
        }

        String firstName = null;
        String lastName = null;
        NameType type = null;
        LocalDate validFrom = null;
        LocalDate validTo = null;
        String note = null;

        firstName = name.getFirstName();
        lastName = name.getLastName();
        type = name.getType();
        validFrom = name.getValidFrom();
        validTo = name.getValidTo();
        note = name.getNote();

        boolean isPrimary = false;

        PersonResponse.PersonNameDto personNameDto = new PersonResponse.PersonNameDto( firstName, lastName, type, isPrimary, validFrom, validTo, note );

        return personNameDto;
    }

    @Override
    public List<PersonResponse.PersonNameDto> toNameDtoList(List<PersonName> names) {
        if ( names == null ) {
            return null;
        }

        List<PersonResponse.PersonNameDto> list = new ArrayList<PersonResponse.PersonNameDto>( names.size() );
        for ( PersonName personName : names ) {
            list.add( toNameDto( personName ) );
        }

        return list;
    }

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
        person.politicalAffiliation( request.politicalAffiliation() );
        person.biography( request.biography() );
        person.photoUrl( request.photoUrl() );
        person.clearanceLevel( request.clearanceLevel() );
        person.birthDate( request.birthDate() );
        person.gender( request.gender() );
        person.nationality( request.nationality() );
        person.placeOfBirth( request.placeOfBirth() );
        person.deathDate( request.deathDate() );

        return person.build();
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
        entity.setPoliticalAffiliation( request.politicalAffiliation() );
        entity.setBiography( request.biography() );
        entity.setPhotoUrl( request.photoUrl() );
        entity.setClearanceLevel( request.clearanceLevel() );
        entity.setBirthDate( request.birthDate() );
        entity.setGender( request.gender() );
        entity.setNationality( request.nationality() );
        entity.setPlaceOfBirth( request.placeOfBirth() );
        entity.setDeathDate( request.deathDate() );
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
}
