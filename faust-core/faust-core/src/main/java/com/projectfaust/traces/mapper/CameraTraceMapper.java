package com.projectfaust.traces.mapper;

import com.projectfaust.traces.CameraTrace;
import com.projectfaust.traces.dto.request.CameraTraceRequest;
import com.projectfaust.traces.dto.response.BaseTraceResponse;
import com.projectfaust.traces.dto.response.CameraTraceResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class CameraTraceMapper {

    public CameraTraceResponse toResponse(CameraTrace entity) {
        if (entity == null) return null;
        var vr = entity.getVehicleRecord();
        return new CameraTraceResponse(
                entity.getExternalId(),
                toBaseResponse(entity),
                vr != null ? vr.getExternalId()    : null,
                vr != null ? vr.getLicensePlate()  : null,
                vr != null ? vr.getMake() + " " + vr.getModel() : null,
                entity.getCameraId(),
                entity.getCameraOperator(),
                entity.getLocationName(),
                entity.getStreetAddress(),
                entity.getCity(),
                entity.getCountry(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getFacialMatchScore(),
                entity.getAnprPlateRaw(),
                entity.getAnprPlateCountry(),
                entity.getDirectionOfTravel(),
                entity.getFootageReference(),
                entity.isFootageArchived()
        );
    }

    protected BaseTraceResponse toBaseResponse(CameraTrace e) {
        return new BaseTraceResponse(
                e.getExternalId(),
                e.getPerson() != null ? e.getPerson().getExternalId() : null,
                e.getPerson() != null ? e.getPerson().getFullName()   : null,
                e.getObservedAt(),
                e.getSourceType(),
                e.getSourceReference(),
                e.getConfidence(),
                e.getVerificationStatus(),
                e.isFlagged(),
                e.getAnalyticalNote(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public CameraTrace toEntity(CameraTraceRequest req) {
        if (req == null) return null;
        return CameraTrace.builder()
                .observedAt(req.observedAt())
                .sourceType(req.sourceType())
                .sourceReference(req.sourceReference())
                .confidence(req.confidence() != null ? req.confidence() :
                        com.projectfaust.traces.enums.TraceConfidence.UNCONFIRMED)
                .verificationStatus(req.verificationStatus() != null ? req.verificationStatus() :
                        com.projectfaust.shared.enums.VerificationStatus.PENDING_REVIEW)
                .flagged(req.flagged())
                .analyticalNote(req.analyticalNote())
                .cameraId(req.cameraId())
                .cameraOperator(req.cameraOperator())
                .locationName(req.locationName())
                .streetAddress(req.streetAddress())
                .city(req.city())
                .country(req.country())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .facialMatchScore(req.facialMatchScore())
                .anprPlateRaw(req.anprPlateRaw())
                .anprPlateCountry(req.anprPlateCountry())
                .directionOfTravel(req.directionOfTravel())
                .footageReference(req.footageReference())
                .footageArchived(req.footageArchived())
                .build();
    }

    public void updateEntity(CameraTraceRequest req, @MappingTarget CameraTrace entity) {
        if (req == null) return;
        if (req.observedAt()         != null) entity.setObservedAt(req.observedAt());
        if (req.sourceType()         != null) entity.setSourceType(req.sourceType());
        if (req.sourceReference()    != null) entity.setSourceReference(req.sourceReference());
        if (req.confidence()         != null) entity.setConfidence(req.confidence());
        if (req.verificationStatus() != null) entity.setVerificationStatus(req.verificationStatus());
        entity.setFlagged(req.flagged());
        if (req.analyticalNote()     != null) entity.setAnalyticalNote(req.analyticalNote());
        if (req.cameraId()           != null) entity.setCameraId(req.cameraId());
        if (req.cameraOperator()     != null) entity.setCameraOperator(req.cameraOperator());
        if (req.locationName()       != null) entity.setLocationName(req.locationName());
        if (req.streetAddress()      != null) entity.setStreetAddress(req.streetAddress());
        if (req.city()               != null) entity.setCity(req.city());
        if (req.country()            != null) entity.setCountry(req.country());
        if (req.latitude()           != null) entity.setLatitude(req.latitude());
        if (req.longitude()          != null) entity.setLongitude(req.longitude());
        if (req.facialMatchScore()   != null) entity.setFacialMatchScore(req.facialMatchScore());
        if (req.anprPlateRaw()       != null) entity.setAnprPlateRaw(req.anprPlateRaw());
        if (req.anprPlateCountry()   != null) entity.setAnprPlateCountry(req.anprPlateCountry());
        if (req.directionOfTravel()  != null) entity.setDirectionOfTravel(req.directionOfTravel());
        if (req.footageReference()   != null) entity.setFootageReference(req.footageReference());
        entity.setFootageArchived(req.footageArchived());
    }
}
