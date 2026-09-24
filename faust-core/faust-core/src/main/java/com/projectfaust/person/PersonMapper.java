package com.projectfaust.person;

import com.projectfaust.financial.PersonAccountRelation;
import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.person.contact.PersonContact;
import com.projectfaust.person.dto.PersonRequest;
import com.projectfaust.person.dto.PersonResponse;
import com.projectfaust.shared.enums.ContactType;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper for the {@link Person} domain within Project Faust.
 *
 * <p>Handles four mapping concerns:</p>
 * <ul>
 *   <li><b>Entity → response</b> — flattens the full person graph into a
 *       {@link PersonResponse} dossier, resolving primary name, computed fields,
 *       and all nested collections.</li>
 *   <li><b>Request → entity</b> — initialises a new {@link Person} from a request,
 *       excluding collections and audit fields managed by the service layer.</li>
 *   <li><b>In-place update</b> — {@link #updateEntityFromRequest} applies scalar
 *       field changes to an existing entity without replacing its identity or collections.</li>
 *   <li><b>Financial operations</b> — maps {@link PersonAccountRelation} to the shared
 *       {@link FinancialOperationsResponse} with person-side fields populated.</li>
 * </ul>
 *
 * <p><b>currentPositions note:</b> the {@code currentPositions} field on
 * {@link PersonResponse} is resolved by the service layer (via the appointment
 * repository) and injected after mapping. It is marked {@code ignore = true}
 * in the mapper — the service enriches the response in the same pattern used by
 * {@code InstitutionService.toEnrichedResponse}.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.UUID.class, ContactType.class }
)
public interface PersonMapper {

