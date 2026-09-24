package com.projectfaust.court;

import com.projectfaust.court.dto.CourtRecordRequestDto;
import com.projectfaust.court.dto.CourtRecordResponseDto;
import com.projectfaust.court.dto.CourtRecordResponseDto.PartyResponseDto;
import org.mapstruct.*;

import java.util.UUID;

/**
 * MapStruct mapper for {@link CourtRecord} and {@link CourtRecordParty} ↔ DTO.
 *
 * <p>Party FK resolution (person / institution) is handled by the service layer.
 * The mapper handles the outbound path only: extracting public UUIDs and building
 * display names from already-loaded proxies.</p>
 *
 * @author Dimitri / Project Faust
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CourtRecordMapper {

    // ── CourtRecord → ResponseDto ─────────────────────────────────────────────

    @Mapping(source = "externalId", target = "publicId")
    @Mapping(target = "linkedCriminalRecordPublicId",
             expression = "java(entity.getLinkedCriminalRecord() != null ? entity.getLinkedCriminalRecord().getExternalId() : null)")
    CourtRecordResponseDto toResponse(CourtRecord entity);

    // ── RequestDto → new Entity ───────────────────────────────────────────────

    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "externalId",            ignore = true)
    @Mapping(target = "parties",               ignore = true)
    @Mapping(target = "linkedCriminalRecord",  ignore = true)
    @Mapping(target = "ingestedAt",            ignore = true)
    CourtRecord toEntity(CourtRecordRequestDto dto);

    // ── RequestDto → partial update ───────────────────────────────────────────

    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "externalId",            ignore = true)
    @Mapping(target = "parties",               ignore = true)
    @Mapping(target = "linkedCriminalRecord",  ignore = true)
    @Mapping(target = "ingestedAt",            ignore = true)
    void updateEntity(CourtRecordRequestDto dto, @MappingTarget CourtRecord entity);

    // ── CourtRecordParty → PartyResponseDto ──────────────────────────────────

    @Mapping(source = "externalId",                target = "publicId")
    @Mapping(target = "personPublicId",      expression = "java(resolvePersonPublicId(p.getPerson()))")
    @Mapping(target = "institutionPublicId", expression = "java(resolveInstitutionPublicId(p.getInstitution()))")
    @Mapping(target = "displayName",         expression = "java(resolveDisplayName(p))")
    PartyResponseDto toPartyResponse(CourtRecordParty p);

    // ── Helpers ───────────────────────────────────────────────────────────────

    default UUID resolvePersonPublicId(com.projectfaust.person.Person person) {
        return person != null ? person.getExternalId() : null;
    }

    default UUID resolveInstitutionPublicId(com.projectfaust.institution.Institution institution) {
        return institution != null ? institution.getExternalId() : null;
    }

    default String resolveDisplayName(CourtRecordParty p) {
        if (p.getPerson() != null) {
            // Adapt if Person exposes getFullName().
            return p.getPerson().getExternalId().toString();
        }
        if (p.getInstitution() != null) {
            return p.getInstitution().getName();
        }
        return p.getPartyNameRaw();
    }
}
