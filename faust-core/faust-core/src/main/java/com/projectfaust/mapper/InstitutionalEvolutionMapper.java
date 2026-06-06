package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionalEvolutionRequest;
import com.projectfaust.dto.response.InstitutionalEvolutionResponse;
import com.projectfaust.entity.InstitutionalEvolution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = { java.util.Collections.class }
)
public interface InstitutionalEvolutionMapper {

    @Mapping(source = "externalId", target = "externalId")
    @Mapping(source = "predecessor.externalId", target = "predecessorExternalId")
    @Mapping(source = "predecessor.name", target = "predecessorName")
    @Mapping(source = "successor.externalId", target = "successorExternalId")
    @Mapping(source = "successor.name", target = "successorName")
    @Mapping(source = "type", target = "evolutionType")
    @Mapping(source = "effectiveDate", target = "effectiveDate")
    InstitutionalEvolutionResponse toResponse(InstitutionalEvolution entity);

    List<InstitutionalEvolutionResponse> toResponseList(List<InstitutionalEvolution> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "externalId", ignore = true)
    @Mapping(target = "predecessor", ignore = true)
    @Mapping(target = "successor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(source = "evolutionType", target = "type")
    @Mapping(source = "effectiveDate", target = "effectiveDate")
    InstitutionalEvolution toEntity(InstitutionalEvolutionRequest request);
}