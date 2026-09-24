package com.projectfaust.location;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Batch geocoding service — resolves GPS coordinates for locations
 * that have no latitude/longitude using Nominatim API.
 *
 * <p>Nominatim fair use policy: max 1 request/second, User-Agent required.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchGeocodingService {

    private final LocationRepository locationRepository;
    private final RestTemplate       restTemplate;

    private static final String NOMINATIM_URL  =
            "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT     =
            "ProjectFaust/1.0 (intelligence platform; contact@faust.gov)";
    private static final long   DELAY_MS       = 1100; // Nominatim fair use

    public record GeocodingResult(
            int total,
            int resolved,
            int failed,
            int skipped
    ) {}

    /**
     * Geocodes all locations without coordinates under a given parent.
     * Uses location name + parent name for accurate geocoding.
     *
     * @param parentId internal Long ID of the parent location (district/province).
     * @return result summary.
     */
    @Transactional
    public GeocodingResult geocodeByParent(Long parentId) {
        List<Location> locations = locationRepository
                .findAllByParentIdAndLatitudeIsNull(parentId);

        log.info("GEOCODE: Found {} locations without GPS under parent {}.",
                locations.size(), parentId);

        // Get parent name for search context
        String parentName = locationRepository.findById(parentId)
                .map(Location::getName)
                .orElse("");

        int resolved = 0, failed = 0, skipped = 0;

        for (Location loc : locations) {
            try {
                double[] coords = geocode(loc.getName(), loc.getLocalName(), parentName);
                if (coords != null) {
                    loc.setLatitude(coords[0]);
                    loc.setLongitude(coords[1]);
                    locationRepository.save(loc);
                    resolved++;
                    log.debug("GEOCODE: {} → {}, {}", loc.getName(), coords[0], coords[1]);
                } else {
                    failed++;
                    log.warn("GEOCODE: No result for '{}'.", loc.getName());
                }
                Thread.sleep(DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                failed++;
                log.error("GEOCODE: Error for '{}' — {}", loc.getName(), e.getMessage());
            }
        }

        log.info("GEOCODE: Complete — resolved={}, failed={}, skipped={}.",
                resolved, failed, skipped);
        return new GeocodingResult(locations.size(), resolved, failed, skipped);
    }

    /**
     * Tries to geocode a location by name using Nominatim.
     * Tries local name first (more accurate for non-English names),
     * then English name, then with parent context.
     *
     * @return [lat, lon] or null if not found.
     */
    private double[] geocode(String name, String localName, String parentName) {
        // Try local name first (Belarusian/Czech names are more accurate)
        if (localName != null && !localName.isBlank()) {
            double[] result = nominatimSearch(localName + ", " + parentName);
            if (result != null) return result;
        }

        // Try English name with parent context
        double[] result = nominatimSearch(name + ", " + parentName);
        if (result != null) return result;

        // Try English name only
        return nominatimSearch(name);
    }

    @SuppressWarnings("unchecked")
    private double[] nominatimSearch(String query) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept-Language", "en");

            String url = NOMINATIM_URL +
                    "?q=" + java.net.URLEncoder.encode(query, "UTF-8") +
                    "&format=json&limit=1";

            ResponseEntity<List> response = restTemplate.exchange(
                    url, HttpMethod.GET,
                    new HttpEntity<>(headers),
                    List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map<String, Object> result = (Map<String, Object>) response.getBody().get(0);
                double lat = Double.parseDouble(result.get("lat").toString());
                double lon = Double.parseDouble(result.get("lon").toString());
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            log.debug("GEOCODE: Nominatim error for '{}' — {}", query, e.getMessage());
        }
        return null;
    }
}