package com.projectfaust.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeonamesResponse(
        List<GeonameEntry> geonames
) {}