package com.projectfaust.intelligence;

import com.projectfaust.intelligence.dto.ConnectionPathResponse;
import com.projectfaust.intelligence.dto.InfluenceMapResponse;
import com.projectfaust.person.Person;
import com.projectfaust.person.PersonConnection;
import com.projectfaust.person.PersonConnectionMapper;
import com.projectfaust.person.PersonConnectionRepository;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.person.dto.PersonConnectionRequest;
import com.projectfaust.person.dto.PersonConnectionResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core service for intelligence graph operations within Project Faust.
 *
 * <p>Provides three analytical capabilities:</p>
 * <ul>
 *   <li><b>Influence map</b> — builds the full connection graph for a subject,
 *       normalising bidirectional edges so the root subject is always the source.</li>
 *   <li><b>Path finding</b> — BFS algorithm to find the shortest connection path
 *       between two persons (degrees of separation), capped at 10 hops.</li>
 *   <li><b>Connection creation</b> — creates directed person-to-person connections
 *       in the intelligence graph.</li>
 * </ul>
 *
 * @author Dimitri / Project Faust
 */
@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private final PersonRepository personRepository;
    private final PersonConnectionRepository connectionRepository;
    private final PersonConnectionMapper connectionMapper;

    // -------------------------------------------------------------------------
    // Influence map
    // -------------------------------------------------------------------------

    /**
     * Builds the influence map for a subject — all connections normalised so
     * the root subject always appears as the source node.
     *
     * <p>When a connection is stored with the root as the target (i.e. someone
     * else initiated the relationship), it is inverted in the response so the
     * frontend graph always renders the root at the centre.</p>
     *
     * @param externalId the public UUID of the root subject.
     * @return the influence map centred on the given subject.
     * @throws EntityNotFoundException if no person matches the given UUID.
     */
    @Transactional(readOnly = true)
    public InfluenceMapResponse getInfluenceMap(UUID externalId) {
        Person root = personRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("PERSON_NOT_FOUND: " + externalId));

        List<PersonConnection> rawConnections =
                connectionRepository.findAllByPersonExternalId(externalId);

        List<PersonConnectionResponse> normalizedConnections = rawConnections.stream()
                .map(conn -> normalizeConnection(conn, externalId))
                .toList();

        return new InfluenceMapResponse(
                root.getExternalId(),
                InfluenceMapResponse.RootType.PERSON,
                root.getFullName(),
                root.getClearanceLevel(),
                normalizedConnections,
                normalizedConnections.size()
        );
    }

    /**
     * Ensures the response always presents the "other" person as the target,
     * regardless of which direction the connection was stored in the DB.
     */
    private PersonConnectionResponse normalizeConnection(PersonConnection conn, UUID rootId) {
        if (conn.getTargetPerson().getExternalId().equals(rootId)) {
            return mapInverted(conn);
        }
        return connectionMapper.toResponse(conn);
    }

    /**
     * Manually maps a connection in the inverted direction (target → source)
     * for cases where the root subject was stored as the target in the DB.
     */
    private PersonConnectionResponse mapInverted(PersonConnection conn) {
        Person otherPerson = conn.getSourcePerson();

        return new PersonConnectionResponse(
                conn.getExternalId(),
                otherPerson.getExternalId(),
                otherPerson.getFullName(),
                conn.getConnectionType(),
                conn.getInfluenceScore(),
                "[REVERSED] " + conn.getDescription(),
                extractCurrentPosition(otherPerson),
                conn.getStartDate(),
                isStillActive(conn.getEndDate()),
                conn.getVerificationStatus()
        );
    }

    // -------------------------------------------------------------------------
    // BFS path finding
    // -------------------------------------------------------------------------

    /**
     * Finds the shortest connection path between two persons using BFS.
     *
     * <p>The graph is treated as undirected — connections are traversed in
     * both directions regardless of how they were stored. Search depth is
     * capped at 10 hops to prevent runaway traversal on dense graphs.</p>
     *
     * @param startId  the public UUID of the starting person.
     * @param targetId the public UUID of the destination person.
     * @return a {@link ConnectionPathResponse} with the path, or {@code found = false}
     *         if no path exists within 10 hops.
     */
    @Transactional(readOnly = true)
    public ConnectionPathResponse findPath(UUID startId, UUID targetId) {
        if (startId.equals(targetId)) {
            return new ConnectionPathResponse(0, List.of("Self"), List.of(startId), true);
        }

        Queue<List<UUID>> queue = new LinkedList<>();
        Set<UUID> visited = new HashSet<>();

        queue.add(List.of(startId));
        visited.add(startId);

        while (!queue.isEmpty()) {
            List<UUID> currentPath = queue.poll();

            // Cap at 10 degrees of separation
            if (currentPath.size() > 10) continue;

            UUID lastNodeId = currentPath.get(currentPath.size() - 1);
            List<PersonConnection> neighbors =
                    connectionRepository.findAllByPersonExternalId(lastNodeId);

            for (PersonConnection conn : neighbors) {
                UUID neighborId = conn.getSourcePerson().getExternalId().equals(lastNodeId)
                        ? conn.getTargetPerson().getExternalId()
                        : conn.getSourcePerson().getExternalId();

                if (neighborId.equals(targetId)) {
                    List<UUID> finalPathIds = new ArrayList<>(currentPath);
                    finalPathIds.add(neighborId);

                    // Bulk fetch all names in a single query
                    List<Person> persons = personRepository.findAllByExternalIdIn(finalPathIds);
                    Map<UUID, String> nameMap = persons.stream()
                            .collect(Collectors.toMap(Person::getExternalId, Person::getFullName));

                    List<String> names = finalPathIds.stream()
                            .map(id -> nameMap.getOrDefault(id, "REDACTED"))
                            .toList();

                    return new ConnectionPathResponse(
                            finalPathIds.size() - 1, names, finalPathIds, true);
                }

                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    List<UUID> nextPath = new ArrayList<>(currentPath);
                    nextPath.add(neighborId);
                    queue.add(nextPath);
                }
            }
        }

        return new ConnectionPathResponse(-1, List.of(), List.of(), false);
    }

    // -------------------------------------------------------------------------
    // Connection creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new directed connection between two persons and persists it.
     *
     * <p>Uses {@link PersonConnectionMapper#toEntity} for consistent mapping
     * rather than manual builder construction.</p>
     *
     * @param request the connection creation request.
     * @return the persisted connection as a response DTO.
     * @throws EntityNotFoundException if source or target person is not found.
     */
    @Transactional
    public PersonConnectionResponse createConnection(PersonConnectionRequest request) {
        return connectionMapper.toResponse(
                connectionRepository.save(connectionMapper.toEntity(request)));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractCurrentPosition(Person p) {
        if (p.getAppointments() == null) return "N/A";
        return p.getAppointments().stream()
                .filter(a -> a.getEndDate() == null)
                .map(a -> a.getOccupation().getTitle())
                .findFirst()
                .orElse("INACTIVE");
    }

    private boolean isStillActive(LocalDate endDate) {
        return endDate == null || endDate.isAfter(LocalDate.now());
    }
}