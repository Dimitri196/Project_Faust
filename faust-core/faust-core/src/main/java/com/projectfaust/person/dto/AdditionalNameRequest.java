package com.projectfaust.person.dto;

import com.projectfaust.shared.enums.NameType;

import java.time.LocalDate;

public record AdditionalNameRequest(
        String firstName,
        String lastName,
        NameType type,
        LocalDate validFrom,
        LocalDate validTo,
        String note
) {}
