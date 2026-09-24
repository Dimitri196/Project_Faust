package com.projectfaust.intelligence.dto;

import com.projectfaust.person.dto.PersonConnectionResponse;
import com.projectfaust.shared.enums.ClearanceLevel;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing an influence map — a graph of connections
 * radiating outward from a root subject (person or institution).
 *
 * <p>Used by the AI analyst and intelligence report layers to visualise
 * the network of relationships around a target subject. The root can be
 * either a {@link com.projectfaust.person.Person} or an
 * {@link com.projectfaust.institution.Institution}, indicated by
 * {@code rootType}.</p>
 *
 * <p>This DTO lives in the {@code intelligence} module rather than
 * {@code institution} because it crosses domain boundaries — it aggregates
 * data from both the person and institution domains. Placing it in either
 * domain module would create an inappropriate cross-module dependency.</p>
 *
 * @param rootId           the public UUID of the root subject of the influence map.
 * @param rootType         whether the root is a PERSON or INSTITUTION.
 * @param rootFullName     display name of the root subject.
 * @param rootClearance    the clearance level of the root subject record.
 * @param connections      list of direct and indirect connections from the root.
 * @param totalConnections total count of connections in the map.
 * @author Dimitri / Project Faust
 */
public record InfluenceMapResponse(
        UUID rootId,
        RootType rootType,
        String rootFullName,
        ClearanceLevel rootClearance,
        List<PersonConnectionResponse> connections,
        int totalConnections
) {

    /**
     * Classifies the type of the root subject in the influence map.
     */
    public enum RootType {
        /** The root subject is a person (HUMINT target). */
        PERSON,
        /** The root subject is an institution (OSINT target). */
        INSTITUTION
    }
}