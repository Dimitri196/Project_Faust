package com.projectfaust.mapper;

import com.projectfaust.dto.request.InstitutionRequest;
import com.projectfaust.dto.response.InstitutionAscendedResponse;
import com.projectfaust.dto.response.InstitutionResponse;
import com.projectfaust.dto.response.InstitutionTreeResponse;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Institution;
import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.HierarchicalLevel;
import com.projectfaust.entity.enums.InstitutionType;
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
public class InstitutionMapperImpl implements InstitutionMapper {

    @Override
    public InstitutionResponse toResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        UUID parentId = null;
        UUID locationId = null;
        String locationName = null;
        boolean isStateOwned = false;
        String name = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        ClearanceLevel clearanceLevel = null;
        boolean active = false;
        String description = null;
        String logoUrl = null;
        String websiteUrl = null;

        publicId = entity.getExternalId();
        parentId = entityParentExternalId( entity );
        locationId = entityLocationExternalId( entity );
        locationName = entityLocationName( entity );
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        clearanceLevel = entity.getClearanceLevel();
        active = entity.isActive();
        description = entity.getDescription();
        logoUrl = entity.getLogoUrl();
        websiteUrl = entity.getWebsiteUrl();

        boolean hasChildren = !entity.getChildren().isEmpty();
        List<LocationResponse> fullLocationPath = null;

        InstitutionResponse institutionResponse = new InstitutionResponse( publicId, name, level, type, clearanceLevel, parentId, hasChildren, isStateOwned, active, description, locationId, locationName, fullLocationPath, logoUrl, websiteUrl );

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
    public Institution toEntity(InstitutionRequest request) {
        if ( request == null ) {
            return null;
        }

        Institution.InstitutionBuilder institution = Institution.builder();

        institution.name( request.name() );
        institution.level( request.level() );
        institution.type( request.type() );
        institution.clearanceLevel( request.clearanceLevel() );
        institution.isStateOwned( request.isStateOwned() );
        institution.active( request.active() );
        institution.description( request.description() );
        institution.logoUrl( request.logoUrl() );
        institution.websiteUrl( request.websiteUrl() );

        return institution.build();
    }

    @Override
    public InstitutionTreeResponse toTreeResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        boolean isStateOwned = false;
        String logoUrl = null;
        List<InstitutionTreeResponse> children = null;
        String name = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        ClearanceLevel clearanceLevel = null;
        String description = null;
        boolean active = false;

        publicId = entity.getExternalId();
        isStateOwned = entity.isStateOwned();
        logoUrl = entity.getLogoUrl();
        children = toTreeResponseListDeep( entity.getChildren() );
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        clearanceLevel = entity.getClearanceLevel();
        description = entity.getDescription();
        active = entity.isActive();

        boolean hasChildren = !entity.getChildren().isEmpty();
        InstitutionTreeResponse parent = null;

        InstitutionTreeResponse institutionTreeResponse = new InstitutionTreeResponse( publicId, name, level, type, clearanceLevel, description, isStateOwned, active, hasChildren, parent, children, logoUrl );

        return institutionTreeResponse;
    }

    @Override
    public InstitutionTreeResponse toFlatTreeResponse(Institution entity) {
        if ( entity == null ) {
            return null;
        }

        UUID publicId = null;
        String logoUrl = null;
        boolean isStateOwned = false;
        String name = null;
        HierarchicalLevel level = null;
        InstitutionType type = null;
        ClearanceLevel clearanceLevel = null;
        String description = null;
        boolean active = false;

        publicId = entity.getExternalId();
        logoUrl = entity.getLogoUrl();
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        clearanceLevel = entity.getClearanceLevel();
        description = entity.getDescription();
        active = entity.isActive();

        List<InstitutionTreeResponse> children = null;
        InstitutionTreeResponse parent = null;
        boolean hasChildren = !entity.getChildren().isEmpty();

        InstitutionTreeResponse institutionTreeResponse = new InstitutionTreeResponse( publicId, name, level, type, clearanceLevel, description, isStateOwned, active, hasChildren, parent, children, logoUrl );

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
        ClearanceLevel clearanceLevel = null;
        String description = null;
        boolean active = false;

        publicId = entity.getExternalId();
        parent = toAscendedResponse( entity.getParent() );
        isStateOwned = entity.isStateOwned();
        name = entity.getName();
        level = entity.getLevel();
        type = entity.getType();
        clearanceLevel = entity.getClearanceLevel();
        description = entity.getDescription();
        active = entity.isActive();

        InstitutionAscendedResponse institutionAscendedResponse = new InstitutionAscendedResponse( publicId, name, level, type, clearanceLevel, description, isStateOwned, active, parent );

        return institutionAscendedResponse;
    }

    private UUID entityParentExternalId(Institution institution) {
        Institution parent = institution.getParent();
        if ( parent == null ) {
            return null;
        }
        return parent.getExternalId();
    }

    private UUID entityLocationExternalId(Institution institution) {
        Location location = institution.getLocation();
        if ( location == null ) {
            return null;
        }
        return location.getExternalId();
    }

    private String entityLocationName(Institution institution) {
        Location location = institution.getLocation();
        if ( location == null ) {
            return null;
        }
        return location.getName();
    }
}
