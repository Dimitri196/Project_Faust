package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HierarchicalLevel {
    INTERNATIONAL("Supranational bodies like EU or UN"),
    NATIONAL("Central government level"),
    REGIONAL("State, Province, or Voivodeship"),
    LOCAL("Municipal or City level"),
    SUB_LOCAL("Districts or Neighborhood councils");

    private final String description;
}
