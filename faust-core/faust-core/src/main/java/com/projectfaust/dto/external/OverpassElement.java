package com.projectfaust.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverpassElement(
        long id,
        Map<String, String> tags
) {
    public String getName() { return tags.get("name"); }
    public String getAdminLevel() { return tags.get("admin_level"); }
}
