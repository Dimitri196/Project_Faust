package com.projectfaust.steganography;

import com.projectfaust.institution.Institution;
import com.projectfaust.institution.InstitutionRepository;
import com.projectfaust.occupation.OccupationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Assembles the intelligence payload and orchestrates steganographic export.
 *
 * <p>Payload structure:
 * <pre>
 * {
 *   "exportedAt": "2026-09-08T...",
 *   "exportedBy": "analyst@faust.gov",
 *   "institution": { ... },
 *   "occupations": [ ... ],
 *   "persons": [ ... ],
 *   "appointments": [ ... ]
 * }
 * </pre>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceExportService {

    private final SteganographyService  steganographyService;
    private final InstitutionRepository institutionRepository;
    private final OccupationRepository  occupationRepository;

    /**
     * Exports institution intelligence data as a steganographic PNG image.
     *
     * @param institutionId  public UUID of the institution to export.
     * @param carrierImage   the carrier PNG image stream (your artwork).
     * @param passphrase     the encryption passphrase.
     * @param exportedByUser the analyst triggering the export (for audit log).
     * @return PNG bytes with embedded encrypted payload.
     */
    @Transactional(readOnly = true)
    public byte[] exportAsSteganographicImage(
            UUID institutionId,
            InputStream carrierImage,
            String passphrase,
            String exportedByUser
    ) throws SteganographyException, IOException {

        log.info("STEG_EXPORT: Starting export for institution {} by {}.",
                institutionId, exportedByUser);

        // 1. Load carrier image
        BufferedImage carrier = ImageIO.read(carrierImage);
        if (carrier == null) {
            throw new SteganographyException("Invalid carrier image format. Use PNG.");
        }

        // 2. Build payload
        IntelligencePayload payload = buildPayload(institutionId, exportedByUser);
        log.info("STEG_EXPORT: Payload built — {} occupations.",
                payload.occupations().size());

        // 3. Check capacity
        int capacity = steganographyService.getCapacityBytes(carrier);
        log.info("STEG_EXPORT: Carrier capacity: {} bytes. Image: {}x{}.",
                capacity, carrier.getWidth(), carrier.getHeight());

        // 4. Embed
        BufferedImage result = steganographyService.embed(carrier, payload, passphrase);

        // 5. Encode as PNG bytes
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(result, "PNG", bos);

        log.info("STEG_EXPORT: Export complete. Output size: {} bytes.", bos.size());
        return bos.toByteArray();
    }

    /**
     * Assembles the full intelligence payload for an institution.
     */
    private IntelligencePayload buildPayload(UUID institutionId, String exportedBy) {
        Institution institution = institutionRepository
                .findByExternalId(institutionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Institution not found: " + institutionId));

        var occupations = occupationRepository
                .findByInstitutionExternalIdFetched(institutionId);

        return new IntelligencePayload(
                java.time.OffsetDateTime.now().toString(),
                exportedBy,
                institution.getName(),
                institutionId.toString(),
                institution.getType().name(),
                occupations.stream().map(o -> Map.of(
                        "title",   o.getTitle(),
                        "code",    o.getCode() != null ? o.getCode() : "",
                        "vacant",  String.valueOf(o.isVacant()),
                        "category", o.getCategory().name()
                )).toList()
        );
    }
}