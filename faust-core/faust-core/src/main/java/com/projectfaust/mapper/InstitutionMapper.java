package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.*;
import com.projectfaust.entity.BankAccount;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.InstitutionAccountRelation;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;


@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.Collections.class }
)
public interface InstitutionMapper {

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent.externalId", target = "parentId")
    @Mapping(source = "location.externalId", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    @Mapping(source = "financialAccounts", target = "financialAccounts") // 👈 PŘIDÁNO
    InstitutionResponse toResponse(Institution entity);

    List<InstitutionResponse> toResponseList(List<Institution> entities);

    // 👈 NOVÉ: Mapování finančního vztahu pro instituci
    InstitutionResponse.InstitutionFinancialDto toFinancialDto(InstitutionAccountRelation relation);

    // 👈 NOVÉ: Mapování bankovního účtu (využívá strukturu definovanou v PersonResponse)
    @Mapping(source = "externalId", target = "publicId")
    PersonResponse.BankAccountResponse toBankAccountResponse(BankAccount account);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "financialAccounts", ignore = true) // 👈 PŘIDÁNO: Zpracuje service layer
    Institution toEntity(InstitutionRequest request);

    /**
     * REKURZIVNÍ MAPOVÁNÍ (Pro Nexus Focus / Hluboký Graf)
     */
    @Named("toTreeDeep")
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", qualifiedByName = "toTreeDeep")
    InstitutionTreeResponse toTreeResponse(Institution entity);

    @Mapping(source = "id", target = "relationId")
    @Mapping(source = "bankAccount.externalId", target = "bankAccountPublicId")
    @Mapping(source = "bankAccount.iban", target = "iban")
    @Mapping(source = "bankAccount.bic", target = "bic")
    @Mapping(source = "bankAccount.bankName", target = "bankName")
    @Mapping(source = "bankAccount.currency", target = "currency")
    @Mapping(source = "bankAccount.monitored", target = "isMonitored")
    @Mapping(target = "personPublicId", ignore = true)
    @Mapping(target = "personDisplayName", ignore = true)
    @Mapping(target = "personRoleType", ignore = true)
    @Mapping(source = "institution.externalId", target = "institutionPublicId")
    @Mapping(source = "institution.name", target = "institutionName")
    @Mapping(source = "roleType", target = "institutionRoleType")
    FinancialOperationsResponse toOperationsResponse(InstitutionAccountRelation relation);

    @Named("toTreeDeep")
    default List<InstitutionTreeResponse> toTreeResponseListDeep(List<Institution> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(this::toTreeResponse)
                .toList();
    }

    /**
     * PLOCHÉ MAPOVÁNÍ (Pro Lazy Loading ve stromu)
     */
    @Named("toTreeFlat")
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(source = "logoUrl", target = "logoUrl")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    @Mapping(target = "hasChildren", expression = "java(!entity.getChildren().isEmpty())")
    InstitutionTreeResponse toFlatTreeResponse(Institution entity);

    default List<InstitutionTreeResponse> toFlatTreeResponseList(List<Institution> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toFlatTreeResponse)
                .toList();
    }

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent", target = "parent")
    @Mapping(source = "stateOwned", target = "isStateOwned")
    InstitutionAscendedResponse toAscendedResponse(Institution entity);
}