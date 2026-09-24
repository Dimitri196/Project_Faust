package com.projectfaust.financial;

import com.projectfaust.financial.dto.BankAccountRequest;
import com.projectfaust.financial.dto.BankAccountResponse;
import com.projectfaust.financial.dto.PersonFinancialRelationResponse;
import com.projectfaust.financial.dto.InstitutionFinancialRelationResponse;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link BankAccount} and its relation entities.
 *
 * <p>FK resolution (person / institution linkage) is handled by the service layer.
 * The mapper handles scalar field mapping and outbound UUID extraction.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface BankAccountMapper {

    // ── BankAccount → ResponseDto ─────────────────────────────────────────────

    @Mapping(source = "externalId", target = "publicId")
    BankAccountResponse toResponse(BankAccount entity);

    // ── RequestDto → new BankAccount ─────────────────────────────────────────

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "accountHolders",     ignore = true)
    @Mapping(target = "institutionalOwners",ignore = true)
    BankAccount toEntity(BankAccountRequest dto);

    // ── RequestDto → partial update ───────────────────────────────────────────

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "accountHolders",     ignore = true)
    @Mapping(target = "institutionalOwners",ignore = true)
    void updateEntity(BankAccountRequest dto, @MappingTarget BankAccount entity);

    // ── PersonAccountRelation → ResponseDto ──────────────────────────────────

    @Mapping(source = "externalId",                   target = "publicId")
    @Mapping(source = "person.externalId",            target = "personPublicId")
    @Mapping(source = "bankAccount.externalId",       target = "accountPublicId")
    @Mapping(source = "bankAccount.iban",             target = "iban")
    @Mapping(source = "bankAccount.bankName",         target = "bankName")
    PersonFinancialRelationResponse toPersonRelationResponse(PersonAccountRelation entity);

    // ── InstitutionAccountRelation → ResponseDto ──────────────────────────────

    @Mapping(source = "institution.externalId",  target = "institutionPublicId")
    @Mapping(source = "institution.name",        target = "institutionName")
    @Mapping(source = "bankAccount.externalId",  target = "accountPublicId")
    @Mapping(source = "bankAccount.iban",        target = "iban")
    @Mapping(source = "bankAccount.bankName",    target = "bankName")
    InstitutionFinancialRelationResponse toInstitutionRelationResponse(InstitutionAccountRelation entity);
}
