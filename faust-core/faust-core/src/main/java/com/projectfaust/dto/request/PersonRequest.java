package com.projectfaust.dto.request;

import com.projectfaust.entity.enums.EducationLevel;
import com.projectfaust.entity.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record PersonRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        String titleBefore,
        String titleAfter,
        EducationLevel educationLevel,
        String fieldOfStudy,
        @Email String email,
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
