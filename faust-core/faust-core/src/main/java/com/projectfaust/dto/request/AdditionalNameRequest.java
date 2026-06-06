package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.NameType;

import java.time.LocalDate;

public record AdditionalNameRequest(
        String firstName,
        String lastName,
        NameType type,
        LocalDate validFrom,
        LocalDate validTo,
        String note
) {}
