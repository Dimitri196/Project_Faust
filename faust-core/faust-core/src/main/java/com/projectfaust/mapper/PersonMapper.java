package com.projectfaust.mapper;

import com.projectfaust.dto.request.PersonRequest;
import com.projectfaust.dto.response.FinancialOperationsResponse;
import com.projectfaust.dto.response.PersonResponse;
import com.projectfaust.entity.*;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for the Person domain within Project Faust.
 * <p>
 * Orchestrates the seamless data conversion between complex entity graphs and lightweight DTOs
 * consumed by the Single Page Application. It manages tactical intelligence logic including
 * extracting primary legal identities, computing age, and exposing temporal communication matrices.
 * </p>
 * * @author Dimitri
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.UUID.class, com.projectfaust.entity.enums.ContactType.class }
)
public interface PersonMapper {

    @Mapping(source = "entity.externalId", target = "publicId")
    @Mapping(target = "displayName", expression = "java(entity.getFullName())")
    @Mapping(target = "age", expression = "java(entity.getAge())")
    @Mapping(target = "firstName", expression = "java(getPrimaryFirstName(entity))")
    @Mapping(target = "lastName", expression = "java(getPrimaryLastName(entity))")
    @Mapping(source = "entity.names", target = "nameHistory")
    @Mapping(source = "entity.contacts", target = "contactHistory")
    @Mapping(source = "entity.financialAccounts", target = "financialAccounts") // 👈 PŘIDÁNO
    @Mapping(target = "primaryEmail", expression = "java(entity.getPrimaryEmail())")
    @Mapping(target = "primaryPhone", expression = "java(entity.getPrimaryPhone())")
    PersonResponse toResponse(Person entity);

    PersonResponse.PersonNameDto toNameDto(PersonName name);

    // Opraven návratový typ na správný vnořený record z PersonResponse
    PersonResponse.PersonContactDto toContactDto(PersonContact contact);

    // 👈 NOVÉ: Mapování finančního vztahu pro osobu
    PersonResponse.PersonFinancialDto toFinancialDto(PersonAccountRelation relation);

    // 👈 NOVÉ: Mapování samotného bankovního účtu na Response
    @Mapping(source = "externalId", target = "publicId")
    PersonResponse.BankAccountResponse toBankAccountResponse(BankAccount account);

    // U zápisových metod ignorujeme 'financialAccounts' – ty si synchronizuješ ručně v servise!
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "names", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "financialAccounts", ignore = true) // 👈 PŘIDÁNO
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "sourceConnections", ignore = true)
    Person toEntity(PersonRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "names", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "financialAccounts", ignore = true) // 👈 PŘIDÁNO
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    @Mapping(target = "appointments", ignore = true)
    @Mapping(target = "sourceConnections", ignore = true)
    void updateEntityFromRequest(PersonRequest request, @MappingTarget Person entity);

    @Mapping(source = "id", target = "relationId")
    @Mapping(source = "bankAccount.externalId", target = "bankAccountPublicId")
    @Mapping(source = "bankAccount.iban", target = "iban")
    @Mapping(source = "bankAccount.bic", target = "bic")
    @Mapping(source = "bankAccount.bankName", target = "bankName")
    @Mapping(source = "bankAccount.currency", target = "currency")
    @Mapping(source = "bankAccount.monitored", target = "isMonitored")
    @Mapping(source = "person.externalId", target = "personPublicId")
    @Mapping(target = "personDisplayName", expression = "java(relation.getPerson().getFullName())")
    @Mapping(source = "roleType", target = "personRoleType")
    @Mapping(target = "institutionPublicId", ignore = true)
    @Mapping(target = "institutionName", ignore = true)
    @Mapping(target = "institutionRoleType", ignore = true)
    FinancialOperationsResponse toOperationsResponse(PersonAccountRelation relation);

    List<PersonResponse> toResponseList(List<Person> entities);

    default String getPrimaryFirstName(Person entity) {
        if (entity == null) return null;
        PersonName primary = entity.getPrimaryNameRecord();
        return (primary != null) ? primary.getFirstName() : entity.getFirstName();
    }

    default String getPrimaryLastName(Person entity) {
        if (entity == null) return null;
        PersonName primary = entity.getPrimaryNameRecord();
        return (primary != null) ? primary.getLastName() : entity.getLastName();
    }
}