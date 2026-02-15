package com.projectfaust.service;

import com.projectfaust.dto.external.GeonameEntry;
import com.projectfaust.dto.external.GeonamesResponse;
import com.projectfaust.dto.request.LocationRequest;
import com.projectfaust.entity.enums.LocationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationHarvesterService {

    private final LocationService locationService;
    private final RestTemplate restTemplate; // Konfiguruj v configu

    private static final String GEONAMES_URL = "http://api.geonames.org/childrenJSON?geonameId={id}&username={user}";

    /**
     * Stáhne všechny sub-lokace pro daný uzel z externího API
     * a uloží je do tvé hierarchie.
     */
    @Transactional
    public void harvestSubLocations(UUID localParentId, int geonameId) {
        String username = "your_demo_username";

        // GeonamesResponse je nyní Record
        GeonamesResponse response = restTemplate.getForObject(
                GEONAMES_URL, GeonamesResponse.class, geonameId, username);

        // V Recordech se k listu přistupuje přes .geonames()
        if (response != null && response.geonames() != null) {
            for (GeonameEntry entry : response.geonames()) {
                LocationRequest request = new LocationRequest(
                        entry.name(),             // Record access: entry.name()
                        mapFeatureClassToType(entry.fcode()), // entry.fcode()
                        entry.countryCode(),      // entry.countryCode()
                        localParentId
                );

                // Validace a uložení přes tvou service vrstvu
                locationService.createLocation(request);
            }
            log.info("HARVEST_SUCCESS: Integrated {} nodes from Geonames", response.geonames().size());
        }
    }

    private LocationType mapFeatureClassToType(String fcode) {
        return switch (fcode) {
            case "PPLC", "PPLA" -> LocationType.CITY;
            case "ADM1" -> LocationType.PROVINCE;
            case "ADM2" -> LocationType.DISTRICT;
            default -> LocationType.SUBLOCATION;
        };
    }
}
