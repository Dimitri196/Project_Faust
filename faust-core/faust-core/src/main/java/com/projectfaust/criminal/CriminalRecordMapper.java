package com.projectfaust.criminal;

import com.projectfaust.criminal.dto.CriminalRecordRequestDto;
import com.projectfaust.criminal.dto.CriminalRecordResponseDto;
import org.mapstruct.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * MapStruct mapper for {@link CriminalRecord} ↔ DTO conversion.
 *
 * <p>The subject FK (person or institution) is resolved and set by the
 * service layer; the mapper only extracts public UUIDs and display names
 * from already-loaded proxies on the outbound path.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CriminalRecordMapper {

    // ── Entity → ResponseDto ──────────────────────────────────────────────────

    @Mapping(source = "externalId",                         target = "publicId")
    @Mapping(target = "subjectPersonPublicId",     expression = "java(resolvePersonPublicId(entity.getSubjectPerson()))")
    @Mapping(target = "subjectInstitutionPublicId",expression = "java(resolveInstitutionPublicId(entity.getSubjectInstitution()))")
    @Mapping(target = "subjectDisplayName",        expression = "java(resolveSubjectDisplayName(entity))")
    @Mapping(target = "recordExpired",             expression = "java(isRecordExpired(entity.getExpiryDate()))")
    CriminalRecordResponseDto toResponse(CriminalRecord entity);

    // ── RequestDto → new Entity ───────────────────────────────────────────────

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "subjectPerson",      ignore = true)
    @Mapping(target = "subjectInstitution", ignore = true)
    @Mapping(target = "ingestedAt",         ignore = true)
    CriminalRecord toEntity(CriminalRecordRequestDto dto);

    // ── RequestDto → partial update ───────────────────────────────────────────

    @Mapping(target = "id",                 ignore = true)
    @Mapping(target = "externalId",         ignore = true)
    @Mapping(target = "subjectPerson",      ignore = true)
    @Mapping(target = "subjectInstitution", ignore = true)
    @Mapping(target = "ingestedAt",         ignore = true)
    void updateEntity(CriminalRecordRequestDto dto, @MappingTarget CriminalRecord entity);

    // ── Helpers ───────────────────────────────────────────────────────────────

    default UUID resolvePersonPublicId(com.projectfaust.person.Person person) {
        return person != null ? person.getExternalId() : null;
    }

    default UUID resolveInstitutionPublicId(com.projectfaust.institution.Institution institution) {
        return institution != null ? institution.getExternalId() : null;
    }

    default String resolveSubjectDisplayName(CriminalRecord entity) {
        if (entity.getSubjectPerson() != null) {
            // Delegate to Person's canonical name; adapt if Person exposes getFullName().
            return entity.getSubjectPerson().getExternalId().toString();
        }
        if (entity.getSubjectInstitution() != null) {
            return entity.getSubjectInstitution().getName();
        }
        return entity.getSubjectNameRaw();
    }

    /** Returns true if the record's legal rehabilitation date has passed. */
    default boolean isRecordExpired(LocalDate expiryDate) {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }
}
