package com.projectfaust.traces;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

/**
 * Digital footprint trace — login, session, or authentication event
 * recorded by an online service, application, or device.
 *
 * <p>Covers: account logins (email, banking, social media, VPN),
 * API authentication events, device telemetry, and email metadata.</p>
 *
 * <p>Intelligence use-cases:
 * <ul>
 *   <li>Map a subject's online service usage pattern.</li>
 *   <li>Correlate IP addresses across services to identify shared infrastructure.</li>
 *   <li>Detect VPN/Tor usage via unusual ASN or geolocation mismatches.</li>
 *   <li>Timeline a subject's digital activity alongside physical presence (CameraTrace).</li>
 * </ul>
 * </p>
 *
 * @author Dimitri / Project Faust
 */
@Entity
@Table(
        name = "digital_traces",
        indexes = {
                @Index(name = "idx_dtrace_external_id", columnList = "external_id"),
                @Index(name = "idx_dtrace_person",      columnList = "person_id"),
                @Index(name = "idx_dtrace_observed_at", columnList = "observed_at"),
                @Index(name = "idx_dtrace_ip",          columnList = "ip_address"),
                @Index(name = "idx_dtrace_service",     columnList = "service_name"),
                @Index(name = "idx_dtrace_account",     columnList = "account_identifier")
        }
)
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DigitalTrace extends BaseTrace {

    // ── Service identification ────────────────────────────────────────────────

    /**
     * Name of the service or platform (e.g. "Gmail", "ProtonMail", "Telegram",
     * "UniCredit Online", "Tor Browser", "ExpressVPN").
     */
    @Column(name = "service_name", length = 200)
    private String serviceName;

    /**
     * The account identifier within the service — username, email address,
     * phone number used as login, or hashed user ID.
     * Never store cleartext passwords here.
     */
    @Column(name = "account_identifier", length = 500)
    private String accountIdentifier;

    /**
     * Type of digital event: LOGIN_SUCCESS, LOGIN_FAILED, LOGOUT,
     * PASSWORD_CHANGE, MFA_BYPASS, API_AUTH, SESSION_ACTIVE, etc.
     * Free-text to accommodate any service's event taxonomy.
     */
    @Column(name = "event_type", length = 100)
    private String eventType;

    // ── Network identifiers ───────────────────────────────────────────────────

    /** IPv4 or IPv6 address at time of event. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Autonomous System Number — identifies the ISP or hosting provider. */
    @Column(name = "asn", length = 20)
    private String asn;

    /** ISP or hosting provider name derived from ASN lookup. */
    @Column(name = "isp_name", length = 200)
    private String ispName;

    /** GeoIP-resolved country code (ISO 3166-1 alpha-2). */
    @Column(name = "geo_country", length = 2)
    private String geoCountry;

    /** GeoIP-resolved city. */
    @Column(name = "geo_city", length = 100)
    private String geoCity;

    // ── Device fingerprint ────────────────────────────────────────────────────

    /** Raw user-agent string from the HTTP request. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Device fingerprint hash (canvas, WebGL, font list, etc.). */
    @Column(name = "device_fingerprint", length = 128)
    private String deviceFingerprint;

    /** Operating system parsed from user-agent or telemetry. */
    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    // ── Anonymisation indicators ──────────────────────────────────────────────

    /** True if the IP is a known Tor exit node at time of event. */
    @Builder.Default
    @Column(name = "tor_exit_node")
    private boolean torExitNode = false;

    /** True if the IP belongs to a known VPN provider. */
    @Builder.Default
    @Column(name = "vpn_detected")
    private boolean vpnDetected = false;

    /** True if the IP is a known data-centre / hosting range (not residential). */
    @Builder.Default
    @Column(name = "datacenter_ip")
    private boolean datacenterIp = false;
}
