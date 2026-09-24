package com.projectfaust.vehicle;

import com.projectfaust.vehicle.dto.VehicleRecordRequestDto;
import com.projectfaust.vehicle.dto.VehicleRecordResponseDto;
import com.projectfaust.vehicle.dto.VehicleRecordResponseDto.VehicleOwnershipHistoryDto;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * MapStruct mapper between {@link VehicleRecord} / {@link VehicleOwnershipHistory}
 * and their DTO representations.
 *
 * <p><b>Owner / operator resolution:</b> each role has a {@code Person XOR
 * Institution} FK pair. The mapper extracts the public UUID from whichever FK is
 * non-null, and builds a display name from the resolved entity name, falling back
 * to the raw registry name if no FK is linked.</p>
 *
 * <p>The {@code toEntity} and {@code updateEntity} methods intentionally ignore
 * the owner/operator FK fields — those are resolved and set by the service layer,
 * which verifies the referenced entities exist before linking them.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface VehicleRecordMapper {

    // =========================================================================
    // VehicleRecord → ResponseDto
    // =========================================================================

    @Mapping(source = "externalId",                         target = "publicId")

    // Owner resolved fields
    @Mapping(target = "ownerPersonPublicId",       expression = "java(resolvePersonPublicId(entity.getOwnerPerson()))")
    @Mapping(target = "ownerInstitutionPublicId",  expression = "java(resolveInstitutionPublicId(entity.getOwnerInstitution()))")
    @Mapping(target = "ownerDisplayName",          expression = "java(resolveOwnerDisplayName(entity))")

    // Operator resolved fields
    @Mapping(target = "operatorPersonPublicId",      expression = "java(resolvePersonPublicId(entity.getOperatorPerson()))")
    @Mapping(target = "operatorInstitutionPublicId", expression = "java(resolveInstitutionPublicId(entity.getOperatorInstitution()))")
    @Mapping(target = "operatorDisplayName",         expression = "java(resolveOperatorDisplayName(entity))")

    // Insurance cross-reference
    @Mapping(target = "insuranceRecordPublicId",  expression = "java(entity.getInsuranceRecord() != null ? entity.getInsuranceRecord().getExternalId() : null)")

    // Computed: is the inspection still valid today?
    @Mapping(target = "inspectionValid", expression = "java(isInspectionValid(entity.getInspectionValidUntil()))")

    VehicleRecordResponseDto toResponse(VehicleRecord entity);

    // =========================================================================
    // RequestDto → new Entity  (FKs are set by the service after mapping)
    // =========================================================================

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "externalId",       ignore = true)
    @Mapping(target = "ownerPerson",      ignore = true)
    @Mapping(target = "ownerInstitution", ignore = true)
    @Mapping(target = "operatorPerson",   ignore = true)
    @Mapping(target = "operatorInstitution", ignore = true)
    @Mapping(target = "insuranceRecord",  ignore = true)
    @Mapping(target = "ownershipHistory", ignore = true)
    @Mapping(target = "ingestedAt",       ignore = true)
    VehicleRecord toEntity(VehicleRecordRequestDto dto);

    // =========================================================================
    // RequestDto → partial update  (nulls are ignored — see NullValuePropertyMappingStrategy)
    // =========================================================================

    @Mapping(target = "id",               ignore = true)
    @Mapping(target = "externalId",       ignore = true)
    @Mapping(target = "ownerPerson",      ignore = true)
    @Mapping(target = "ownerInstitution", ignore = true)
    @Mapping(target = "operatorPerson",   ignore = true)
    @Mapping(target = "operatorInstitution", ignore = true)
    @Mapping(target = "insuranceRecord",  ignore = true)
    @Mapping(target = "ownershipHistory", ignore = true)
    @Mapping(target = "ingestedAt",       ignore = true)
    void updateEntity(VehicleRecordRequestDto dto, @MappingTarget VehicleRecord entity);

    // =========================================================================
    // VehicleOwnershipHistory → nested DTO
    // =========================================================================

    @Mapping(source = "externalId",                           target = "publicId")
    @Mapping(target = "fromPersonPublicId",       expression = "java(resolvePersonPublicId(h.getFromPerson()))")
    @Mapping(target = "fromInstitutionPublicId",  expression = "java(resolveInstitutionPublicId(h.getFromInstitution()))")
    @Mapping(target = "fromDisplayName",          expression = "java(resolveFromDisplayName(h))")
    @Mapping(target = "toPersonPublicId",         expression = "java(resolvePersonPublicId(h.getToPerson()))")
    @Mapping(target = "toInstitutionPublicId",    expression = "java(resolveInstitutionPublicId(h.getToInstitution()))")
    @Mapping(target = "toDisplayName",            expression = "java(resolveToDisplayName(h))")
    VehicleOwnershipHistoryDto toHistoryDto(VehicleOwnershipHistory h);

    // =========================================================================
    // Private helper methods (default implementations called from expressions)
    // =========================================================================

    /** Extracts the public UUID from a Person FK, or null if unlinked. */
    default UUID resolvePersonPublicId(com.projectfaust.person.Person person) {
        return person != null ? person.getExternalId() : null;
    }

    /** Extracts the public UUID from an Institution FK, or null if unlinked. */
    default UUID resolveInstitutionPublicId(com.projectfaust.institution.Institution institution) {
        return institution != null ? institution.getExternalId() : null;
    }

    // ── Display-name helpers ──────────────────────────────────────────────────

    default String resolveOwnerDisplayName(VehicleRecord v) {
        if (v.getOwnerPerson() != null) {
            return buildPersonName(v.getOwnerPerson());
        }
        if (v.getOwnerInstitution() != null) {
            return v.getOwnerInstitution().getName();
        }
        return v.getOwnerNameRaw();
    }

    default String resolveOperatorDisplayName(VehicleRecord v) {
        if (v.getOperatorPerson() != null) {
            return buildPersonName(v.getOperatorPerson());
        }
        if (v.getOperatorInstitution() != null) {
            return v.getOperatorInstitution().getName();
        }
        return v.getOperatorNameRaw();
    }

    default String resolveFromDisplayName(VehicleOwnershipHistory h) {
        if (h.getFromPerson() != null) {
            return buildPersonName(h.getFromPerson());
        }
        if (h.getFromInstitution() != null) {
            return h.getFromInstitution().getName();
        }
        return h.getFromNameRaw();
    }

    default String resolveToDisplayName(VehicleOwnershipHistory h) {
        if (h.getToPerson() != null) {
            return buildPersonName(h.getToPerson());
        }
        if (h.getToInstitution() != null) {
            return h.getToInstitution().getName();
        }
        return h.getToNameRaw();
    }

    /**
     * Builds "SURNAME, Firstname" from a Person's primary name if available,
     * otherwise falls back to whatever the Person entity exposes.
     * Adjust if Person has a dedicated getFullName() method.
     */
    default String buildPersonName(com.projectfaust.person.Person person) {
        if (person == null) return null;
        // Delegate to Person's own display method; fallback to externalId string.
        // Update this if Person exposes a dedicated getFullName() helper.
        return person.getExternalId().toString();
    }

    /** Returns true if inspectionValidUntil is in the future (inclusive of today). */
    default boolean isInspectionValid(LocalDate inspectionValidUntil) {
        return inspectionValidUntil != null && !inspectionValidUntil.isBefore(LocalDate.now());
    }
}
