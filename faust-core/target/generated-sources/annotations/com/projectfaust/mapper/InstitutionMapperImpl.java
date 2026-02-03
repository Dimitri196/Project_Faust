package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-02-02T20:38:30+0100",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class InstitutionMapperImpl implements InstitutionMapper {

    @Override
    public Institution toEntity(InstitutionRequest request) {
        if ( request == null ) {
            return null;
        }

        Institution.InstitutionBuilder institution = Institution.builder();

        institution.isStateOwned( request.isStateOwned() );
        institution.name( request.name() );
        institution.countryCode( request.countryCode() );
        institution.level( request.level() );
        institution.type( request.type() );
        institution.description( request.description() );

        return institution.build();
    }

    @Override
    public InstitutionResponse toResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        UUID parentId = null;
        boolean isStateOwned = false;
        String name = null;
        String countryCode = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        String description = null;

        publicId = entity.getExternalId();
        parentId = entityParentExternalId( entity );
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        countryCode = entity.getCountryCode();
        level = entity.getLevel();
        type = entity.getType();
        description = entity.getDescription();

        boolean hasChildren = !entity.getChildren().isEmpty();

        InstitutionResponse institutionResponse = new InstitutionResponse( publicId, name, countryCode, level, type, parentId, hasChildren, isStateOwned, description );

        return institutionResponse;
    }

    @Override
    public List<InstitutionResponse> toResponseList(List<Institution> entities) {
        if ( entities == null ) {
            return null;
        }

        List<InstitutionResponse> list = new ArrayList<InstitutionResponse>( entities.size() );
        for ( Institution institution : entities ) {
            list.add( toResponse( institution ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromRequest(InstitutionRequest request, Institution entity) {
        if ( request == null ) {
            return;
        }

        entity.setName( request.name() );
        entity.setCountryCode( request.countryCode() );
        entity.setLevel( request.level() );
        entity.setType( request.type() );
        entity.setDescription( request.description() );
    }

    @Override
    public InstitutionTreeResponse toTreeResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        List<InstitutionTreeResponse> children = null;
        boolean isStateOwned = false;
        String name = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        String description = null;

        publicId = entity.getExternalId();
        children = toTreeResponseList( entity.getChildren() );
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        description = entity.getDescription();

        InstitutionTreeResponse institutionTreeResponse = new InstitutionTreeResponse( publicId, name, level, type, description, isStateOwned, children );

        return institutionTreeResponse;
    }

    @Override
    public InstitutionAscendedResponse toAscendedResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        InstitutionAscendedResponse parent = null;
        boolean isStateOwned = false;
        String name = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        String description = null;

        publicId = entity.getExternalId();
        parent = toAscendedResponse( entity.getParent() );
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        description = entity.getDescription();

        InstitutionAscendedResponse institutionAscendedResponse = new InstitutionAscendedResponse( publicId, name, level, type, description, isStateOwned, parent );

        return institutionAscendedResponse;
    }

    @Override
    public List<InstitutionTreeResponse> toTreeResponseList(List<Institution> entities) {
        if ( entities == null ) {
            return null;
        }

        List<InstitutionTreeResponse> list = new ArrayList<InstitutionTreeResponse>( entities.size() );
        for ( Institution institution : entities ) {
            list.add( toTreeResponse( institution ) );
        }

        return list;
    }

    private UUID entityParentExternalId(Institution institution) {
        Institution parent = institution.getParent();
        if ( parent == null ) {
            return null;
        }
        return parent.getExternalId();
    }
}
