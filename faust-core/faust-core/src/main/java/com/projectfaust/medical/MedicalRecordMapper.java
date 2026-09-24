package com.projectfaust.medical;

import com.projectfaust.medical.dto.MedicalRecordRequestDto;
import com.projectfaust.medical.dto.MedicalRecordResponseDto;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * MapStruct mapper for {@link MedicalRecord} ↔ DTOs.
 *
 * <p>Subject FK resolution (person / institution) is performed by the service layer.
 * The mapper handles the outbound path: extracting public UUIDs, computing the
 * {@code subjectDisplayName} fallback chain, and computing {@code assessmentExpired}.</p>
 *
 * <p><b>Display name fallback chain:</b>
 * person.externalId.toString() → institution.getName() → subjectNameRaw</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MedicalRecordMapper {

    // ── MedicalRecord → ResponseDto ───────────────────────────────────────────

    @Mapping(source = "externalId",           target = "publicId")
    @Mapping(target = "subjectDisplayName",   expression = "java(resolveSubjectDisplayName(entity))")
    @Mapping(target = "subjectPersonPublicId",      expression = "java(resolvePersonPublicId(entity.getSubjectPerson()))")
    @Mapping(target = "subjectInstitutionPublicId", expression = "java(resolveInstitutionPublicId(entity.getSubjectInstitution()))")
    @Mapping(target = "assessmentExpired",    expression = "java(isAssessmentExpired(entity.getExpiryDate()))")
    MedicalRecordResponseDto toResponse(MedicalRecord entity);

    // ── RequestDto → new Entity ───────────────────────────────────────────────

    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "externalId",            ignore = true)
    @Mapping(target = "subjectPerson",         ignore = true)
    @Mapping(target = "subjectInstitution",    ignore = true)
    @Mapping(target = "ingestedAt",            ignore = true)
    MedicalRecord toEntity(MedicalRecordRequestDto dto);

    // ── RequestDto → partial update ───────────────────────────────────────────

    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "externalId",            ignore = true)
    @Mapping(target = "subjectPerson",         ignore = true)
    @Mapping(target = "subjectInstitution",    ignore = true)
    @Mapping(target = "ingestedAt",            ignore = true)
    void updateEntity(MedicalRecordRequestDto dto, @MappingTarget MedicalRecord entity);

    // ── Helpers ───────────────────────────────────────────────────────────────

    default UUID resolvePersonPublicId(com.projectfaust.person.Person person) {
        return person != null ? person.getExternalId() : null;
    }

    default UUID resolveInstitutionPublicId(com.projectfaust.institution.Institution institution) {
        return institution != null ? institution.getExternalId() : null;
    }

    /**
     * Builds the display name for the subject.
     * Precedence: resolved person public ID → institution name → raw name field.
     * (Person name is intentionally limited to public UUID for HUMINT data
     * security — full name resolution happens at the UI layer with proper
     * clearance enforcement.)
     */
    default String resolveSubjectDisplayName(MedicalRecord entity) {
        if (entity.getSubjectPerson() != null) {
            return entity.getSubjectPerson().getExternalId().toString();
        }
        if (entity.getSubjectInstitution() != null) {
            return entity.getSubjectInstitution().getName();
        }
        return entity.getSubjectNameRaw();
    }

    /**
     * Returns {@code true} if the record has a non-null expiry date that is
     * strictly in the past — indicating e.g. a fitness-for-duty certificate
     * has lapsed.
     */
    default boolean isAssessmentExpired(LocalDate expiryDate) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
}
