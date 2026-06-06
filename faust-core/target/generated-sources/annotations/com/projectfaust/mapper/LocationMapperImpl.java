package com.projectfaust.mapper;

import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.ClearanceLevel;
import com.projectfaust.entity.enums.LocationType;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-05-07T11:11:06+0200",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.1 (Eclipse Adoptium)"
)
@Component
public class LocationMapperImpl implements LocationMapper {

    @Override
    public LocationResponse toResponse(Location location) {
        if ( location == null ) {
            return null;
        }

        UUID parentExternalId = null;
        String parentName = null;
        UUID externalId = null;
        String name = null;
        LocationType type = null;
        String isoCode = null;
        ClearanceLevel clearanceLevel = null;
        boolean active = false;
        Double latitude = null;
        Double longitude = null;

        parentExternalId = locationParentExternalId( location );
        parentName = locationParentName( location );
        externalId = location.getExternalId();
        name = location.getName();
        type = location.getType();
        isoCode = location.getIsoCode();
        clearanceLevel = location.getClearanceLevel();
        active = location.isActive();
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        boolean hasChildren = location.getChildren() != null && !location.getChildren().isEmpty();

        LocationResponse locationResponse = new LocationResponse( externalId, name, type, isoCode, parentExternalId, parentName, clearanceLevel, active, latitude, longitude, hasChildren );

        return locationResponse;
    }

    @Override
    public Location toEntity(LocationRequest request) {
        if ( request == null ) {
            return null;
        }

        Location.LocationBuilder location = Location.builder();

        location.name( request.name() );
        location.type( request.type() );
        location.isoCode( request.isoCode() );
        location.latitude( request.latitude() );
        location.longitude( request.longitude() );
        location.clearanceLevel( request.clearanceLevel() );

        return location.build();
    }

    private UUID locationParentExternalId(Location location) {
        Location parent = location.getParent();
        if ( parent == null ) {
            return null;
        }
        return parent.getExternalId();
    }

    private String locationParentName(Location location) {
        Location parent = location.getParent();
        if ( parent == null ) {
            return null;
        }
        return parent.getName();
    }
}
