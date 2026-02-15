package com.projectfaust.mapper;

import com.projectfaust.dto.request.OccupationRequest;
import com.projectfaust.dto.response.OccupationResponse;
import com.projectfaust.dto.response.OccupationTreeResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.Occupation;
import com.projectfaust.entity.enums.OccupationCategory;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-11T19:01:07+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class OccupationMapperImpl implements OccupationMapper {

    @Override
    public Occupation toEntity(OccupationRequest request) {
        if ( request == null ) {
            return null;
        }

        Occupation.OccupationBuilder occupation = Occupation.builder();

        occupation.title( request.title() );
        occupation.code( request.code() );
        occupation.category( request.category() );
        occupation.isVacant( request.isVacant() );
        occupation.rank( request.rank() );
        occupation.description( request.description() );

        return occupation.build();
    }

    @Override
    public OccupationResponse toResponse(Occupation entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        String institutionName = null;
        UUID institutionPublicId = null;
        String supervisorTitle = null;
        UUID reportsToPublicId = null;
        String title = null;
        String code = null;
        OccupationCategory category = null;
        String rank = null;
        String description = null;

        publicId = entity.getExternalId();
        institutionName = entityInstitutionName( entity );
        institutionPublicId = entityInstitutionExternalId( entity );
        supervisorTitle = entityReportsToTitle( entity );
        reportsToPublicId = entityReportsToExternalId( entity );
        title = entity.getTitle();
        code = entity.getCode();
        category = entity.getCategory();
        rank = entity.getRank();
        description = entity.getDescription();

        boolean isVacant = false;

        OccupationResponse occupationResponse = new OccupationResponse( publicId, title, code, category, institutionName, institutionPublicId, supervisorTitle, reportsToPublicId, isVacant, rank, description );

        return occupationResponse;
    }

    @Override
    public List<OccupationResponse> toResponseList(List<Occupation> entities) {
        if ( entities == null ) {
            return null;
        }

        List<OccupationResponse> list = new ArrayList<OccupationResponse>( entities.size() );
        for ( Occupation occupation : entities ) {
            list.add( toResponse( occupation ) );
        }

        return list;
    }

    @Override
    public OccupationTreeResponse toTreeResponse(Occupation entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        List<OccupationTreeResponse> subordinates = null;
        String title = null;
        String code = null;
        OccupationCategory category = null;
        String rank = null;

        publicId = entity.getExternalId();
        subordinates = toTreeResponseList( entity.getSubordinates() );
        title = entity.getTitle();
        code = entity.getCode();
        category = entity.getCategory();
        rank = entity.getRank();

        String currentOccupantName = mapCurrentOccupant(entity);
        boolean isVacant = false;

        OccupationTreeResponse occupationTreeResponse = new OccupationTreeResponse( publicId, title, code, category, isVacant, rank, currentOccupantName, subordinates );

        return occupationTreeResponse;
    }

    @Override
    public List<OccupationTreeResponse> toTreeResponseList(List<Occupation> entities) {
        if ( entities == null ) {
            return null;
        }

        List<OccupationTreeResponse> list = new ArrayList<OccupationTreeResponse>( entities.size() );
        for ( Occupation occupation : entities ) {
            list.add( toTreeResponse( occupation ) );
        }

        return list;
    }

    private String entityInstitutionName(Occupation occupation) {
        Institution institution = occupation.getInstitution();
        if ( institution == null ) {
            return null;
        }
        return institution.getName();
    }

    private UUID entityInstitutionExternalId(Occupation occupation) {
        Institution institution = occupation.getInstitution();
        if ( institution == null ) {
            return null;
        }
        return institution.getExternalId();
    }

    private String entityReportsToTitle(Occupation occupation) {
        Occupation reportsTo = occupation.getReportsTo();
        if ( reportsTo == null ) {
            return null;
        }
        return reportsTo.getTitle();
    }

    private UUID entityReportsToExternalId(Occupation occupation) {
        Occupation reportsTo = occupation.getReportsTo();
        if ( reportsTo == null ) {
            return null;
        }
        return reportsTo.getExternalId();
    }
}
