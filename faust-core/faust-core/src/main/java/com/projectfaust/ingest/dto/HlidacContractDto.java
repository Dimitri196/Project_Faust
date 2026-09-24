package com.projectfaust.ingest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.projectfaust.ingest.hlidacstatu.HlidacStatuContractMapper;

import java.util.List;

/**
 * Raw response DTO for a single contract record from the Hlidac Statu (CZ) API.
 *
 * <p>Field names follow the Czech API's terminology with multiple
 * {@code @JsonAlias} entries — the API has historically used inconsistent
 * field names across endpoints and versions (e.g. price has appeared as
 * {@code CenaBezDPH}, {@code hodnotaBezDph}, {@code hodnota}, {@code Cena}).</p>
 *
 * <p>This DTO is consumed exclusively by {@link HlidacStatuContractMapper} —
 * no other part of the system should reference Czech field names directly.</p>
 *
 * @author Dimitri / Project Faust
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record HlidacContractDto(

        @JsonProperty("Id")
        String id,

        @JsonProperty("Predmet")
        String subject,

        @JsonAlias({"CenaBezDPH", "hodnotaBezDph", "hodnota", "Cena", "CenaVcetneDph"})
        Double priceWithoutVat,

        @JsonProperty("DatumUzavreni")
        String dateConfirmed,

        @JsonAlias({"Platce", "platce"})
        InstitutionInfo buyer,

        @JsonAlias({"Dodavatele", "Prijemce", "prijemce", "dodavatele"})
        List<InstitutionInfo> suppliers,

        @JsonProperty("SmluvniStrany")
        List<InstitutionInfo> allParties

) {
    /**
     * Represents an institution (buyer or supplier) within a Hlidac Statu contract record.
     *
     * @param ico  the Czech company registration number (IČO).
     * @param name the institution's display name.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstitutionInfo(
            @JsonAlias({"Ico", "ico", "ICO"}) String ico,
            @JsonProperty("Nazev") String name
    ) {}
}