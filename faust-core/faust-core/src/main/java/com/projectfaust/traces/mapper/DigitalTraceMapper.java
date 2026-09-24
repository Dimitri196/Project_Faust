package com.projectfaust.traces.mapper;

import com.projectfaust.traces.DigitalTrace;
import com.projectfaust.traces.dto.request.DigitalTraceRequest;
import com.projectfaust.traces.dto.response.BaseTraceResponse;
import com.projectfaust.traces.dto.response.DigitalTraceResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class DigitalTraceMapper {

    // ── Entity → Response ─────────────────────────────────────────────────────

    public DigitalTraceResponse toResponse(DigitalTrace entity) {
        if (entity == null) return null;
        return new DigitalTraceResponse(
                entity.getExternalId(),
                toBaseResponse(entity),
                entity.getServiceName(),
                entity.getAccountIdentifier(),
                entity.getEventType(),
                entity.getIpAddress(),
                entity.getAsn(),
                entity.getIspName(),
                entity.getGeoCountry(),
                entity.getGeoCity(),
                entity.getUserAgent(),
                entity.getDeviceFingerprint(),
                entity.getOperatingSystem(),
                entity.isTorExitNode(),
                entity.isVpnDetected(),
                entity.isDatacenterIp()
        );
    }

    protected BaseTraceResponse toBaseResponse(DigitalTrace e) {
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

    // ── Request → Entity (for creation; no person/id set here) ───────────────

    public DigitalTrace toEntity(DigitalTraceRequest request) {
        if (request == null) return null;
        return DigitalTrace.builder()
                .observedAt(request.observedAt())
                .sourceType(request.sourceType())
                .sourceReference(request.sourceReference())
                .confidence(request.confidence() != null ? request.confidence() :
                        com.projectfaust.traces.enums.TraceConfidence.UNCONFIRMED)
                .verificationStatus(request.verificationStatus() != null ? request.verificationStatus() :
                        com.projectfaust.shared.enums.VerificationStatus.PENDING_REVIEW)
                .flagged(request.flagged())
                .analyticalNote(request.analyticalNote())
                .serviceName(request.serviceName())
                .accountIdentifier(request.accountIdentifier())
                .eventType(request.eventType())
                .ipAddress(request.ipAddress())
                .asn(request.asn())
                .ispName(request.ispName())
                .geoCountry(request.geoCountry())
                .geoCity(request.geoCity())
                .userAgent(request.userAgent())
                .deviceFingerprint(request.deviceFingerprint())
                .operatingSystem(request.operatingSystem())
                .torExitNode(request.torExitNode())
                .vpnDetected(request.vpnDetected())
                .datacenterIp(request.datacenterIp())
                .build();
    }

    // ── Request → Entity (update — mutable fields only) ──────────────────────

    public void updateEntity(DigitalTraceRequest request, @MappingTarget DigitalTrace entity) {
        if (request == null) return;
        if (request.observedAt()        != null) entity.setObservedAt(request.observedAt());
        if (request.sourceType()        != null) entity.setSourceType(request.sourceType());
        if (request.sourceReference()   != null) entity.setSourceReference(request.sourceReference());
        if (request.confidence()        != null) entity.setConfidence(request.confidence());
        if (request.verificationStatus()!= null) entity.setVerificationStatus(request.verificationStatus());
        entity.setFlagged(request.flagged());
        if (request.analyticalNote()    != null) entity.setAnalyticalNote(request.analyticalNote());
        if (request.serviceName()       != null) entity.setServiceName(request.serviceName());
        if (request.accountIdentifier() != null) entity.setAccountIdentifier(request.accountIdentifier());
        if (request.eventType()         != null) entity.setEventType(request.eventType());
        if (request.ipAddress()         != null) entity.setIpAddress(request.ipAddress());
        if (request.asn()               != null) entity.setAsn(request.asn());
        if (request.ispName()           != null) entity.setIspName(request.ispName());
        if (request.geoCountry()        != null) entity.setGeoCountry(request.geoCountry());
        if (request.geoCity()           != null) entity.setGeoCity(request.geoCity());
        if (request.userAgent()         != null) entity.setUserAgent(request.userAgent());
        if (request.deviceFingerprint() != null) entity.setDeviceFingerprint(request.deviceFingerprint());
        if (request.operatingSystem()   != null) entity.setOperatingSystem(request.operatingSystem());
        entity.setTorExitNode(request.torExitNode());
        entity.setVpnDetected(request.vpnDetected());
        entity.setDatacenterIp(request.datacenterIp());
    }
}
