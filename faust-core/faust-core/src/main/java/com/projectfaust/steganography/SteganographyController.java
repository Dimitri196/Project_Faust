package com.projectfaust.steganography;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST controller for steganographic intelligence export.
 *
 * <p>Base path: {@code /api/v1/intelligence/export}</p>
 *
 * @author Dimitri / Project Faust
 */
@RestController
@RequestMapping("/api/v1/intelligence/export")
@RequiredArgsConstructor
@Tag(name = "Intelligence Export",
        description = "Steganographic export of intelligence data into carrier media")
public class SteganographyController {

    private final IntelligenceExportService exportService;

    /**
     * Exports institution intelligence as a steganographic PNG image.
     *
     * <p>Upload your artwork as the carrier image.
     * The encrypted payload is invisibly embedded in the pixel LSBs.
     * The output PNG looks identical to the input.</p>
     *
     * @param institutionId the institution to export.
     * @param passphrase    encryption passphrase — never stored on server.
     * @param carrier       the carrier PNG image (your artwork).
     * @return PNG file with embedded encrypted payload.
     */
    @PostMapping(value = "/image/{institutionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = "image/png")
    @PreAuthorize("hasRole('ANALYST')")
    @Operation(
            summary = "Export as steganographic PNG",
            description = "Embeds encrypted institution intelligence into a carrier PNG image. " +
                    "Output is visually identical to input. ANALYST access required."
    )
    public ResponseEntity<byte[]> exportAsImage(
            @PathVariable UUID institutionId,
            @RequestParam String passphrase,
            @RequestPart("carrier") MultipartFile carrier,
            @AuthenticationPrincipal UserDetails user
    ) {
        try {
            byte[] result = exportService.exportAsSteganographicImage(
                    institutionId,
                    carrier.getInputStream(),
                    passphrase,
                    user.getUsername()
            );

            String filename = "export_" + institutionId.toString().substring(0, 8) + ".png";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(result);

        } catch (SteganographyException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}