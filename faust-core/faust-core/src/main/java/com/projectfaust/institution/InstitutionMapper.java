package com.projectfaust.institution;

import com.projectfaust.financial.BankAccount;
import com.projectfaust.financial.InstitutionAccountRelation;
import com.projectfaust.financial.dto.FinancialOperationsResponse;
import com.projectfaust.institution.dto.InstitutionAscendedResponse;
import com.projectfaust.institution.dto.InstitutionRequest;
import com.projectfaust.institution.dto.InstitutionResponse;
import com.projectfaust.institution.dto.InstitutionTreeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

/**
 * MapStruct mapper for the {@link Institution} domain.
 *
 * <p>Provides four distinct response projections to serve different frontend use cases:</p>
 * <ul>
 *   <li>{@link #toResponse} — standard flat response for list views and dossier panels.</li>
 *   <li>{@link #toTreeResponse} — deep recursive tree for the Nexus Focus graph; requires
 *       children to be eagerly loaded (JOIN FETCH) before calling to avoid
 *       {@code LazyInitializationException}.</li>
 *   <li>{@link #toFlatTreeResponse} — shallow tree node for lazy-load drill-down navigation;
 *       children collection is intentionally omitted.</li>
 *   <li>{@link #toAscendedResponse} — ancestor chain projection for breadcrumb rendering.</li>
 * </ul>
 *
 * <p><b>hasChildren note:</b> the {@code hasChildren} flag is resolved by the service layer
 * via {@link InstitutionRepository#existsActiveChildByParentExternalId} before calling the
 * mapper, and passed as an explicit parameter. This avoids triggering a lazy collection load
 * inside the mapper, which would throw outside a {@code @Transactional} boundary.</p>
 *
 * <p><b>parentId in tree responses:</b> both {@link #toTreeResponse} and
 * {@link #toFlatTreeResponse} map {@code parent.externalId} to the scalar {@code parentId}
 * field rather than nesting a full parent object. This prevents the bidirectional
 * parent+children recursion that causes infinite loops during JSON serialisation.</p>
 *
 * <p><b>Module boundary:</b> {@link BankAccount} mapping is delegated to the
 * {@code financial} module mapper. This mapper owns only institution-domain projections.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.Collections.class }
)
public interface InstitutionMapper {

    // -------------------------------------------------------------------------
    // Standard response mapping
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link Institution} entity to a flat {@link InstitutionResponse}.
     *
     * <p>{@code hasChildren} must be resolved by the service before calling this method
     * (via a repository existence check) and passed as an explicit parameter.</p>
     *
     * @param entity      the institution entity.
     * @param hasChildren whether the institution has at least one active child node.
     * @return a flat response DTO safe for REST serialisation.
     */
    @Mapping(source = "entity.externalId",          target = "publicId")
    @Mapping(source = "entity.parent.externalId",   target = "parentId")
    @Mapping(source = "entity.location.externalId", target = "locationId")
    @Mapping(source = "entity.location.name",       target = "locationName")
    @Mapping(source = "entity.stateOwned",          target = "stateOwned")
    @Mapping(source = "entity.financialAccounts",   target = "financialAccounts")
    @Mapping(source = "hasChildren",                target = "hasChildren")
    InstitutionResponse toResponse(Institution entity, boolean hasChildren);

    /**
     * Convenience bulk mapper for list endpoints.
     *
     * <p>Note: {@code hasChildren} defaults to {@code false} in this variant —
     * use {@link #toResponse(Institution, boolean)} individually when the flag matters.</p>
     *
     * @param entities list of institution entities.
     * @return list of flat response DTOs.
     */
    List<InstitutionResponse> toResponseList(List<Institution> entities);

    // -------------------------------------------------------------------------
    // Financial relation mapping
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link InstitutionAccountRelation} to a nested financial DTO
     * embedded within {@link InstitutionResponse}.
     *
     * @param relation the financial account relationship.
     * @return a compact financial DTO.
     */
    InstitutionResponse.InstitutionFinancialDto toFinancialDto(InstitutionAccountRelation relation);

    /**
     * Maps an {@link InstitutionAccountRelation} to the shared
     * {@link FinancialOperationsResponse} used across institution and person contexts.
     *
     * <p>Person-side fields ({@code personPublicId}, {@code personDisplayName},
     * {@code personRoleType}) are intentionally left null — they are populated
     * by the person-side mapper when this DTO is used in a person context.</p>
     *
     * @param relation the institution account relationship.
     * @return a shared financial operations response with institution fields populated.
     */
    @Mapping(source = "id",                         target = "relationId")
    @Mapping(source = "bankAccount.externalId",     target = "bankAccountPublicId")
    @Mapping(source = "bankAccount.iban",           target = "iban")
    @Mapping(source = "bankAccount.bic",            target = "bic")
    @Mapping(source = "bankAccount.bankName",       target = "bankName")
    @Mapping(source = "bankAccount.currency",       target = "currency")
    @Mapping(source = "bankAccount.monitored",      target = "monitored")
    @Mapping(source = "institution.externalId",     target = "institutionPublicId")
    @Mapping(source = "institution.name",           target = "institutionName")
    @Mapping(source = "roleType",                   target = "institutionRoleType")
    @Mapping(target = "personPublicId",             ignore = true)
    @Mapping(target = "personDisplayName",          ignore = true)
    @Mapping(target = "personRoleType",             ignore = true)
    FinancialOperationsResponse toOperationsResponse(InstitutionAccountRelation relation);

    // -------------------------------------------------------------------------
    // Entity creation mapping
    // -------------------------------------------------------------------------

    /**
     * Initialises a new {@link Institution} entity from an {@link InstitutionRequest} DTO.
     *
     * <p>The following fields are intentionally excluded and must be handled
     * by the service layer or JPA lifecycle callbacks:</p>
     * <ul>
     *   <li>{@code id} / {@code externalId} — assigned by JPA and Builder.Default.</li>
     *   <li>{@code parent} — resolved from {@code parentExternalId} by the service.</li>
     *   <li>{@code children} / {@code financialAccounts} — managed as collections.</li>
     *   <li>{@code location} — resolved from {@code locationId} by the service.</li>
     *   <li>{@code createdAt} / {@code updatedAt} — set by @PrePersist / @PreUpdate.</li>
     *   <li>{@code verificationStatus} — defaults to PENDING_REVIEW in the service.</li>
     * </ul>
     *
     * @param request the creation DTO.
     * @return an entity ready for parent/location resolution and persistence.
     */
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "parent",             ignore = true)
    @Mapping(target = "children",           ignore = true)
    @Mapping(target = "location",           ignore = true)
    @Mapping(target = "financialAccounts",  ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    Institution toEntity(InstitutionRequest request);

    // -------------------------------------------------------------------------
    // Tree mappings — Nexus Focus graph
    // -------------------------------------------------------------------------

    /**
     * Deep recursive tree mapping for the Nexus Focus visualisation.
     *
     * <p><b>Transaction boundary:</b> this method must only be called when the
     * institution's {@code children} collection has been eagerly loaded via a
     * {@code JOIN FETCH} or {@code @EntityGraph} query. Calling it on a lazily-loaded
     * entity outside a {@code @Transactional} boundary will throw a
     * {@code LazyInitializationException}.</p>
     *
     * <p><b>Serialisation safety:</b> {@code parentId} is mapped from
     * {@code parent.externalId} as a scalar UUID rather than a nested object.
     * This prevents the bidirectional parent+children recursion that would cause
     * an infinite loop during JSON serialisation.</p>
     *
     * @param entity the institution with eagerly-loaded children.
     * @return a recursive tree response for deep graph rendering.
     */
    @Named("toTreeDeep")
    @Mapping(source = "externalId",         target = "publicId")
    @Mapping(source = "stateOwned",         target = "stateOwned")
    @Mapping(source = "logoUrl",            target = "logoUrl")
    @Mapping(source = "parent.externalId",  target = "parentId")
    @Mapping(target = "hasChildren",        expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(target = "children",           qualifiedByName = "toTreeDeep")
    InstitutionTreeResponse toTreeResponse(Institution entity);

    /**
     * Converts a list of institutions to deep tree responses.
     * See {@link #toTreeResponse} for transaction boundary requirements.
     *
     * @param entities list of institutions with eagerly-loaded children.
     * @return list of recursive tree responses.
     */
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
     * Flat (shallow) tree node mapping for lazy-load drill-down navigation.
     *
     * <p>The {@code children} collection is intentionally omitted — the frontend
     * requests children on demand when a node is expanded via
     * {@code GET /parent/{id}}.</p>
     *
     * <p><b>Serialisation safety:</b> {@code parentId} is mapped as a scalar UUID,
     * same as the deep variant, preventing any recursion risk.</p>
     *
     * @param entity the institution entity.
     * @return a shallow tree node with parentId but without children.
     */
    @Named("toTreeFlat")
    @Mapping(source = "externalId",         target = "publicId")
    @Mapping(source = "stateOwned",         target = "stateOwned")
    @Mapping(source = "logoUrl",            target = "logoUrl")
    @Mapping(source = "parent.externalId",  target = "parentId")
    @Mapping(target = "hasChildren",        expression = "java(!entity.getChildren().isEmpty())")
    @Mapping(target = "children",           ignore = true)
    InstitutionTreeResponse toFlatTreeResponse(Institution entity);

    /**
     * Converts a list of institutions to flat tree nodes.
     *
     * @param entities list of institution entities.
     * @return list of shallow tree nodes.
     */
    default List<InstitutionTreeResponse> toFlatTreeResponseList(List<Institution> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream()
                .map(this::toFlatTreeResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Ancestor chain mapping
    // -------------------------------------------------------------------------

    /**
     * Maps an institution to an {@link InstitutionAscendedResponse} for breadcrumb rendering.
     *
     * <p>The {@code parent} field is mapped recursively to build the full ancestor chain.
     * This is serialisation-safe — the chain is strictly unidirectional (child → parent)
     * with no children collection, so it terminates at the root where {@code parent}
     * is {@code null}.</p>
     *
     * <p>Requires the parent chain to be loaded within an active {@code @Transactional}
     * boundary to avoid lazy-load exceptions.</p>
     *
     * @param entity the institution entity.
     * @return an ascended response with parent chain for breadcrumb display.
     */
    @Mapping(source = "externalId", target = "publicId")
    @Mapping(source = "parent",     target = "parent")
    @Mapping(source = "stateOwned", target = "stateOwned")
    InstitutionAscendedResponse toAscendedResponse(Institution entity);
}