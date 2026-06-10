package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionalEvolutionRequest;
import com.projectfaust.dto.response.InstitutionalEvolutionResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.InstitutionalEvolution;
import com.projectfaust.entity.enums.InstitutionalEvolutionType;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-07T11:11:06+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class InstitutionalEvolutionMapperImpl implements InstitutionalEvolutionMapper {

    @Override
    public InstitutionalEvolutionResponse toResponse(InstitutionalEvolution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID externalId = null;
        UUID predecessorExternalId = null;
        String predecessorName = null;
        UUID successorExternalId = null;
        String successorName = null;
        InstitutionalEvolutionType evolutionType = null;
        LocalDate effectiveDate = null;
        String legalBasis = null;
        String description = null;
        OffsetDateTime createdAt = null;

        externalId = entity.getExternalId();
        predecessorExternalId = entityPredecessorExternalId( entity );
        predecessorName = entityPredecessorName( entity );
        successorExternalId = entitySuccessorExternalId( entity );
        successorName = entitySuccessorName( entity );
        evolutionType = entity.getType();
        effectiveDate = entity.getEffectiveDate();
        legalBasis = entity.getLegalBasis();
        description = entity.getDescription();
        createdAt = entity.getCreatedAt();

        InstitutionalEvolutionResponse institutionalEvolutionResponse = new InstitutionalEvolutionResponse( externalId, predecessorExternalId, predecessorName, successorExternalId, successorName, evolutionType, effectiveDate, legalBasis, description, createdAt );

        return institutionalEvolutionResponse;
    }

    @Override
    public List<InstitutionalEvolutionResponse> toResponseList(List<InstitutionalEvolution> entities) {
        if ( entities == null ) {
            return null;
        }

        List<InstitutionalEvolutionResponse> list = new ArrayList<InstitutionalEvolutionResponse>( entities.size() );
        for ( InstitutionalEvolution institutionalEvolution : entities ) {
            list.add( toResponse( institutionalEvolution ) );
        }

        return list;
    }

    @Override
    public InstitutionalEvolution toEntity(InstitutionalEvolutionRequest request) {
        if ( request == null ) {
            return null;
        }

        InstitutionalEvolution.InstitutionalEvolutionBuilder institutionalEvolution = InstitutionalEvolution.builder();

        institutionalEvolution.type( request.evolutionType() );
        institutionalEvolution.effectiveDate( request.effectiveDate() );
        institutionalEvolution.legalBasis( request.legalBasis() );
        institutionalEvolution.description( request.description() );

        return institutionalEvolution.build();
    }

    private UUID entityPredecessorExternalId(InstitutionalEvolution institutionalEvolution) {
        Institution predecessor = institutionalEvolution.getPredecessor();
        if ( predecessor == null ) {
            return null;
        }
        return predecessor.getExternalId();
    }

    private String entityPredecessorName(InstitutionalEvolution institutionalEvolution) {
        Institution predecessor = institutionalEvolution.getPredecessor();
        if ( predecessor == null ) {
            return null;
        }
        return predecessor.getName();
    }

    private UUID entitySuccessorExternalId(InstitutionalEvolution institutionalEvolution) {
        Institution successor = institutionalEvolution.getSuccessor();
        if ( successor == null ) {
            return null;
        }
        return successor.getExternalId();
    }

    private String entitySuccessorName(InstitutionalEvolution institutionalEvolution) {
        Institution successor = institutionalEvolution.getSuccessor();
        if ( successor == null ) {
            return null;
        }
        return successor.getName();
    }
}
