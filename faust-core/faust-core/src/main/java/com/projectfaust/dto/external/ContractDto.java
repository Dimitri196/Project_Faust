package com.projectfaust.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractDto(
        @JsonProperty("Id") String id,
        @JsonProperty("Predmet") String subject,
        @JsonAlias({"CenaBezDPH", "hodnotaBezDph", "hodnota", "Cena"})
        Double priceWithoutVat,
        @JsonProperty("DatumUzavreni") String dateConfirmed,
        @JsonProperty("Platce") InstitutionInfo buyer,
        @JsonProperty("Dodavatele") List<InstitutionInfo> suppliers
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstitutionInfo(
            @JsonAlias({"Ico", "ico"}) String ico,
            @JsonProperty("Nazev") String name
    ) {}
}
