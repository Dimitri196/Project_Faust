package com.projectfaust.appointment;

import com.projectfaust.appointment.dto.AppointmentRequest;
import com.projectfaust.appointment.dto.AppointmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * MapStruct mapper for the {@link Appointment} domain.
 *
 * <p>Handles two transformations:</p>
 * <ul>
 *   <li>{@link #toEntity} — initialises a new entity from a request DTO. Person and
 *       occupation references are excluded and set by the service after mapping.</li>
 *   <li>{@link #toResponse} — flattens the entity into a response DTO, resolving
 *       person and occupation references to scalar display fields.</li>
 * </ul>
 *
 * <p><b>Boolean field note:</b> entity fields {@code acting} and {@code exOffoAccess}
 * map directly to response fields of the same name. No explicit {@code @Mapping}
 * annotations are needed for these — MapStruct matches by name automatically after
 * the {@code is} prefix was removed from both entity and DTO fields.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AppointmentMapper {

    // -------------------------------------------------------------------------
    // Entity creation mapping
    // -------------------------------------------------------------------------

    /**
     * Initialises a new {@link Appointment} entity from an {@link AppointmentRequest}.
     *
     * <p>The following fields are intentionally excluded and must be set by the
     * service layer:</p>
     * <ul>
     *   <li>{@code id} / {@code externalId} — assigned by JPA.</li>
     *   <li>{@code person} / {@code occupation} — resolved from UUIDs by the service.</li>
     *   <li>{@code createdAt} / {@code updatedAt} — set by @PrePersist / @PreUpdate.</li>
     *   <li>{@code verificationStatus} — defaults to PENDING_REVIEW on the entity.</li>
     * </ul>
     *
     * @param request the creation DTO.
     * @return an entity ready for person/occupation resolution and persistence.
     */
    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "person",             ignore = true)
    @Mapping(target = "occupation",         ignore = true)
    @Mapping(target = "createdAt",          ignore = true)
    @Mapping(target = "updatedAt",          ignore = true)
    @Mapping(target = "verificationStatus", ignore = true)
    Appointment toEntity(AppointmentRequest request);

    // -------------------------------------------------------------------------
    // Response mapping
    // -------------------------------------------------------------------------

    /**
     * Maps an {@link Appointment} entity to a flat {@link AppointmentResponse}.
     *
     * <p>Person and occupation are flattened to scalar fields. The {@code acting}
     * and {@code exOffoAccess} boolean fields map by name automatically — no
     * explicit mapping needed after removing the {@code is} prefix from both sides.</p>
     *
     * @param entity the appointment entity.
     * @return a flat response DTO safe for REST serialisation.
     */
    @Mapping(source = "externalId",            target = "publicId")
    @Mapping(source = "person.externalId",     target = "personPublicId")
    @Mapping(source = "person.fullName",       target = "personDisplayName")
    @Mapping(source = "person.photoUrl",       target = "personPhotoUrl")
    @Mapping(source = "occupation.title",      target = "occupationTitle")
    @Mapping(source = "occupation.externalId", target = "occupationPublicId")
    AppointmentResponse toResponse(Appointment entity);

    /**
     * Bulk response mapping.
     *
     * @param entities list of appointment entities.
     * @return list of flat response DTOs.
     */
    List<AppointmentResponse> toResponseList(List<Appointment> entities);
}