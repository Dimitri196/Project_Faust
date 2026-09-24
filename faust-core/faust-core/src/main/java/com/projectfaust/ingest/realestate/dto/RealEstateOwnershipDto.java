package com.projectfaust.ingest.realestate.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Normalised DTO representing a single real estate ownership record
 * as received from any country-specific cadaster source.
 *
 * <p>Country-specific fetch services map their raw response into this
 * shape before dispatching to Kafka. The consumer and ingest pipeline
 * never see country-specific field names — only this normalised structure.
 * Mirrors the role of {@link com.projectfaust.ingest.dto.HlidacContractDto}
 * in the contract pipeline.</p>
 *
 * @param sourceSystem      country/registry identifier (e.g. "CUZK_CZ", "KATASTER_SK").
 * @param externalId        unique ID in the source registry.
 * @param countryCode       ISO 3166-1 alpha-2 country code.
 * @param ownerNationalId   owner's national registration number (IČO for CZ entities).
 * @param ownerName         full name as registered in the cadaster.
 * @param ownerType         "PERSON" or "INSTITUTION".
 * @param propertyType      "PARCEL", "BUILDING", or "UNIT".
 * @param propertyAddress   human-readable address.
 * @param cadastralUnit     cadastral unit name/code (katastrální území for CZ).
 * @param parcelNumber      parcel number as registered in the cadaster.
 * @param ownershipShare    fractional share if co-owned (e.g. "1/2") — null if sole owner.
 * @param estimatedValue    estimated value in local currency — null if not published.
 * @param currency          ISO 4217 currency code.
 * @param encumbered        true if property has registered encumbrances.
 * @param registryUrl       direct URL to the record in the public cadaster viewer.
 * @param rawData           raw source payload for forensic analysis.
 *
 * @author Dimitri / Project Faust
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RealEstateOwnershipDto(
        @JsonProperty("sourceSystem")    String sourceSystem,
        @JsonProperty("externalId")      String externalId,
        @JsonProperty("countryCode")     String countryCode,
        @JsonProperty("ownerNationalId") String ownerNationalId,
        @JsonProperty("ownerName")       String ownerName,
        @JsonProperty("ownerType")       String ownerType,
        @JsonProperty("propertyType")    String propertyType,
        @JsonProperty("propertyAddress") String propertyAddress,
        @JsonProperty("cadastralUnit")   String cadastralUnit,
        @JsonProperty("parcelNumber")    String parcelNumber,
        @JsonProperty("ownershipShare")  String ownershipShare,
        @JsonProperty("estimatedValue")  Double estimatedValue,
        @JsonProperty("currency")        String currency,
        @JsonProperty("encumbered")      Boolean encumbered,
        @JsonProperty("registryUrl")     String registryUrl,
        @JsonProperty("rawData")         String rawData
) {}