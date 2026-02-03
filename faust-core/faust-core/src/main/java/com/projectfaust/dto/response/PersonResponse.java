package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.EducationLevel;

import java.util.UUID;

public record PersonResponse(
        UUID publicId,
        String firstName,
        String lastName,
        String titleBefore,
        String titleAfter,
        String displayName, // Combined: "Mgr. Jan Novák, Ph.D."
        EducationLevel educationLevel,
        String fieldOfStudy,
        String email,
        String phone,
        String biography
) {}