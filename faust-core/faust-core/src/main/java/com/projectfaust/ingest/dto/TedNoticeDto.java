package com.projectfaust.ingest.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Raw response DTO for a single notice from the TED (Tenders Electronic Daily)
 * API v3 — the EU's official public procurement journal.
 *
 * <p>TED notices follow the eForms standard (since November 2022). This DTO
 * covers the subset of fields requested via the {@code fields} parameter in
 * the search request. Fields not requested will be null — the mapper handles
 * all nulls defensively.</p>
 *
 * <p>Unlike {@link HlidacContractDto}, TED uses structured nested objects
 * for buyer and award data rather than flat fields with aliases. Buyer is
 * always present on a notice; awarded supplier appears only on Contract Award
 * Notices (notice-type=can-standard), not on Contract Notices
 * (notice-type=cn-standard).</p>
 *
 * <p>This DTO is consumed exclusively by
 * {@link com.projectfaust.ingest.ted.TedContractMapper} — no other part of
 * the system should reference TED field names directly.</p>
 *
 * @author Dimitri / Project Faust
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TedNoticeDto(

        @JsonProperty("publication-number")
        String publicationNumber,

        @JsonProperty("publication-date")
        String publicationDate,

        @JsonProperty("notice-type")
        String noticeType,

        @JsonProperty("buyer")
        List<BuyerDto> buyers,

        @JsonProperty("award-outcome")
        List<AwardOutcomeDto> awardOutcomes,

        @JsonProperty("notice-value")
        NoticeValueDto noticeValue,

        @JsonProperty("short-description")
        String shortDescription,

        @JsonProperty("main-cpv")
        String mainCpv

) {
    /**
     * Contracting authority (buyer) on a TED notice.
     *
     * @param name      official name of the contracting authority.
     * @param country   ISO 3166-1 alpha-2 country code (e.g. "CZE" — TED uses alpha-3).
     * @param nationalId the buyer's national registration number (IČO for CZ).
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BuyerDto(
            @JsonProperty("official-name") String name,
            @JsonProperty("buyer-country") String country,
            @JsonProperty("national-id")   String nationalId
    ) {}

    /**
     * Award outcome — present on Contract Award Notices.
     * Contains the winning supplier and awarded value.
     *
     * @param suppliers  list of awarded suppliers (usually one per lot).
     * @param awardedValue the final awarded contract value.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AwardOutcomeDto(
            @JsonProperty("awarded-suppliers") List<SupplierDto> suppliers,
            @JsonProperty("awarded-value")     NoticeValueDto awardedValue
    ) {}

    /**
     * Awarded supplier on a Contract Award Notice.
     *
     * @param name      official name of the supplier.
     * @param nationalId the supplier's national registration number.
     * @param country   ISO 3166-1 alpha-2 country code.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SupplierDto(
            @JsonProperty("official-name") String name,
            @JsonProperty("national-id")   String nationalId,
            @JsonProperty("country")       String country
    ) {}

    /**
     * Monetary value — amount with currency code.
     *
     * @param amount   the numeric value.
     * @param currency ISO 4217 currency code (e.g. "CZK", "EUR").
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NoticeValueDto(
            @JsonProperty("amount")   Double amount,
            @JsonProperty("currency") String currency
    ) {}
}