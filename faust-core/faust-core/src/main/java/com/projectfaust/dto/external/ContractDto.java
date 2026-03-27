package com.projectfaust.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ContractDto(
        @JsonProperty("Id") String id,
        @JsonProperty("Predmet") String subject,

        // Pokrýváme všechny možné názvy polí pro cenu
        @JsonAlias({"CenaBezDPH", "hodnotaBezDph", "hodnota", "Cena", "CenaVcetneDph"})
        Double priceWithoutVat,

        @JsonProperty("DatumUzavreni") String dateConfirmed,

        @JsonAlias({"Platce", "platce"})
        InstitutionInfo buyer,

        // KLÍČOVÁ ZMĚNA: Aliasy pro dodavatele
        @JsonAlias({"Dodavatele", "Prijemce", "prijemce", "dodavatele"})
        List<InstitutionInfo> suppliers,

        // Záchranná síť: Seznam všech stran smlouvy
        @JsonProperty("SmluvniStrany")
        List<InstitutionInfo> allParties
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InstitutionInfo(
            @JsonAlias({"Ico", "ico", "ICO"}) String ico,
            @JsonProperty("Nazev") String name
    ) {}
}
