package com.projectfaust.traces.mapper;

import com.projectfaust.traces.TelcoTrace;
import com.projectfaust.traces.dto.request.TelcoTraceRequest;
import com.projectfaust.traces.dto.response.BaseTraceResponse;
import com.projectfaust.traces.dto.response.TelcoTraceResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class TelcoTraceMapper {

    public TelcoTraceResponse toResponse(TelcoTrace entity) {
        if (entity == null) return null;
        return new TelcoTraceResponse(
                entity.getExternalId(),
                toBaseResponse(entity),
                entity.getImsi(),
                entity.getImei(),
                entity.getMsisdn(),
                entity.getOperatorCode(),
                entity.getOperatorName(),
                entity.getRatType(),
                entity.getCellId(),
                entity.getAreaCode(),
                entity.getCellLatitude(),
                entity.getCellLongitude(),
                entity.getCellRadiusMeters(),
                entity.getCellAddress(),
                entity.getCountry(),
                entity.getCity(),
                entity.getRssiDbm(),
                entity.getEventType(),
                entity.isActiveIntercept(),
                entity.getDurationSeconds()
        );
    }

    protected BaseTraceResponse toBaseResponse(TelcoTrace e) {
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

    public TelcoTrace toEntity(TelcoTraceRequest req) {
        if (req == null) return null;
        return TelcoTrace.builder()
                .observedAt(req.observedAt())
                .sourceType(req.sourceType())
                .sourceReference(req.sourceReference())
                .confidence(req.confidence() != null ? req.confidence() :
                        com.projectfaust.traces.enums.TraceConfidence.UNCONFIRMED)
                .verificationStatus(req.verificationStatus() != null ? req.verificationStatus() :
                        com.projectfaust.shared.enums.VerificationStatus.PENDING_REVIEW)
                .flagged(req.flagged())
                .analyticalNote(req.analyticalNote())
                .imsi(req.imsi())
                .imei(req.imei())
                .msisdn(req.msisdn())
                .operatorCode(req.operatorCode())
                .operatorName(req.operatorName())
                .ratType(req.ratType())
                .cellId(req.cellId())
                .areaCode(req.areaCode())
                .cellLatitude(req.cellLatitude())
                .cellLongitude(req.cellLongitude())
                .cellRadiusMeters(req.cellRadiusMeters())
                .cellAddress(req.cellAddress())
                .country(req.country())
                .city(req.city())
                .rssiDbm(req.rssiDbm())
                .eventType(req.eventType())
                .activeIntercept(req.activeIntercept())
                .durationSeconds(req.durationSeconds())
                .build();
    }

    public void updateEntity(TelcoTraceRequest req, @MappingTarget TelcoTrace entity) {
        if (req == null) return;
        if (req.observedAt()         != null) entity.setObservedAt(req.observedAt());
        if (req.sourceType()         != null) entity.setSourceType(req.sourceType());
        if (req.sourceReference()    != null) entity.setSourceReference(req.sourceReference());
        if (req.confidence()         != null) entity.setConfidence(req.confidence());
        if (req.verificationStatus() != null) entity.setVerificationStatus(req.verificationStatus());
        entity.setFlagged(req.flagged());
        if (req.analyticalNote()     != null) entity.setAnalyticalNote(req.analyticalNote());
        if (req.imsi()               != null) entity.setImsi(req.imsi());
        if (req.imei()               != null) entity.setImei(req.imei());
        if (req.msisdn()             != null) entity.setMsisdn(req.msisdn());
        if (req.operatorCode()       != null) entity.setOperatorCode(req.operatorCode());
        if (req.operatorName()       != null) entity.setOperatorName(req.operatorName());
        if (req.ratType()            != null) entity.setRatType(req.ratType());
        if (req.cellId()             != null) entity.setCellId(req.cellId());
        if (req.areaCode()           != null) entity.setAreaCode(req.areaCode());
        if (req.cellLatitude()       != null) entity.setCellLatitude(req.cellLatitude());
        if (req.cellLongitude()      != null) entity.setCellLongitude(req.cellLongitude());
        if (req.cellRadiusMeters()   != null) entity.setCellRadiusMeters(req.cellRadiusMeters());
        if (req.cellAddress()        != null) entity.setCellAddress(req.cellAddress());
        if (req.country()            != null) entity.setCountry(req.country());
        if (req.city()               != null) entity.setCity(req.city());
        if (req.rssiDbm()            != null) entity.setRssiDbm(req.rssiDbm());
        if (req.eventType()          != null) entity.setEventType(req.eventType());
        entity.setActiveIntercept(req.activeIntercept());
        if (req.durationSeconds()    != null) entity.setDurationSeconds(req.durationSeconds());
    }
}
