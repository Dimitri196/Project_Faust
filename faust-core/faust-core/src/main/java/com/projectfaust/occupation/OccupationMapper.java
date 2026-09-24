package com.projectfaust.occupation;

import com.projectfaust.occupation.dto.OccupationAscendedResponse;
import com.projectfaust.occupation.dto.OccupationRequest;
import com.projectfaust.occupation.dto.OccupationResponse;
import com.projectfaust.occupation.dto.OccupationTreeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

/**
 * MapStruct mapper for the {@link Occupation} domain.
 *
 * <p>Provides three response projections:</p>
 * <ul>
 *   <li>{@link #toResponse} — flat response for list views and dossier panels.</li>
 *   <li>{@link #toTreeResponse} — recursive downward tree for command chain visualisation.
 *       Requires subordinates to be eagerly loaded before calling.</li>
 *   <li>{@link #toAscendedResponse} — upward chain of command for breadcrumb rendering.</li>
 * </ul>
 *
 * <p><b>Current occupant resolution:</b> {@link #mapCurrentOccupant} and
 * {@link #mapCurrentOccupantId} resolve the active {@link Appointment} to surface
 * the current holder's name and UUID. These methods handle vacant, acting, and
 * in-transition states.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OccupationMapper {

    // -------------------------------------------------------------------------
    // Entity creation mapping
    // -------------------------------------------------------------------------

    /**
     * Initialises a new {@link Occupation} entity from an {@link OccupationRequest}.
     *
     * <p>Institution, reporting line, and audit fields are excluded — resolved
     * and set by the service layer after mapping.</p>
     *
     * @param request the creation DTO.
     * @return an entity ready for institution/reportsTo resolution and persistence.
     */
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "institution",        ignore = true)
    @Mapping(target = "reportsTo",          ignore = true)
    @Mapping(target = "subordinates",       ignore = true)
    @Mapping(target = "appointments",       ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    Occupation toEntity(OccupationRequest request);

    // -------------------------------------------------------------------------
    // Standard response mapping
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link Occupation} entity to a flat {@link OccupationResponse}.
     *
     * <p>Institution and reporting-line references are flattened to scalar fields.
     * The current occupant name and UUID are resolved via the default helper methods.</p>
     *
     * @param entity the occupation entity.
     * @return a flat response DTO.
     */
    @Mapping(source = "externalId",             target = "publicId")
    @Mapping(source = "institution.name",       target = "institutionName")
    @Mapping(source = "institution.externalId", target = "institutionPublicId")
    @Mapping(source = "reportsTo.title",        target = "supervisorTitle")
    @Mapping(source = "reportsTo.externalId",   target = "reportsToPublicId")
    @Mapping(target = "currentOccupantName",    expression = "java(mapCurrentOccupant(entity))")
    @Mapping(target = "currentOccupantId",      expression = "java(mapCurrentOccupantId(entity))")
    OccupationResponse toResponse(Occupation entity);

    /**
     * Bulk flat response mapping.
     *
     * @param entities list of occupation entities.
     * @return list of flat response DTOs.
     */
    List<OccupationResponse> toResponseList(List<Occupation> entities);

    // -------------------------------------------------------------------------
    // Tree mapping — command chain (downward)
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link Occupation} to a recursive {@link OccupationTreeResponse}.
     *
     * <p><b>Transaction boundary:</b> subordinates must be eagerly loaded before
     * calling this method. Use
     * {@link OccupationRepository#findWithSubordinatesByExternalId} or
     * {@link OccupationRepository#findTopLevelRoles} which pre-load subordinates.</p>
     *
     * @param entity the occupation with eagerly-loaded subordinates.
     * @return a recursive tree response.
     */
    @Mapping(source = "externalId",           target = "publicId")
    @Mapping(source = "reportsTo.externalId", target = "reportsToPublicId")
    @Mapping(source = "subordinates",         target = "subordinates")
    @Mapping(target = "currentOccupantName",  expression = "java(mapCurrentOccupant(entity))")
    @Mapping(target = "currentOccupantId",    expression = "java(mapCurrentOccupantId(entity))")
    OccupationTreeResponse toTreeResponse(Occupation entity);

    /**
     * Bulk tree response mapping.
     *
     * @param entities list of occupation entities with eagerly-loaded subordinates.
     * @return list of recursive tree responses.
     */
    List<OccupationTreeResponse> toTreeResponseList(List<Occupation> entities);

    // -------------------------------------------------------------------------
    // Ascended mapping — chain of command (upward)
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link Occupation} to an {@link OccupationAscendedResponse} for
     * chain-of-command breadcrumb rendering.
     *
     * <p>The {@code parent} field maps recursively upward to the root.
     * Safe from infinite recursion — upward only, no subordinates collection.</p>
     *
     * @param entity the occupation entity.
     * @return an ascended response with parent chain.
     */
    @Mapping(source = "externalId",          target = "publicId")
    @Mapping(source = "reportsTo",           target = "parent")
    @Mapping(target = "currentOccupantName", expression = "java(mapCurrentOccupant(entity))")
    OccupationAscendedResponse toAscendedResponse(Occupation entity);

    /**
     * Bulk ascended response mapping.
     *
     * @param entities list of occupation entities.
     * @return list of ascended responses.
     */
    List<OccupationAscendedResponse> toAscendedResponseList(List<Occupation> entities);

    // -------------------------------------------------------------------------
    // Default helpers — current occupant resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves the display name of the person currently holding this position.
     *
     * <p>Handles three states:</p>
     * <ul>
     *   <li>{@code VACANT} — no active appointment and position is marked vacant.</li>
     *   <li>{@code (Acting) Name} — active appointment flagged as acting.</li>
     *   <li>{@code Name} — standard active appointment.</li>
     *   <li>{@code In transition} — appointments exist but none is currently active.</li>
     * </ul>
     *
     * @param entity the occupation entity.
     * @return the display name of the current holder, or a status string.
     */
    default String mapCurrentOccupant(Occupation entity) {
        if (entity.getAppointments() == null || entity.getAppointments().isEmpty()) {
            return entity.isVacant() ? "VACANT" : "No active appointment";
        }
        return entity.findCurrentAppointment()
                .map(app -> {
                    String fullName = app.getPerson().getFullName();
                    return app.isActing() ? "(Acting) " + fullName : fullName;
                })
                .orElse(entity.isVacant() ? "VACANT" : "In transition");
    }

    /**
     * Resolves the public UUID of the person currently holding this position.
     *
     * <p>Used by the frontend to navigate from an org-chart node directly
     * to the person's dossier.</p>
     *
     * @param entity the occupation entity.
     * @return the public UUID of the current holder, or {@code null} if vacant.
     */
    default UUID mapCurrentOccupantId(Occupation entity) {
        if (entity.getAppointments() == null) return null;
        return entity.findCurrentAppointment()
                .map(app -> app.getPerson().getExternalId())
                .orElse(null);
    }
}