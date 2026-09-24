package com.projectfaust.dto.request;

import com.projectfaust.shared.enums.ThesisType;

public record AcademicThesisScraperRequest(
        String sourceSystemId,
        String authorRaw, // Jméno tak, jak je na webu (např. "Mgr. Petr Mlejnek")
        String title,
        ThesisType thesisType,
        String universityName,
        String facultyName,
        String supervisorName,
        String opponentName,
        Integer defenseYear,
        String repositoryUrl,
        boolean isClassified,
        String abstractText,
        String keywords,
        String language
) {}
