package com.projectfaust.service;

import com.projectfaust.dto.external.OverpassElement;
import com.projectfaust.dto.external.OverpassResponse;
import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.entity.Location;
import com.projectfaust.entity.enums.LocationType;
import com.projectfaust.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
@Slf4j
public class OverpassImportService {

    private final LocationService locationService;
    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void importObceFromOverpass(String jsonContent, String parentDistrictName) {
        try {
            // 1. Najdeme rodičovský okres v DB
            Location parentDistrict = locationRepository
                    .findByNameAndTypeAndParentExternalId(parentDistrictName, LocationType.DISTRICT, null)
                    .orElseThrow(() -> new RuntimeException("Parent District not found: " + parentDistrictName));

            OverpassResponse response = objectMapper.readValue(jsonContent, OverpassResponse.class);

            int count = 0;
            for (OverpassElement element : response.elements()) {
                // Filtrujeme pouze admin_level 8 (obce)
                if ("8".equals(element.getAdminLevel())) {
                    LocationRequest request = new LocationRequest(
                            element.getName(),
                            LocationType.CITY,
                            "OSM-" + element.id(), // Použijeme OSM ID jako unikátní kód
                            parentDistrict.getExternalId()
                    );

                    try {
                        locationService.createLocation(request);
                        count++;
                    } catch (Exception e) {
                        log.warn("Skipping duplicate or invalid obec: {}", element.getName());
                    }
                }
            }
            log.info("IMPORT_COMPLETE: Processed {} obce into {}", count, parentDistrictName);
        } catch (Exception e) {
            log.error("IMPORT_FAILED: Could not process Overpass data", e);
        }
    }
}