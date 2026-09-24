package com.projectfaust.ingest.realestate.core;

import com.projectfaust.ingest.realestate.dto.RealEstateResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link RealEstateOwnership} entities to {@link RealEstateResponse} DTOs.
 *
 * <p>Not using MapStruct here — the entity has lazy-loaded FK relations
 * (person, institution) that need null-safe resolution, which MapStruct
 * handles poorly without custom configuration. Direct mapping is cleaner
 * and more explicit for this case.</p>
 *
 * @author Dimitri / Project Faust
 */
@Component
public class RealEstateMapper {

    public RealEstateResponse toResponse(RealEstateOwnership entity) {
        return new RealEstateResponse(
                entity.getExternalId(),
                entity.getSourceSystem() != null
                        ? entity.getSourceSystem().name() : null,
                entity.getCountryCode(),
                entity.getPropertyType() != null
                        ? entity.getPropertyType().name() : null,
                entity.getPropertyAddress(),
                entity.getCadastralUnit(),
                entity.getParcelNumber(),
                entity.getOwnershipShare(),
                entity.getEstimatedValue(),
                entity.getCurrency(),
                entity.isEncumbered(),
                entity.getRegistryUrl(),
                // Person — null if this record is institution-owned
                entity.getPerson() != null
                        ? entity.getPerson().getExternalId() : null,
                entity.getPerson() != null
                        ? entity.getPerson().getFirstName() + " "
                          + entity.getPerson().getLastName() : null,
                // Institution — null if this record is person-owned
                entity.getInstitution() != null
                        ? entity.getInstitution().getExternalId() : null,
                entity.getInstitution() != null
                        ? entity.getInstitution().getName() : null,
                entity.getIngestedAt()
        );
    }
}