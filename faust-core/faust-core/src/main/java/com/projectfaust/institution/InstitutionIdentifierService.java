package com.projectfaust.institution;

import com.projectfaust.institution.dto.InstitutionIdentifierRequest;
import com.projectfaust.institution.dto.InstitutionIdentifierResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing {@link InstitutionIdentifier} records.
 *
 * <p>Identifiers bridge {@link Institution} nodes to external data sources
 * (procurement contracts, sanctions lists, financial disclosures). Each
 * identifier carries a scheme type, value, and optional country code,
 * enabling source-agnostic linkage across any national or international
 * registration system.</p>
 *
 * @author Dimitri / Project Faust
 */
@Service
@RequiredArgsConstructor
public class InstitutionIdentifierService {

    private final InstitutionRepository institutionRepository;
    private final InstitutionIdentifierRepository identifierRepository;

    /**
     * Returns all active identifiers for the given institution.
     *
     * @param #id public UUID of the institution.
     * @return list of active identifier records.
     */
    private InstitutionIdentifierResponse toResponse(InstitutionIdentifier i) {
        return new InstitutionIdentifierResponse(
                i.getId(),
                i.getType(),
                i.getValue(),
                i.getCountryCode(),
                i.getNote(),
                i.isActive(),
                i.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InstitutionIdentifierResponse> getAll(UUID institutionPublicId) {
        Institution institution = resolve(institutionPublicId);
        return identifierRepository
                .findAllByInstitutionIdAndActiveTrue(institution.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Registers a new identifier against an institution.
     *
     * <p>Rejects duplicates — if an active identifier with the same
     * type + countryCode + value already exists, returns HTTP 409.</p>
     *
     * @param institutionPublicId public UUID of the institution.
     * @param request             the validated creation request.
     * @return the created identifier record.
     */
    @Transactional
    public InstitutionIdentifierResponse add(UUID institutionPublicId,
                                             InstitutionIdentifierRequest request) {
        Institution institution = resolve(institutionPublicId);

        // Normalise country code before duplicate check
        String normalizedCountry = request.countryCode() != null
                ? request.countryCode().trim().toUpperCase()
                : null;

        // Duplicate check — same scheme + value + country for this institution
        boolean exists = identifierRepository
                .findAllByInstitutionIdAndActiveTrue(institution.getId())
                .stream()
                .anyMatch(i -> i.getType() == request.type()
                        && i.getValue().equals(request.value().trim())
                        && Objects.equals(i.getCountryCode(), normalizedCountry));

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active identifier of type " + request.type() +
                            " with value '" + request.value() + "' already exists for this institution.");
        }

        InstitutionIdentifier entity = InstitutionIdentifier.builder()
                .type(request.type())
                .value(request.value().trim())
                .countryCode(normalizedCountry)
                .note(request.note())
                .active(true)
                .build();
        institution.addIdentifier(entity);

        return toResponse(identifierRepository.save(entity));
    }

    /**
     * Deactivates (soft-deletes) an identifier.
     *
     * <p>Records are never hard-deleted — deactivation preserves the
     * audit trail of historical linkage.</p>
     *
     * @param institutionPublicId public UUID of the institution (ownership check).
     * @param identifierId        UUID of the identifier to deactivate.
     */
    @Transactional
    public void deactivate(UUID institutionPublicId, UUID identifierId) {
        Institution institution = resolve(institutionPublicId);

        InstitutionIdentifier identifier = identifierRepository.findById(identifierId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Identifier not found: " + identifierId));

        if (!identifier.getInstitution().getId().equals(institution.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Identifier does not belong to institution: " + institutionPublicId);
        }

        identifier.setActive(false);
        identifierRepository.save(identifier);
    }

    private Institution resolve(UUID publicId) {
        return institutionRepository.findByExternalId(publicId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Institution not found: " + publicId));
    }
}