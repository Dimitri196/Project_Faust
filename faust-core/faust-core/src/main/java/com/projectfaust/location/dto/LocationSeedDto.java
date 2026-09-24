package com.projectfaust.location.dto;

import com.projectfaust.shared.enums.ClearanceLevel;
import com.projectfaust.shared.enums.LocationType;

import java.util.List;

/**
 * Data Transfer Object for deserialising the geographic seed file
 * ({@code resources/seed/locations.json}) into a typed hierarchy.
 *
 * <p>The recursive {@code children} field mirrors the nested JSON structure directly,
 * eliminating any impedance mismatch between the file format and the Java model.
 * The seed service walks this tree depth-first, persisting parent nodes before
 * their children to satisfy the foreign key constraint.</p>
 *
 * <p><b>Idempotency:</b> the seed service uses
 * {@code LocationRepository#findByNameAndTypeAndParentExternalId} to skip nodes
 * that already exist, making seed runs safe to repeat.</p>
 *
 * @param name           the official name of the location (required).
 * @param type           the {@link LocationType} category of the node (required).
 * @param isoCode        ISO 3166-1 alpha-2 code for countries, or internal code.
 * @param latitude       WGS-84 latitude in decimal degrees; {@code null} if unknown.
 * @param longitude      WGS-84 longitude in decimal degrees; {@code null} if unknown.
 * @param clearanceLevel the security clearance for this node;
 *                       defaults to {@code LEVEL_1_PUBLIC} in the service if omitted.
 * @param children       nested child nodes to be persisted after this node.
 * @author Dimitri / Project Faust
 */
public record LocationSeedDto(
        String name,
        LocationType type,
        String isoCode,
        Double latitude,
        Double longitude,
        ClearanceLevel clearanceLevel,
        List<LocationSeedDto> children
) {}