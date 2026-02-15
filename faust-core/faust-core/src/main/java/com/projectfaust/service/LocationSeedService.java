package com.projectfaust.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectfaust.dto.LocationSeedDto;
import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.dto.response.LocationResponse;
import com.projectfaust.entity.Location;
import com.projectfaust.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;


import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationSeedService {

    private final LocationRepository locationRepository;
    private final LocationService locationService;
    private final ObjectMapper objectMapper;


    @Transactional
    public void seedLocations() {
        try {
            InputStream inputStream = getClass().getResourceAsStream("/seed/locations.json");
            if (inputStream == null) {
                log.warn("SEED_SKIPPED: Resource '/seed/locations.json' not found.");
                return;
            }

            List<LocationSeedDto> rootLocations = objectMapper.readValue(inputStream,
                    new TypeReference<List<LocationSeedDto>>() {});

            for (LocationSeedDto rootDto : rootLocations) {
                processLocation(rootDto, null);
            }
        } catch (Exception e) {
            log.error("SEED_CRITICAL_FAILURE: Geographic backbone initialization failed", e);
        }
    }

    private void processLocation(LocationSeedDto dto, UUID parentExternalId) {
        Optional<Location> existing = locationRepository.findByNameAndTypeAndParentExternalId(
                dto.name(), dto.type(), parentExternalId);

        UUID currentExternalId;

        if (existing.isEmpty()) {
            LocationRequest request = new LocationRequest(
                    dto.name(), dto.type(), dto.isoCode(), parentExternalId);

            LocationResponse saved = locationService.createLocation(request);
            currentExternalId = saved.externalId();
            log.info("NODE_CREATED: {} [{}]", dto.name(), dto.type());
        } else {
            currentExternalId = existing.get().getExternalId();
            log.debug("NODE_EXISTS: Skipping creation for {}", dto.name());
        }

        if (dto.children() != null) {
            for (LocationSeedDto child : dto.children()) {
                processLocation(child, currentExternalId);
            }
        }
    }
}
