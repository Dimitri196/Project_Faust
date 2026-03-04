package com.projectfaust.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Basic gender classification for personnel and biographical records.
 */
@Getter
@RequiredArgsConstructor
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other"),
    UNKNOWN("Unknown");

    /**
     * Human-readable label for UI display.
     */
    private final String displayName;
}
