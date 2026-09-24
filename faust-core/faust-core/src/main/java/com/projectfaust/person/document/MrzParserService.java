package com.projectfaust.person.document;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Parser for ICAO 9303 Machine Readable Zone (MRZ) strings.
 *
 * <p>Supports:
 * <ul>
 *   <li>TD1 — ID cards (3 lines × 30 chars)</li>
 *   <li>TD3 — Passports (2 lines × 44 chars)</li>
 * </ul>
 *
 * <p>MRZ is found at the bottom of travel documents and identity cards.
 * It encodes: document type, issuing state, surname, given names,
 * document number, nationality, date of birth, sex, expiry date.
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
public class MrzParserService {

    /**
     * Parses a raw MRZ string and returns structured data.
     *
     * @param mrz raw MRZ string (lines concatenated, no spaces).
     * @return parsed result or empty if MRZ is invalid.
     */
    public Optional<MrzResult> parse(String mrz) {
        if (mrz == null) return Optional.empty();

        String clean = mrz.toUpperCase().replaceAll("[\\s\\n]", "");

        try {
            if (clean.length() == 88) return parseTd3(clean); // Passport
            if (clean.length() == 90) return parseTd1(clean); // ID card
            log.warn("MRZ: Unknown format length {}.", clean.length());
            return Optional.empty();
        } catch (Exception e) {
            log.error("MRZ: Parse error — {}", e.getMessage());
            return Optional.empty();
        }
    }

    // ── TD3 — Passport (2 × 44) ───────────────────────────────────────────────

    private Optional<MrzResult> parseTd3(String mrz) {
        String line1 = mrz.substring(0, 44);
        String line2 = mrz.substring(44, 88);

        String docType       = line1.substring(0, 2).replace("<", "").trim();
        String issuingState  = line1.substring(2, 5).replace("<", "").trim();
        String names         = line1.substring(5, 44);
        String[] nameParts   = names.split("<<");
        String surname       = nameParts[0].replace("<", " ").trim();
        String givenNames    = nameParts.length > 1
                ? nameParts[1].replace("<", " ").trim() : "";

        String docNumber     = line2.substring(0, 9).replace("<", "").trim();
        String nationality   = line2.substring(10, 13).replace("<", "").trim();
        String dobRaw        = line2.substring(13, 19);
        String sex           = line2.substring(20, 21);
        String expiryRaw     = line2.substring(21, 27);

        return Optional.of(new MrzResult(
                docType, issuingState, docNumber,
                surname, givenNames, nationality,
                parseDate(dobRaw, true),
                parseDate(expiryRaw, false),
                sex, MrzFormat.TD3
        ));
    }

    // ── TD1 — ID Card (3 × 30) ───────────────────────────────────────────────

    private Optional<MrzResult> parseTd1(String mrz) {
        String line1 = mrz.substring(0, 30);
        String line2 = mrz.substring(30, 60);
        String line3 = mrz.substring(60, 90);

        String docType      = line1.substring(0, 2).replace("<", "").trim();
        String issuingState = line1.substring(2, 5).replace("<", "").trim();
        String docNumber    = line1.substring(5, 14).replace("<", "").trim();

        String dobRaw       = line2.substring(0, 6);
        String sex          = line2.substring(7, 8);
        String expiryRaw    = line2.substring(8, 14);
        String nationality  = line2.substring(15, 18).replace("<", "").trim();

        String names        = line3;
        String[] nameParts  = names.split("<<");
        String surname      = nameParts[0].replace("<", " ").trim();
        String givenNames   = nameParts.length > 1
                ? nameParts[1].replace("<", " ").trim() : "";

        return Optional.of(new MrzResult(
                docType, issuingState, docNumber,
                surname, givenNames, nationality,
                parseDate(dobRaw, true),
                parseDate(expiryRaw, false),
                sex, MrzFormat.TD1
        ));
    }

    // ── Date parsing ──────────────────────────────────────────────────────────

    private LocalDate parseDate(String yymmdd, boolean isBirthDate) {
        if (yymmdd == null || yymmdd.length() < 6) return null;
        try {
            int yy = Integer.parseInt(yymmdd.substring(0, 2));
            int mm = Integer.parseInt(yymmdd.substring(2, 4));
            int dd = Integer.parseInt(yymmdd.substring(4, 6));

            // ICAO century heuristic:
            // Birth dates: YY >= current year's last 2 digits → 1900s
            // Expiry dates: always future → 2000s
            int currentYY = LocalDate.now().getYear() % 100;
            int century = isBirthDate
                    ? (yy > currentYY ? 1900 : 2000)
                    : 2000;

            return LocalDate.of(century + yy, mm, dd);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Result types ──────────────────────────────────────────────────────────

    public enum MrzFormat { TD1, TD3 }

    /**
     * Structured data extracted from an MRZ string.
     */
    public record MrzResult(
            String documentType,
            String issuingState,
            String documentNumber,
            String surname,
            String givenNames,
            String nationality,
            LocalDate dateOfBirth,
            LocalDate expiryDate,
            String sex,
            MrzFormat format
    ) {
        public String fullName() {
            return (givenNames + " " + surname).trim();
        }
    }
}
