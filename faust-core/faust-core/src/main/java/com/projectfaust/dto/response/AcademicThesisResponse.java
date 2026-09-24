package com.projectfaust.dto.response;

import com.projectfaust.shared.enums.ThesisType;

import java.util.UUID;

public record AcademicThesisResponse(
        UUID externalId,
        UUID personPublicId,
        String title,
        ThesisType thesisType,
        String universityName,
        String supervisorName,
        Integer defenseYear,
        boolean isClassified,
        boolean isVerifiedByAgent,
        String repositoryUrl
) {}
