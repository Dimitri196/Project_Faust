package com.projectfaust.mobility;

import com.projectfaust.mobility.dto.MobilityEventRequestDto;
import com.projectfaust.mobility.dto.MobilityEventResponseDto;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * MapStruct mapper for {@link MobilityEvent} ↔ DTOs.
 *
 * <p>Subject FK resolution (person / vehicle) is performed by the service layer.
 * The mapper handles the outbound path: extracting public UUIDs and computing
 * the {@code violationOverdue} convenience flag.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MobilityEventMapper {

    // ── MobilityEvent → ResponseDto ───────────────────────────────────────────

    @Mapping(source = "externalId",            target = "publicId")
    @Mapping(target = "subjectPersonPublicId",  expression = "java(resolvePersonPublicId(entity.getSubjectPerson()))")
    @Mapping(target = "subjectVehiclePublicId", expression = "java(resolveVehiclePublicId(entity.getSubjectVehicle()))")
    @Mapping(target = "violationOverdue",       expression = "java(isViolationOverdue(entity))")
    MobilityEventResponseDto toResponse(MobilityEvent entity);

    // ── RequestDto → new Entity ───────────────────────────────────────────────

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "externalId",     ignore = true)
    @Mapping(target = "subjectPerson",  ignore = true)
    @Mapping(target = "subjectVehicle", ignore = true)
    @Mapping(target = "ingestedAt",     ignore = true)
    MobilityEvent toEntity(MobilityEventRequestDto dto);

    // ── RequestDto → partial update ───────────────────────────────────────────

    @Mapping(target = "id",             ignore = true)
    @Mapping(target = "externalId",     ignore = true)
    @Mapping(target = "subjectPerson",  ignore = true)
    @Mapping(target = "subjectVehicle", ignore = true)
    @Mapping(target = "ingestedAt",     ignore = true)
    void updateEntity(MobilityEventRequestDto dto, @MappingTarget MobilityEvent entity);

    // ── Helpers ───────────────────────────────────────────────────────────────

    default UUID resolvePersonPublicId(com.projectfaust.person.Person person) {
        return person != null ? person.getExternalId() : null;
    }

    default UUID resolveVehiclePublicId(com.projectfaust.vehicle.VehicleRecord vehicle) {
        return vehicle != null ? vehicle.getExternalId() : null;
    }

    /**
     * A violation is overdue when:
     * <ul>
     *   <li>the event is a violation type, AND</li>
     *   <li>the fine has not been paid (null treated as unpaid), AND</li>
     *   <li>fineDueDate is non-null and in the past</li>
     * </ul>
     */
    default boolean isViolationOverdue(MobilityEvent entity) {
        if (entity.getFineAmount() == null) return false;
        if (Boolean.TRUE.equals(entity.getFinePaid())) return false;
        return entity.getFineDueDate() != null
                && entity.getFineDueDate().isBefore(LocalDate.now());
    }
}
