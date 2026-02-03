package com.projectfaust.entity.enums;

public enum ClearanceLevel {
    LEVEL_1_PUBLIC,    // Vidí jen základní názvy institucí
    LEVEL_2_INTERNAL,  // Vidí strukturu a obsazené pozice
    LEVEL_3_CONFIDENTIAL, // Vidí detaily osob (maily, telefony) [cite: 3, 5]
    LEVEL_4_SECRET,    // Vidí historii jmenování a životopisy [cite: 7]
    LEVEL_5_TOP_SECRET // Vidí vše včetně platů a vazeb
}