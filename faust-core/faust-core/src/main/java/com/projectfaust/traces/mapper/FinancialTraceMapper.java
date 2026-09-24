package com.projectfaust.traces.mapper;

import com.projectfaust.traces.FinancialTrace;
import com.projectfaust.traces.dto.request.FinancialTraceRequest;
import com.projectfaust.traces.dto.response.BaseTraceResponse;
import com.projectfaust.traces.dto.response.FinancialTraceResponse;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public abstract class FinancialTraceMapper {

    public FinancialTraceResponse toResponse(FinancialTrace entity) {
        if (entity == null) return null;
        return new FinancialTraceResponse(
                entity.getExternalId(),
                toBaseResponse(entity),
                entity.getBankAccount() != null ? entity.getBankAccount().getExternalId() : null,
                entity.getCardNumberMasked(),
                entity.getIbanRaw(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getTransactionType(),
                entity.getAuthorizationCode(),
                entity.getMerchantName(),
                entity.getMerchantCategoryCode(),
                entity.getTerminalId(),
                entity.getMerchantAddress(),
                entity.getMerchantCity(),
                entity.getMerchantCountry(),
                entity.getLatitude(),
                entity.getLongitude(),
                entity.isAmlFlagged(),
                entity.getAmlReason()
        );
    }

    protected BaseTraceResponse toBaseResponse(FinancialTrace e) {
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

    public FinancialTrace toEntity(FinancialTraceRequest req) {
        if (req == null) return null;
        return FinancialTrace.builder()
                .observedAt(req.observedAt())
                .sourceType(req.sourceType())
                .sourceReference(req.sourceReference())
                .confidence(req.confidence() != null ? req.confidence() :
                        com.projectfaust.traces.enums.TraceConfidence.UNCONFIRMED)
                .verificationStatus(req.verificationStatus() != null ? req.verificationStatus() :
                        com.projectfaust.shared.enums.VerificationStatus.PENDING_REVIEW)
                .flagged(req.flagged())
                .analyticalNote(req.analyticalNote())
                .cardNumberMasked(req.cardNumberMasked())
                .ibanRaw(req.ibanRaw())
                .amount(req.amount())
                .currency(req.currency())
                .transactionType(req.transactionType())
                .authorizationCode(req.authorizationCode())
                .merchantName(req.merchantName())
                .merchantCategoryCode(req.merchantCategoryCode())
                .terminalId(req.terminalId())
                .merchantAddress(req.merchantAddress())
                .merchantCity(req.merchantCity())
                .merchantCountry(req.merchantCountry())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .amlFlagged(req.amlFlagged())
                .amlReason(req.amlReason())
                .build();
    }

    public void updateEntity(FinancialTraceRequest req, @MappingTarget FinancialTrace entity) {
        if (req == null) return;
        if (req.observedAt()         != null) entity.setObservedAt(req.observedAt());
        if (req.sourceType()         != null) entity.setSourceType(req.sourceType());
        if (req.sourceReference()    != null) entity.setSourceReference(req.sourceReference());
        if (req.confidence()         != null) entity.setConfidence(req.confidence());
        if (req.verificationStatus() != null) entity.setVerificationStatus(req.verificationStatus());
        entity.setFlagged(req.flagged());
        if (req.analyticalNote()     != null) entity.setAnalyticalNote(req.analyticalNote());
        if (req.cardNumberMasked()   != null) entity.setCardNumberMasked(req.cardNumberMasked());
        if (req.ibanRaw()            != null) entity.setIbanRaw(req.ibanRaw());
        if (req.amount()             != null) entity.setAmount(req.amount());
        if (req.currency()           != null) entity.setCurrency(req.currency());
        if (req.transactionType()    != null) entity.setTransactionType(req.transactionType());
        if (req.authorizationCode()  != null) entity.setAuthorizationCode(req.authorizationCode());
        if (req.merchantName()       != null) entity.setMerchantName(req.merchantName());
        if (req.merchantCategoryCode()!= null) entity.setMerchantCategoryCode(req.merchantCategoryCode());
        if (req.terminalId()         != null) entity.setTerminalId(req.terminalId());
        if (req.merchantAddress()    != null) entity.setMerchantAddress(req.merchantAddress());
        if (req.merchantCity()       != null) entity.setMerchantCity(req.merchantCity());
        if (req.merchantCountry()    != null) entity.setMerchantCountry(req.merchantCountry());
        if (req.latitude()           != null) entity.setLatitude(req.latitude());
        if (req.longitude()          != null) entity.setLongitude(req.longitude());
        entity.setAmlFlagged(req.amlFlagged());
        if (req.amlReason()          != null) entity.setAmlReason(req.amlReason());
    }
}
