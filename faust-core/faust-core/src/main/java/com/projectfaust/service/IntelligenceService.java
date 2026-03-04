package com.projectfaust.service;

import com.projectfaust.dto.request.PersonConnectionRequest;
import com.projectfaust.dto.response.ConnectionPathResponse;
import com.projectfaust.dto.response.InfluenceMapResponse;
import com.projectfaust.dto.response.PersonConnectionResponse;
import com.projectfaust.entity.Person;
import com.projectfaust.entity.PersonConnection;
import com.projectfaust.mapper.PersonConnectionMapper;
import com.projectfaust.repository.PersonConnectionRepository;
import com.projectfaust.repository.PersonRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private final PersonRepository personRepository;
    private final PersonConnectionRepository connectionRepository;
    private final PersonConnectionMapper connectionMapper;

    /**
     * Sestaví mapu vlivu pro daný subjekt.
     * Automaticky řeší obousměrnost vztahů v grafu.
     */
    @Transactional(readOnly = true)
    public InfluenceMapResponse getInfluenceMap(UUID externalId) {

        Person root = personRepository.findByExternalId(externalId)
                .orElseThrow(() -> new EntityNotFoundException("Subject identification failed. Access denied."));

        List<PersonConnection> rawConnections = connectionRepository.findAllByPersonExternalId(externalId);

        List<PersonConnectionResponse> normalizedConnections = rawConnections.stream()
                .map(conn -> normalizeConnection(conn, externalId))
                .toList();

        return new InfluenceMapResponse(
                root.getExternalId(),
                root.getFullName(),
                root.getClearanceLevel().name(),
                normalizedConnections,
                normalizedConnections.size()
        );
    }

    /**
     * Zajišťuje, aby v Response byl 'Target' vždy ten "druhý" člověk,
     * i když je v DB subjekt uložen na pozici targetu.
     */
    private PersonConnectionResponse normalizeConnection(PersonConnection conn, UUID rootId) {
        // Pokud je kořenový subjekt (ten, koho hledáme) v DB jako Target, musíme vazbu pro HUD "otočit"
        if (conn.getTargetPerson().getExternalId().equals(rootId)) {
            return mapInverted(conn);
        }
        // Jinak použijeme standardní MapStruct mapování
        return connectionMapper.toResponse(conn);
    }

    /**
     * Manuální mapování pro invertovaný vztah (Target -> Source).
     */
    private PersonConnectionResponse mapInverted(PersonConnection conn) {
        Person otherPerson = conn.getSourcePerson(); // Pro nás je teď cílem ten, kdo byl v DB zdroj

        return new PersonConnectionResponse(
                conn.getExternalId(),
                otherPerson.getExternalId(),
                otherPerson.getFullName(),
                conn.getConnectionType(),
                conn.getInfluenceScore(),
                "[REVERSED] " + conn.getDescription(),
                extractCurrentPosition(otherPerson),
                conn.getStartDate(),
                isStillActive(conn.getEndDate())
        );
    }

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

    /**
     * Realizuje algoritmus prohledávání do šířky (BFS) pro nalezení
     * nejkratší cesty mezi dvěma subjekty v grafu vlivu.
     */
    @Transactional(readOnly = true)
    public ConnectionPathResponse findPath(UUID startId, UUID targetId) {
        if (startId.equals(targetId)) {
            return new ConnectionPathResponse(0, List.of("Self"), List.of(startId), true);
        }

        // Fronta pro cesty (Queue of Paths)
        Queue<List<UUID>> queue = new LinkedList<>();
        // Množina navštívených uzlů, aby se zabránilo nekonečným cyklům
        Set<UUID> visited = new HashSet<>();

        queue.add(List.of(startId));
        visited.add(startId);

        while (!queue.isEmpty()) {
            List<UUID> currentPath = queue.poll();

            // 1. Kontrola hloubky (Max 10 stupňů odloučení)
            if (currentPath.size() > 10) continue;

            UUID lastNodeId = currentPath.get(currentPath.size() - 1);
            List<PersonConnection> neighbors = connectionRepository.findAllByPersonExternalId(lastNodeId);

            for (PersonConnection conn : neighbors) {
                UUID neighborId = conn.getSourcePerson().getExternalId().equals(lastNodeId)
                        ? conn.getTargetPerson().getExternalId()
                        : conn.getSourcePerson().getExternalId();

                if (neighborId.equals(targetId)) {
                    List<UUID> finalPathIds = new ArrayList<>(currentPath);
                    finalPathIds.add(neighborId);

                    // --- EFEKTIVNÍ BULK FETCH JMEN ---
                    List<Person> persons = personRepository.findAllByExternalIdIn(finalPathIds);
                    Map<UUID, String> nameMap = persons.stream()
                            .collect(Collectors.toMap(Person::getExternalId, Person::getFullName));

                    List<String> names = finalPathIds.stream()
                            .map(id -> nameMap.getOrDefault(id, "REDACTED"))
                            .toList();

                    return new ConnectionPathResponse(finalPathIds.size() - 1, names, finalPathIds, true);
                }

                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    List<UUID> nextPath = new ArrayList<>(currentPath);
                    nextPath.add(neighborId);
                    queue.add(nextPath);
                }
            }
        }

        // Pokud fronta skončí a cíl jsme nenašli, cesta neexistuje
        return new ConnectionPathResponse(-1, List.of(), List.of(), false);
    }

    @Transactional
    public PersonConnectionResponse createConnection(PersonConnectionRequest request) {
        Person source = personRepository.findByExternalId(request.sourcePersonId())
                .orElseThrow(() -> new EntityNotFoundException("Source node not found"));
        Person target = personRepository.findByExternalId(request.targetPersonId())
                .orElseThrow(() -> new EntityNotFoundException("Target node not found"));

        PersonConnection connection = PersonConnection.builder()
                .externalId(UUID.randomUUID())
                .sourcePerson(source)
                .targetPerson(target)
                .connectionType(request.type())
                .influenceScore(request.influenceScore())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        PersonConnection saved = connectionRepository.save(connection);
        return connectionMapper.toResponse(saved);
    }

}
