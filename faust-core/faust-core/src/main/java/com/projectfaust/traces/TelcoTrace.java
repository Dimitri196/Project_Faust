package com.projectfaust.traces;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

/**
 * Telecommunications trace — a mobile device registration event at a
 * base transceiver station (BTS), Wi-Fi access point, or satellite phone network.
 *
 * <p>Each row represents a single registration or handover event: the moment
 * a device associated with a cell tower, AP, or satellite node, pinpointing
 * the device (and by attribution, its owner) to within the cell's coverage area.</p>
 *
 * <p>Covers: GSM/UMTS/LTE/5G BTS logs, IMSI catcher intercepts, Wi-Fi AP
 * association logs, and satellite phone registrations.</p>
 *
 * <p>Intelligence use-cases:
 * <ul>
 *   <li>Place a subject in a geographic area without camera corroboration.</li>
 *   <li>Correlate IMSI/IMEI across operators to de-anonymise SIM swaps.</li>
 *   <li>Detect burner phone patterns: IMEI constant, IMSI changes frequently.</li>
 *   <li>Map cell tower registrations to reconstruct a daily movement pattern.</li>
 *   <li>Cross-correlate two subjects at the same BTS within the same time window
 *       to infer a meeting even without camera evidence.</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "telco_traces",
        indexes = {
                @Index(name = "idx_ttrace_external_id", columnList = "external_id"),
                @Index(name = "idx_ttrace_person",      columnList = "person_id"),
                @Index(name = "idx_ttrace_observed_at", columnList = "observed_at"),
                @Index(name = "idx_ttrace_imsi",        columnList = "imsi"),
                @Index(name = "idx_ttrace_imei",        columnList = "imei"),
                @Index(name = "idx_ttrace_cell_id",     columnList = "cell_id"),
                @Index(name = "idx_ttrace_operator",    columnList = "operator_code")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TelcoTrace extends BaseTrace {

    // ── Device identifiers ────────────────────────────────────────────────────

    /**
     * International Mobile Subscriber Identity — identifies the SIM card.
     * 15-digit string. Key for correlating across networks and detecting SIM swaps.
     */
    @Column(name = "imsi", length = 15)
    private String imsi;

    /**
     * International Mobile Equipment Identity — identifies the physical handset.
     * 15-digit string. Key for detecting "burner" behaviour (same IMEI, multiple IMSIs).
     */
    @Column(name = "imei", length = 15)
    private String imei;

    /**
     * Mobile Subscriber Integrated Services Digital Network Number —
     * the phone number associated with this SIM at time of event.
     * International format, e.g. "+420603123456".
     */
    @Column(name = "msisdn", length = 20)
    private String msisdn;

    // ── Network node ──────────────────────────────────────────────────────────

    /**
     * Mobile Country Code + Mobile Network Code (e.g. "23001" = T-Mobile CZ).
     */
    @Column(name = "operator_code", length = 6)
    private String operatorCode;

    /** Human-readable operator name. */
    @Column(name = "operator_name", length = 100)
    private String operatorName;

    /**
     * Radio access technology: GSM, UMTS, LTE, NR (5G), WIFI, SATELLITE.
     */
    @Column(name = "rat_type", length = 20)
    private String ratType;

    /**
     * Cell / node identifier in the operator's network.
     * Format: MCC-MNC-LAC-CID for GSM/UMTS, or CGI/ECGI for LTE/5G.
     */
    @Column(name = "cell_id", length = 50)
    private String cellId;

    /** Location Area Code (GSM/UMTS) or Tracking Area Code (LTE/5G). */
    @Column(name = "area_code", length = 10)
    private String areaCode;

    // ── Physical location of the node ─────────────────────────────────────────

    /** GPS latitude of the BTS/AP antenna. */
    @Column(name = "cell_latitude")
    private Double cellLatitude;

    /** GPS longitude of the BTS/AP antenna. */
    @Column(name = "cell_longitude")
    private Double cellLongitude;

    /**
     * Estimated coverage radius of this cell in metres.
     * Urban microcells: ~200 m. Rural macrocells: up to 35 km.
     * Used to derive the uncertainty radius for location estimation.
     */
    @Column(name = "cell_radius_meters")
    private Integer cellRadiusMeters;

    @Column(name = "cell_address", length = 300)
    private String cellAddress;

    /** ISO 3166-1 alpha-2 country of the BTS. */
    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    // ── Signal quality ────────────────────────────────────────────────────────

    /** Received Signal Strength Indicator in dBm (negative value, higher = better). */
    @Column(name = "rssi_dbm")
    private Integer rssiDbm;

    // ── Event classification ──────────────────────────────────────────────────

    /**
     * Event type as reported by the operator or IMSI catcher:
     * REGISTRATION, HANDOVER, CALL_SETUP, SMS_SEND, DATA_SESSION,
     * LOCATION_UPDATE, IMSI_CATCH, WIFI_ASSOC, WIFI_AUTH, etc.
     */
    @Column(name = "event_type", length = 50)
    private String eventType;

    /** True if this record was produced by an IMSI catcher / active intercept device. */
    @Builder.Default
    @Column(name = "active_intercept")
    private boolean activeIntercept = false;

    /**
     * Duration of the session/call in seconds, if applicable.
     * Null for instantaneous events (SMS, registration).
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
}
