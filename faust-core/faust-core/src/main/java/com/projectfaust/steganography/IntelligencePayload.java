package com.projectfaust.steganography;

import java.util.List;
import java.util.Map;

/**
 * Serializable payload embedded in steganographic carrier.
 * All fields are plain types for clean JSON serialization.
 *
 * @author Dimitri / Project Faust
 */
public record IntelligencePayload(
        String exportedAt,
        String exportedBy,
        String institutionName,
        String institutionId,
        String institutionType,
        List<Map<String, String>> occupations
) {}