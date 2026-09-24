package com.projectfaust.ingest.realestate.cuzk;

import com.projectfaust.ingest.realestate.core.RealEstateOwnership;
import com.projectfaust.ingest.realestate.core.RealEstateSourceMapper;
import com.projectfaust.ingest.realestate.dto.RealEstateOwnershipDto;
import com.projectfaust.shared.enums.CadasterSourceSystem;
import com.projectfaust.shared.enums.PropertyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Maps normalised {@link RealEstateOwnershipDto} records from the ČÚZK
 * cadaster into {@link RealEstateOwnership} entities.
 *
 * <p>Since all country-specific fetch services normalise to
 * {@link RealEstateOwnershipDto} before dispatching to Kafka, this mapper
 * operates on the already-normalised DTO — no country-specific field
 * name handling needed here. The fetch service handles raw-to-DTO mapping;
 * this mapper handles DTO-to-entity mapping.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Component
public class CuzkRealEstateMapper implements RealEstateSourceMapper<RealEstateOwnershipDto> {

    @Override
    public CadasterSourceSystem supports() {
        return CadasterSourceSystem.CUZK_CZ;
    }

    @Override
    public RealEstateOwnership toEntity(RealEstateOwnershipDto source) {
        PropertyType propertyType = resolvePropertyType(source.propertyType());

        return RealEstateOwnership.builder()
                .sourceSystem(CadasterSourceSystem.CUZK_CZ)
                .sourceRegistryId(source.externalId())
                .countryCode("CZ")
                .ownerName(source.ownerName())
                .ownerNationalId(source.ownerNationalId())
                .propertyType(propertyType)
                .propertyAddress(source.propertyAddress())
                .cadastralUnit(source.cadastralUnit())
                .parcelNumber(source.parcelNumber())
                .ownershipShare(source.ownershipShare())
                .estimatedValue(source.estimatedValue() != null
                        ? BigDecimal.valueOf(source.estimatedValue()) : null)
                .currency(source.currency() != null ? source.currency() : "CZK")
                .encumbered(Boolean.TRUE.equals(source.encumbered()))
                .registryUrl(source.registryUrl())
                .rawData(source.rawData())
                // person/institution FK resolved by consumer after
                // looking up ownerNationalId in identifier table
                .build();
    }

    private PropertyType resolvePropertyType(String raw) {
        if (raw == null) return PropertyType.BUILDING;
        return switch (raw.toUpperCase()) {
            case "PARCEL"   -> PropertyType.PARCEL;
            case "UNIT"     -> PropertyType.UNIT;
            default         -> PropertyType.BUILDING;
        };
    }
}