package com.projectfaust.person.document;

import com.projectfaust.person.Person;
import com.projectfaust.person.document.dto.PersonDocumentRequest;
import com.projectfaust.person.document.dto.PersonDocumentResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for {@link PersonDocument} ↔ DTOs.
 *
 * @author Dimitri / Project Faust
 */
@Component
public class PersonDocumentMapper {

    public PersonDocument toEntity(PersonDocumentRequest request, Person person) {
        return PersonDocument.builder()
                .person(person)
                .documentType(request.documentType())
                .documentNumberRaw(request.documentNumberRaw())
                .issuingState(request.issuingState())
                .issuingAuthority(request.issuingAuthority())
                .validFrom(request.validFrom())
                .validTo(request.validTo())
                .active(request.active())
                .authentic(request.authentic())
                .mrzLine(request.mrzLine())
                .verificationStatus(request.verificationStatus())
                .confidenceScore(request.confidenceScore() != null ? request.confidenceScore() : 1.0)
                .clearanceLevel(request.clearanceLevel())
                .analyticalNote(request.analyticalNote())
                .build();
    }

    public PersonDocumentResponse toResponse(PersonDocument doc) {
        return new PersonDocumentResponse(
                doc.getExternalId(),
                doc.getPerson().getExternalId(),
                doc.getPerson().getFullName(),
                doc.getDocumentType(),
                doc.getDocumentNumberRaw(),
                doc.getDocumentNumberNormalized(),
                doc.getIssuingState(),
                doc.getIssuingAuthority(),
                doc.getValidFrom(),
                doc.getValidTo(),
                doc.isActive(),
                doc.isAuthentic(),
                doc.getMrzLine(),
                doc.getVerificationStatus(),
                doc.getConfidenceScore(),
                doc.getClearanceLevel(),
                doc.getAnalyticalNote(),
                doc.getCreatedAt()
        );
    }
}