    // -------------------------------------------------------------------------
    // Entity → response
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Person} entity to a full {@link PersonResponse} dossier.
     *
     * <p>{@code currentPositions} is excluded — resolved and injected by the service.
     * {@code verificationStatus}, {@code createdAt}, and {@code updatedAt} map
     * automatically by name.</p>
     *
     * @param entity the person entity.
     * @return a full dossier response DTO.
     */
    @Mapping(source = "entity.externalId",          target = "publicId")
    @Mapping(target = "displayName",                expression = "java(entity.getFullName())")
    @Mapping(target = "age",                        expression = "java(entity.getAge())")
    @Mapping(target = "firstName",                  expression = "java(getPrimaryFirstName(entity))")
    @Mapping(target = "lastName",                   expression = "java(getPrimaryLastName(entity))")
    @Mapping(source = "entity.names",               target = "nameHistory")
    @Mapping(source = "entity.contacts",            target = "contactHistory")
    @Mapping(source = "entity.financialAccounts",   target = "financialAccounts")
    @Mapping(target = "primaryEmail",               expression = "java(entity.getPrimaryEmail())")
    @Mapping(target = "primaryPhone",               expression = "java(entity.getPrimaryPhone())")
    @Mapping(target = "currentPositions",           ignore = true)
    PersonResponse toResponse(Person entity);

    /**
     * Bulk response mapping.
     *
     * @param entities list of person entities.
     * @return list of response DTOs.
     */
    List<PersonResponse> toResponseList(List<Person> entities);

    /**
     * Maps a {@link PersonName} to a {@link PersonResponse.PersonNameDto}.
     *
     * @param name the name entity.
     * @return a name DTO for the response.
     */
    PersonResponse.PersonNameDto toNameDto(PersonName name);

    /**
     * Maps a {@link PersonContact} to a {@link PersonResponse.PersonContactDto}.
     *
     * @param contact the contact entity.
     * @return a contact DTO for the response.
     */
    PersonResponse.PersonContactDto toContactDto(PersonContact contact);

    /**
     * Maps a {@link PersonAccountRelation} to a {@link PersonResponse.PersonFinancialDto}.
     *
     * <p>Uses {@code externalId} as {@code relationId} — the internal Long id
     * must never cross the API boundary.</p>
     *
     * @param relation the financial account relationship.
     * @return a financial DTO for the person dossier.
     */
    @Mapping(source = "externalId",            target = "relationId")
    @Mapping(source = "bankAccount",           target = "bankAccount")
    PersonResponse.PersonFinancialDto toFinancialDto(PersonAccountRelation relation);

    // -------------------------------------------------------------------------
    // Entity creation and update
    // -------------------------------------------------------------------------

    /**
     * Initialises a new {@link Person} entity from a {@link PersonRequest}.
     *
     * <p>The following fields are excluded and handled by the service or JPA:</p>
     * <ul>
     *   <li>{@code id} / {@code externalId} — assigned by JPA.</li>
     *   <li>{@code names} / {@code contacts} / {@code financialAccounts} /
     *       {@code sourceConnections} / {@code appointments} — managed by service.</li>
     *   <li>{@code fullNameSearchNormalized} — managed by DB trigger.</li>
     *   <li>{@code createdAt} / {@code updatedAt} — set by @PrePersist / @PreUpdate.</li>
     *   <li>{@code verificationStatus} — defaults to PENDING_REVIEW on entity.</li>
     * </ul>
     *
     * @param request the creation DTO.
     * @return an entity ready for collection population and persistence.
     */
    @Mapping(target = "id",                     ignore = true)
    @Mapping(target = "externalId",             ignore = true)
    @Mapping(target = "names",                  ignore = true)
    @Mapping(target = "contacts",               ignore = true)
    @Mapping(target = "financialAccounts",      ignore = true)
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    @Mapping(target = "appointments",           ignore = true)
    @Mapping(target = "sourceConnections",      ignore = true)
    @Mapping(target = "createdAt",              ignore = true)
    @Mapping(target = "updatedAt",              ignore = true)
    @Mapping(target = "verificationStatus",     ignore = true)
    Person toEntity(PersonRequest request);

    /**
     * Applies scalar field changes from a request onto an existing {@link Person} entity.
     *
     * <p>Updates in place — preserves the entity's identity, collections, and audit
     * fields. Only scalar biographical fields are updated.</p>
     *
     * @param request the update DTO.
     * @param entity  the entity to update (annotated with {@code @MappingTarget}).
     */
    @Mapping(target = "id",                     ignore = true)
    @Mapping(target = "externalId",             ignore = true)
    @Mapping(target = "names",                  ignore = true)
    @Mapping(target = "contacts",               ignore = true)
    @Mapping(target = "financialAccounts",      ignore = true)
    @Mapping(target = "fullNameSearchNormalized", ignore = true)
    @Mapping(target = "appointments",           ignore = true)
    @Mapping(target = "sourceConnections",      ignore = true)
    @Mapping(target = "createdAt",              ignore = true)
    @Mapping(target = "updatedAt",              ignore = true)
    @Mapping(target = "verificationStatus",     ignore = true)
    void updateEntityFromRequest(PersonRequest request, @MappingTarget Person entity);

    // -------------------------------------------------------------------------
    // Financial operations mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link PersonAccountRelation} to the shared {@link FinancialOperationsResponse}.
     *
     * <p>Institution-side fields ({@code institutionPublicId}, {@code institutionName},
     * {@code institutionRoleType}) are intentionally null — populated by the institution
     * mapper when this DTO is used in an institution context.</p>
     *
     * @param relation the person account relationship.
     * @return a shared financial operations response with person fields populated.
     */
    @Mapping(source = "id",                         target = "relationId")
    @Mapping(source = "bankAccount.externalId",     target = "bankAccountPublicId")
    @Mapping(source = "bankAccount.iban",           target = "iban")
    @Mapping(source = "bankAccount.bic",            target = "bic")
    @Mapping(source = "bankAccount.bankName",       target = "bankName")
    @Mapping(source = "bankAccount.currency",       target = "currency")
    @Mapping(source = "bankAccount.monitored",      target = "monitored")
    @Mapping(source = "person.externalId",          target = "personPublicId")
    @Mapping(target = "personDisplayName",          expression = "java(relation.getPerson().getFullName())")
    @Mapping(source = "roleType",                   target = "personRoleType")
    @Mapping(target = "institutionPublicId",        ignore = true)
    @Mapping(target = "institutionName",            ignore = true)
    @Mapping(target = "institutionRoleType",        ignore = true)
    FinancialOperationsResponse toOperationsResponse(PersonAccountRelation relation);

    // -------------------------------------------------------------------------
    // Default helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the primary first name — from the primary {@link PersonName} record
     * if available, falling back to the deprecated {@link Person#getFirstName()} field.
     *
     * @param entity the person entity.
     * @return the primary first name, or {@code null} if unavailable.
     */
    default String getPrimaryFirstName(Person entity) {
        if (entity == null) return null;
        PersonName primary = entity.getPrimaryNameRecord();
        return (primary != null) ? primary.getFirstName() : entity.getFirstName();
    }

    /**
     * Resolves the primary last name — from the primary {@link PersonName} record
     * if available, falling back to the deprecated {@link Person#getLastName()} field.
     *
     * @param entity the person entity.
     * @return the primary last name, or {@code null} if unavailable.
     */
    default String getPrimaryLastName(Person entity) {
        if (entity == null) return null;
        PersonName primary = entity.getPrimaryNameRecord();
        return (primary != null) ? primary.getLastName() : entity.getLastName();
    }
}