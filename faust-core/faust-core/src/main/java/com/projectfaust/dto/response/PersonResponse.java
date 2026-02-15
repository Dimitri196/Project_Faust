package com.projectfaust.dto.response;

import com.projectfaust.entity.enums.EducationLevel;
import com.projectfaust.entity.enums.Gender;
import java.time.LocalDate;
import java.util.UUID;

public record PersonResponse(
        UUID publicId,
        String firstName,
        String lastName,
        String titleBefore,
        String titleAfter,
        String displayName,
        Integer age,
        EducationLevel educationLevel,
        String fieldOfStudy,
        String email,
        String phone,
        String biography,
        String photoUrl,
        String politicalAffiliation,
        LocalDate birthDate,
        Gender gender,
        String nationality,
        String placeOfBirth,
        LocalDate deathDate
) {}
