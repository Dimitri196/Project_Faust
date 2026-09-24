package com.projectfaust.ingest.ted;

import com.projectfaust.ingest.core.ContractSourceMapper;
import com.projectfaust.ingest.core.ExternalContract;
import com.projectfaust.ingest.dto.TedNoticeDto;
import com.projectfaust.shared.enums.SourceSystem;
import com.projectfaust.shared.enums.VerificationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Maps raw {@link TedNoticeDto} records from the TED (Tenders Electronic Daily)
 * API v3 to the normalised {@link ExternalContract} entity.
 *
 * <p>Implements {@link ContractSourceMapper} for {@link SourceSystem#TED_EU}.
 * Registered as a Spring {@code @Component} — {@link com.projectfaust.ingest.core.ContractIngestService}
 * picks it up automatically via its mapper registry.</p>
 *
 * <p><b>TED-specific mapping notes:</b></p>
 * <ul>
 *   <li><b>Notice types:</b> TED publishes two relevant notice types —
 *       Contract Notices ({@code cn-standard}: tender announcements, no supplier yet)
 *       and Contract Award Notices ({@code can-standard}: awarded contracts with
 *       supplier data). Both are mapped; supplier fields are null for cn-standard.</li>
 *   <li><b>Buyer national ID:</b> TED's {@code national-id} field contains the
 *       buyer's registration number in the issuing country's format (IČO for CZ).
 *       This maps directly to {@code ExternalContract.buyerIco}, enabling the
 *       same ICO-based linkage used for Hlidač Státu data.</li>
 *   <li><b>Country codes:</b> TED uses ISO 3166-1 alpha-3 codes ("CZE") in
 *       some fields and alpha-2 ("CZ") in others. The mapper normalises to
 *       alpha-2 where needed for consistency with the identifier table.</li>
 *   <li><b>Value:</b> mapped from {@code award-outcome[0].awarded-value} on
 *       award notices, falling back to {@code notice-value} on tender notices.</li>
 *   <li><b>Multiple lots:</b> TED contracts may have multiple award outcomes
 *       (one per lot). Only the first awarded supplier is mapped to
 *       {@code supplierIco/supplierName} — the full raw JSON preserves all.</li>
 * </ul>
 *
 * <p>All records default to {@link VerificationStatus#OFFICIAL_REGISTRY} —
 * TED is the EU's official procurement journal, making this the highest-trust
 * ingest source in the current pipeline.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TedContractMapper implements ContractSourceMapper<TedNoticeDto> {

    @Override
    public SourceSystem supports() {
        return SourceSystem.TED_EU;
    }

    @Override
    public ExternalContract toEntity(TedNoticeDto source) {
        // Resolve primary buyer — TED notices can technically list multiple
        // buyers (joint procurement), but the first is always the lead authority.
        TedNoticeDto.BuyerDto buyer = resolvePrimaryBuyer(source);

        // Resolve supplier — only present on award notices (can-standard).
        // Null on contract notices (cn-standard) — position not yet awarded.
        TedNoticeDto.SupplierDto supplier = resolvePrimarySupplier(source);

        // Value — prefer awarded value from first award outcome, fall back to
        // the notice-level estimated value on tender notices.
        BigDecimal amount = resolveAmount(source);

        return ExternalContract.builder()
                .sourceSystem(SourceSystem.TED_EU)
                .externalId(source.publicationNumber())
                .buyerIco(buyer != null ? buyer.nationalId() : null)
                .buyerName(buyer != null ? buyer.name() : null)
                .supplierIco(supplier != null ? supplier.nationalId() : null)
                .supplierName(supplier != null ? supplier.name() : null)
                .amountTotal(amount)
                .contractDate(parseDateSafely(source.publicationDate()))
                .subjectText(source.shortDescription())
                .rawJsonData(null) // serialised by TedContractFetchService before mapping
                // TED is the EU's official procurement journal — OFFICIAL_REGISTRY
                // trust level, highest in the pipeline (vs UNVERIFIED for Hlidač Státu).
                .verificationStatus(VerificationStatus.OFFICIAL_REGISTRY)
                .build();
    }

    // -------------------------------------------------------------------------
    // Resolution helpers
    // -------------------------------------------------------------------------

    private TedNoticeDto.BuyerDto resolvePrimaryBuyer(TedNoticeDto source) {
        if (source.buyers() == null || source.buyers().isEmpty()) {
            log.warn("TED_MAPPER: No buyer found on notice {} — buyerIco will be null.",
                    source.publicationNumber());
            return null;
        }
        if (source.buyers().size() > 1) {
            log.debug("TED_MAPPER: Notice {} has {} buyers (joint procurement) — " +
                            "only lead buyer mapped; remainder in rawJsonData.",
                    source.publicationNumber(), source.buyers().size());
        }
        return source.buyers().getFirst();
    }

    private TedNoticeDto.SupplierDto resolvePrimarySupplier(TedNoticeDto source) {
        if (source.awardOutcomes() == null || source.awardOutcomes().isEmpty()) {
            // Contract notice (cn-standard) — no supplier yet, this is expected.
            return null;
        }

        TedNoticeDto.AwardOutcomeDto firstAward = source.awardOutcomes().getFirst();
        if (firstAward.suppliers() == null || firstAward.suppliers().isEmpty()) {
            log.debug("TED_MAPPER: Award outcome on notice {} has no supplier data.",
                    source.publicationNumber());
            return null;
        }

        if (source.awardOutcomes().size() > 1) {
            log.debug("TED_MAPPER: Notice {} has {} award outcomes (multi-lot) — " +
                            "only first lot's supplier mapped; remainder in rawJsonData.",
                    source.publicationNumber(), source.awardOutcomes().size());
        }

        return firstAward.suppliers().getFirst();
    }

    private BigDecimal resolveAmount(TedNoticeDto source) {
        // Prefer awarded value from first award outcome (actual final value)
        if (source.awardOutcomes() != null && !source.awardOutcomes().isEmpty()) {
            TedNoticeDto.AwardOutcomeDto firstAward = source.awardOutcomes().getFirst();
            if (firstAward.awardedValue() != null && firstAward.awardedValue().amount() != null) {
                return parsePriceSafely(firstAward.awardedValue().amount(),
                        source.publicationNumber(), "awarded-value");
            }
        }

        // Fall back to notice-level estimated value (tender notices)
        if (source.noticeValue() != null && source.noticeValue().amount() != null) {
            return parsePriceSafely(source.noticeValue().amount(),
                    source.publicationNumber(), "notice-value");
        }

        return null;
    }

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    private BigDecimal parsePriceSafely(Double amount, String noticeId, String field) {
        if (amount == null || amount <= 0) return null;
        try {
            return BigDecimal.valueOf(amount);
        } catch (NumberFormatException e) {
            log.warn("TED_MAPPER: Could not parse {} on notice {}: {}", field, noticeId, amount);
            return null;
        }
    }

    private LocalDate parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            // TED uses ISO-8601 date format (yyyy-MM-dd) consistently
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("TED_MAPPER: Could not parse date '{}' — contractDate will be null.", dateStr);
            return null;
        }
    }
}