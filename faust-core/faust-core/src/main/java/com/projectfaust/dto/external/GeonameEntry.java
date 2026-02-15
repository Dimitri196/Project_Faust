package com.projectfaust.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeonameEntry(
        int geonameId,
        String name,
        String countryCode,
        String fcode,
        long population
) {}