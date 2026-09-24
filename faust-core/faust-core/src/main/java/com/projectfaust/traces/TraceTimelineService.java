package com.projectfaust.traces;

import com.projectfaust.person.Person;
import com.projectfaust.person.PersonRepository;
import com.projectfaust.traces.dto.response.*;
import com.projectfaust.traces.mapper.*;
import com.projectfaust.traces.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

/**
 * Aggregates all five trace subtypes for a person into a unified chronological
 * timeline — the primary data feed for the analyst dossier view.
 *
 * <p>Each event is normalised into a {@link TraceTimelineResponse.TimelineEntry}
 * with a human-readable {@code summary} string (for quick-scan display) and the
 * full typed payload attached for detail view.</p>
 *
 * <p>Entry order: ascending by {@code observedAt} so the timeline reads
 * chronologically from top (oldest) to bottom (newest).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceTimelineService {

    private final PersonRepository            personRepository;

    private final DigitalTraceRepository      digitalRepo;
    private final FinancialTraceRepository    financialRepo;
    private final CameraTraceRepository       cameraRepo;
    private final TelcoTraceRepository        telcoRepo;
    private final SurveillanceEventRepository surveillanceRepo;

    private final DigitalTraceMapper          digitalMapper;
    private final FinancialTraceMapper        financialMapper;
    private final CameraTraceMapper           cameraMapper;
    private final TelcoTraceMapper            telcoMapper;
    private final SurveillanceEventMapper     surveillanceMapper;

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns the full timeline for a person — all trace types, all time.
     */
    public TraceTimelineResponse getTimeline(UUID personPublicId) {
        return buildTimeline(personPublicId, null, null);
    }

    /**
     * Returns the timeline for a person within a date range.
     */
    public TraceTimelineResponse getTimeline(UUID personPublicId,
                                             LocalDateTime from,
                                             LocalDateTime to) {
        return buildTimeline(personPublicId, from, to);
    }

    // ── Core aggregation ───────────────────────────────────────────────────────

    private TraceTimelineResponse buildTimeline(UUID personPublicId,
                                                LocalDateTime from,
                                                LocalDateTime to) {
        log.debug("FAUST_TIMELINE_BUILD person={} from={} to={}", personPublicId, from, to);

        Person person = personRepository.findByExternalId(personPublicId)
                .orElseThrow(() -> new NoSuchElementException("Person not found: " + personPublicId));

        // ── Fetch all types ───────────────────────────────────────────────────

        List<DigitalTrace>     digitals    = rangedOrAll(
                () -> digitalRepo.findAllByPersonExternalId(personPublicId),
                () -> digitalRepo.findByPersonAndDateRange(personPublicId, from, to),
                from);

        List<FinancialTrace>   financials  = rangedOrAll(
                () -> financialRepo.findAllByPersonExternalId(personPublicId),
                () -> financialRepo.findByPersonAndDateRange(personPublicId, from, to),
                from);

        List<CameraTrace>      cameras     = rangedOrAll(
                () -> cameraRepo.findAllByPersonExternalId(personPublicId),
                () -> cameraRepo.findByPersonAndDateRange(personPublicId, from, to),
                from);

        List<TelcoTrace>       telcos      = rangedOrAll(
                () -> telcoRepo.findAllByPersonExternalId(personPublicId),
                () -> telcoRepo.findByPersonAndDateRange(personPublicId, from, to),
                from);

        List<SurveillanceEvent> survEvents = rangedOrAll(
                () -> surveillanceRepo.findAllByPersonExternalId(personPublicId),
                () -> surveillanceRepo.findByPersonAndDateRange(personPublicId, from, to),
                from);

        // ── Map each subtype to TimelineEntry ─────────────────────────────────

        List<TraceTimelineResponse.TimelineEntry> entries = Stream.of(
                digitals.stream()   .map(this::toEntry),
                financials.stream() .map(this::toEntry),
                cameras.stream()    .map(this::toEntry),
                telcos.stream()     .map(this::toEntry),
                survEvents.stream() .map(this::toEntry)
        ).flatMap(s -> s)
         .sorted(Comparator.comparing(TraceTimelineResponse.TimelineEntry::observedAt))
         .toList();

        // ── Aggregate stats ───────────────────────────────────────────────────

        Map<String, Integer> countByType = new LinkedHashMap<>();
        countByType.put("DIGITAL",     digitals.size());
        countByType.put("FINANCIAL",   financials.size());
        countByType.put("CAMERA",      cameras.size());
        countByType.put("TELCO",       telcos.size());
        countByType.put("SURVEILLANCE", survEvents.size());

        Map<String, Integer> countByCountry = new TreeMap<>();
        entries.stream()
               .filter(e -> e.country() != null)
               .forEach(e -> countByCountry.merge(e.country(), 1, Integer::sum));

        LocalDateTime firstAt = entries.isEmpty() ? null : entries.getFirst().observedAt();
        LocalDateTime lastAt  = entries.isEmpty() ? null : entries.getLast().observedAt();

        return new TraceTimelineResponse(
                personPublicId,
                person.getFullName(),
                firstAt,
                lastAt,
                entries.size(),
                countByType,
                countByCountry,
                entries
        );
    }

    // ── Subtype → TimelineEntry conversions ───────────────────────────────────

    private TraceTimelineResponse.TimelineEntry toEntry(DigitalTrace t) {
        DigitalTraceResponse payload = digitalMapper.toResponse(t);
        String summary = buildDigitalSummary(t);
        return new TraceTimelineResponse.TimelineEntry(
                t.getExternalId(), "DIGITAL", t.getObservedAt(),
                t.getSourceType(), t.getConfidence(), t.isFlagged(),
                summary, null, null, t.getGeoCountry(), t.getGeoCity(), payload);
    }

    private TraceTimelineResponse.TimelineEntry toEntry(FinancialTrace t) {
        FinancialTraceResponse payload = financialMapper.toResponse(t);
        String summary = buildFinancialSummary(t);
        return new TraceTimelineResponse.TimelineEntry(
                t.getExternalId(), "FINANCIAL", t.getObservedAt(),
                t.getSourceType(), t.getConfidence(), t.isFlagged(),
                summary, t.getLatitude(), t.getLongitude(),
                t.getMerchantCountry(), t.getMerchantCity(), payload);
    }

    private TraceTimelineResponse.TimelineEntry toEntry(CameraTrace t) {
        CameraTraceResponse payload = cameraMapper.toResponse(t);
        String summary = buildCameraSummary(t);
        return new TraceTimelineResponse.TimelineEntry(
                t.getExternalId(), "CAMERA", t.getObservedAt(),
                t.getSourceType(), t.getConfidence(), t.isFlagged(),
                summary, t.getLatitude(), t.getLongitude(),
                t.getCountry(), t.getCity(), payload);
    }

    private TraceTimelineResponse.TimelineEntry toEntry(TelcoTrace t) {
        TelcoTraceResponse payload = telcoMapper.toResponse(t);
        String summary = buildTelcoSummary(t);
        return new TraceTimelineResponse.TimelineEntry(
                t.getExternalId(), "TELCO", t.getObservedAt(),
                t.getSourceType(), t.getConfidence(), t.isFlagged(),
                summary, t.getCellLatitude(), t.getCellLongitude(),
                t.getCountry(), t.getCity(), payload);
    }

    private TraceTimelineResponse.TimelineEntry toEntry(SurveillanceEvent t) {
        SurveillanceEventResponse payload = surveillanceMapper.toResponse(t);
        String summary = buildSurveillanceSummary(t);
        return new TraceTimelineResponse.TimelineEntry(
                t.getExternalId(), "SURVEILLANCE", t.getObservedAt(),
                t.getSourceType(), t.getConfidence(), t.isFlagged(),
                summary, t.getLatitude(), t.getLongitude(),
                t.getCountry(), t.getCity(), payload);
    }

    // ── Human-readable summary builders ───────────────────────────────────────

    private String buildDigitalSummary(DigitalTrace t) {
        StringBuilder sb = new StringBuilder();
        if (t.getServiceName() != null)       sb.append(t.getServiceName()).append(" · ");
        if (t.getEventType() != null)         sb.append(t.getEventType()).append(" · ");
        if (t.getIpAddress() != null)         sb.append("IP ").append(t.getIpAddress());
        if (t.isTorExitNode())                sb.append(" [TOR]");
        if (t.isVpnDetected())                sb.append(" [VPN]");
        if (t.getGeoCountry() != null)        sb.append(" · ").append(t.getGeoCountry());
        return sb.toString().strip().replaceAll("^·\\s*|\\s*·\\s*$", "");
    }

    private String buildFinancialSummary(FinancialTrace t) {
        StringBuilder sb = new StringBuilder();
        if (t.getTransactionType() != null) sb.append(t.getTransactionType()).append(" · ");
        if (t.getMerchantName()    != null) sb.append(t.getMerchantName()).append(" · ");
        if (t.getMerchantCity()    != null) sb.append(t.getMerchantCity()).append(" · ");
        if (t.getAmount()          != null && t.getCurrency() != null)
            sb.append(t.getAmount().toPlainString()).append(" ").append(t.getCurrency());
        if (t.isAmlFlagged())               sb.append(" [AML]");
        return sb.toString().strip().replaceAll("^·\\s*|\\s*·\\s*$", "");
    }

    private String buildCameraSummary(CameraTrace t) {
        StringBuilder sb = new StringBuilder();
        if (t.getSourceType() != null)        sb.append(t.getSourceType().name()).append(" · ");
        if (t.getLocationName() != null)      sb.append(t.getLocationName()).append(" · ");
        if (t.getCity() != null)              sb.append(t.getCity());
        if (t.getFacialMatchScore() != null)
            sb.append(String.format(" · face=%.0f%%", t.getFacialMatchScore() * 100));
        if (t.getAnprPlateRaw() != null)      sb.append(" · plate=").append(t.getAnprPlateRaw());
        return sb.toString().strip().replaceAll("^·\\s*|\\s*·\\s*$", "");
    }

    private String buildTelcoSummary(TelcoTrace t) {
        StringBuilder sb = new StringBuilder();
        if (t.getRatType()       != null) sb.append(t.getRatType()).append(" · ");
        if (t.getOperatorName()  != null) sb.append(t.getOperatorName()).append(" · ");
        if (t.getCity()          != null) sb.append(t.getCity()).append(", ");
        if (t.getCountry()       != null) sb.append(t.getCountry());
        if (t.isActiveIntercept())        sb.append(" [INTERCEPT]");
        if (t.getCellRadiusMeters() != null)
            sb.append(String.format(" · ±%d m", t.getCellRadiusMeters()));
        return sb.toString().strip().replaceAll("^·\\s*|\\s*·\\s*$", "");
    }

    private String buildSurveillanceSummary(SurveillanceEvent t) {
        StringBuilder sb = new StringBuilder();
        if (t.getEventType()    != null) sb.append(t.getEventType().name()).append(" · ");
        if (t.getLocationName() != null) sb.append(t.getLocationName()).append(" · ");
        if (t.getCity()         != null) sb.append(t.getCity());
        int participants = t.getMeetingParticipants() == null ? 0 : t.getMeetingParticipants().size();
        if (participants > 0) sb.append(String.format(" · %d participants", participants));
        if (t.isPhotoEvidence())         sb.append(" [PHOTO]");
        if (t.isAvRecording())           sb.append(" [A/V]");
        return sb.toString().strip().replaceAll("^·\\s*|\\s*·\\s*$", "");
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Supplier<T> { T get(); }

    private <T> List<T> rangedOrAll(Supplier<List<T>> allQuery,
                                    Supplier<List<T>> rangedQuery,
                                    LocalDateTime from) {
        return (from != null) ? rangedQuery.get() : allQuery.get();
    }
}
