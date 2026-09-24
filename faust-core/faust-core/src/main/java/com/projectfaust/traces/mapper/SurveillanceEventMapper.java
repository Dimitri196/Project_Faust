package com.projectfaust.traces.mapper;

import com.projectfaust.person.Person;
import com.projectfaust.traces.SurveillanceEvent;
import com.projectfaust.traces.dto.request.SurveillanceEventRequest;
import com.projectfaust.traces.dto.response.BaseTraceResponse;
import com.projectfaust.traces.dto.response.SurveillanceEventResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class SurveillanceEventMapper {

    public SurveillanceEventResponse toResponse(SurveillanceEvent entity) {
        if (entity == null) return null;
        var vr = entity.getVehicleRecord();
        return new SurveillanceEventResponse(
                entity.getExternalId(),
                toBaseResponse(entity),
                vr != null ? vr.getExternalId()   : null,
                vr != null ? vr.getLicensePlate() : null,
                entity.getEventType(),
                entity.getLocationName(),
                entity.getStreetAddress(),
                entity.getCity(),
                entity.getCountry(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.getDescription(),
                entity.getAssetCodename(),
                entity.getOperationName(),
                toParticipantSummaries(entity.getMeetingParticipants()),
                entity.isPhotoEvidence(),
                entity.isAvRecording(),
                entity.getEvidenceReference()
        );
    }

    protected BaseTraceResponse toBaseResponse(SurveillanceEvent e) {
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

    protected List<SurveillanceEventResponse.ParticipantSummary> toParticipantSummaries(
            java.util.Set<Person> participants) {
        if (participants == null) return List.of();
        return participants.stream()
                .map(p -> new SurveillanceEventResponse.ParticipantSummary(
                        p.getExternalId(),
                        p.getFullName()))
                .toList();
    }

    public SurveillanceEvent toEntity(SurveillanceEventRequest req) {
        if (req == null) return null;
        return SurveillanceEvent.builder()
                .observedAt(req.observedAt())
                .sourceType(req.sourceType())
                .sourceReference(req.sourceReference())
                .confidence(req.confidence() != null ? req.confidence() :
                        com.projectfaust.traces.enums.TraceConfidence.UNCONFIRMED)
                .verificationStatus(req.verificationStatus() != null ? req.verificationStatus() :
                        com.projectfaust.shared.enums.VerificationStatus.PENDING_REVIEW)
                .flagged(req.flagged())
                .analyticalNote(req.analyticalNote())
                .eventType(req.eventType())
                .locationName(req.locationName())
                .streetAddress(req.streetAddress())
                .city(req.city())
                .country(req.country())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .description(req.description())
                .assetCodename(req.assetCodename())
                .operationName(req.operationName())
                .photoEvidence(req.photoEvidence())
                .avRecording(req.avRecording())
                .evidenceReference(req.evidenceReference())
                .build();
    }

    public void updateEntity(SurveillanceEventRequest req, @MappingTarget SurveillanceEvent entity) {
        if (req == null) return;
        if (req.observedAt()         != null) entity.setObservedAt(req.observedAt());
        if (req.sourceType()         != null) entity.setSourceType(req.sourceType());
        if (req.sourceReference()    != null) entity.setSourceReference(req.sourceReference());
        if (req.confidence()         != null) entity.setConfidence(req.confidence());
        if (req.verificationStatus() != null) entity.setVerificationStatus(req.verificationStatus());
        entity.setFlagged(req.flagged());
        if (req.analyticalNote()     != null) entity.setAnalyticalNote(req.analyticalNote());
        if (req.eventType()          != null) entity.setEventType(req.eventType());
        if (req.locationName()       != null) entity.setLocationName(req.locationName());
        if (req.streetAddress()      != null) entity.setStreetAddress(req.streetAddress());
        if (req.city()               != null) entity.setCity(req.city());
        if (req.country()            != null) entity.setCountry(req.country());
        if (req.latitude()           != null) entity.setLatitude(req.latitude());
        if (req.longitude()          != null) entity.setLongitude(req.longitude());
        if (req.description()        != null) entity.setDescription(req.description());
        if (req.assetCodename()      != null) entity.setAssetCodename(req.assetCodename());
        if (req.operationName()      != null) entity.setOperationName(req.operationName());
        entity.setPhotoEvidence(req.photoEvidence());
        entity.setAvRecording(req.avRecording());
        if (req.evidenceReference()  != null) entity.setEvidenceReference(req.evidenceReference());
    }
}
