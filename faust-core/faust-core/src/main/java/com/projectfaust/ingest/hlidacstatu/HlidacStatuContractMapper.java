package com.projectfaust.ingest.hlidacstatu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectfaust.ingest.core.ContractSourceMapper;
import com.projectfaust.ingest.core.ExternalContract;
import com.projectfaust.ingest.dto.HlidacContractDto;
import com.projectfaust.shared.enums.SourceSystem;
import com.projectfaust.shared.enums.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Maps raw {@link HlidacContractDto} records from the Hlidač Státu API
 * to the normalised {@link ExternalContract} entity.
 *
 * <p>Implements {@link ContractSourceMapper} for {@link SourceSystem#HLIDAC_STATU_CZ}.
 * Registered as a Spring {@code @Component} — {@link com.projectfaust.ingest.core.ContractIngestService}
 * picks it up automatically via its mapper registry.</p>
 *
 * <p><b>Field mapping quirks handled here:</b></p>
 * <ul>
 *   <li>Price: multiple historic field names covered by {@code @JsonAlias}
 *       on the DTO; this mapper handles null/zero gracefully.</li>
 *   <li>Date: Hlidač Státu uses ISO-8601 datetime strings
 *       ({@code "2023-04-15T00:00:00"}) — parsed to {@link LocalDate}.</li>
 *   <li>Supplier: the API returns a list; only the first supplier is mapped
 *       to {@code ExternalContract.supplierIco/supplierName} — additional
 *       suppliers are preserved in {@code rawJsonData} for re-processing.</li>
 *   <li>Parties fallback: when {@code buyer} or {@code suppliers} are null,
 *       the mapper attempts to resolve them from {@code allParties}.</li>
 * </ul>
 *
 * <p>All new records default to {@link VerificationStatus#UNVERIFIED} —
 * Hlidač Státu is a civic initiative, not an official state register.
 * Records require analyst confirmation before trust is elevated.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HlidacStatuContractMapper implements ContractSourceMapper<HlidacContractDto> {

    private final ObjectMapper objectMapper;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    @Override
    public SourceSystem supports() {
        return SourceSystem.HLIDAC_STATU_CZ;
    }

    @Override
    public ExternalContract toEntity(HlidacContractDto source) {
        HlidacContractDto.InstitutionInfo buyer = resolveBuyer(source);
        HlidacContractDto.InstitutionInfo supplier = resolveSupplier(source);
        String rawJson = serializeRaw(source);

        return ExternalContract.builder()
                .sourceSystem(SourceSystem.HLIDAC_STATU_CZ)
                .externalId(source.id())
                .buyerIco(buyer != null ? buyer.ico() : null)
                .buyerName(buyer != null ? buyer.name() : null)
                .supplierIco(supplier != null ? supplier.ico() : null)
                .supplierName(supplier != null ? supplier.name() : null)
                .amountTotal(parsePriceSafely(source.priceWithoutVat()))
                .contractDate(parseDateSafely(source.dateConfirmed()))
                .subjectText(source.subject())
                .rawJsonData(rawJson)
                .verificationStatus(VerificationStatus.UNVERIFIED)
                .build();
    }

    // -------------------------------------------------------------------------
    // Resolution helpers
    // -------------------------------------------------------------------------

    private HlidacContractDto.InstitutionInfo resolveBuyer(HlidacContractDto source) {
        if (source.buyer() != null && source.buyer().ico() != null) {
            return source.buyer();
        }
        if (source.allParties() != null && !source.allParties().isEmpty()) {
            log.debug("FAUST_MAPPER: Falling back to allParties[0] for buyer on contract {}",
                    source.id());
            return source.allParties().getFirst();
        }
        log.warn("FAUST_MAPPER: No buyer found for contract {} — buyerIco will be null.",
                source.id());
        return null;
    }

    private HlidacContractDto.InstitutionInfo resolveSupplier(HlidacContractDto source) {
        if (source.suppliers() != null && !source.suppliers().isEmpty()) {
            if (source.suppliers().size() > 1) {
                log.debug("FAUST_MAPPER: Contract {} has {} suppliers — " +
                                "only the first is mapped; remainder preserved in rawJsonData.",
                        source.id(), source.suppliers().size());
            }
            return source.suppliers().getFirst();
        }
        if (source.allParties() != null && source.allParties().size() > 1) {
            log.debug("FAUST_MAPPER: Falling back to allParties[1] for supplier on contract {}",
                    source.id());
            return source.allParties().get(1);
        }
        log.warn("FAUST_MAPPER: No supplier found for contract {} — supplierIco will be null.",
                source.id());
        return null;
    }

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    private BigDecimal parsePriceSafely(Double price) {
        if (price == null || price <= 0) return null;
        try {
            return BigDecimal.valueOf(price);
        } catch (NumberFormatException e) {
            log.warn("FAUST_MAPPER: Could not parse price value: {}", price);
            return null;
        }
    }

    private LocalDate parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateStr, fmt);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        log.warn("FAUST_MAPPER: Could not parse date '{}' — contractDate will be null.", dateStr);
        return null;
    }

    private String serializeRaw(HlidacContractDto source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (Exception e) {
            log.warn("FAUST_MAPPER: Could not serialize raw DTO for contract {}.", source.id());
            return null;
        }
    }
}