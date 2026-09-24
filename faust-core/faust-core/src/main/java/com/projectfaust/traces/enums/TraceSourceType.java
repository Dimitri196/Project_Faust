package com.projectfaust.traces.enums;

/**
 * Classifies the technical or human source that produced a trace record.
 *
 * <p>Used across all trace subtypes as the top-level provenance discriminator.
 * Determines which analyst clearance level is required to view the record
 * and which ingest pipeline produced it.</p>
 *
 * @author Dimitri / Project Faust
 */
public enum TraceSourceType {

    // ── Digital / cyber ───────────────────────────────────────────────────────

    /** Login, session, or authentication event from an online service. */
    DIGITAL_LOGIN,

    /** API call, app session, or device telemetry record. */
    DIGITAL_SESSION,

    /** Email metadata (headers, routing — not content). */
    EMAIL_METADATA,

    // ── Financial ─────────────────────────────────────────────────────────────

    /** POS / ATM card transaction from a payment processor feed. */
    CARD_TRANSACTION,

    /** Wire transfer or SEPA record. */
    BANK_TRANSFER,

    /** Cash withdrawal or deposit at a branch or ATM. */
    CASH_OPERATION,

    // ── Camera / biometric ────────────────────────────────────────────────────

    /** Closed-circuit television — fixed or mobile camera. */
    CCTV,

    /** Automatic Number Plate Recognition camera hit. */
    ANPR,

    /** Facial recognition match from any camera source. */
    FACIAL_RECOGNITION,

    // ── Telco / radio ─────────────────────────────────────────────────────────

    /** GSM/UMTS/LTE/5G base transceiver station registration. */
    MOBILE_BTS,

    /** IMSI catcher / Stingray intercept. */
    IMSI_CATCHER,

    /** Wi-Fi access point association log. */
    WIFI_AP,

    /** Satellite phone or Thuraya/Iridium registration. */
    SATELLITE_PHONE,

    // ── Operative / human ─────────────────────────────────────────────────────

    /** Physical observation by a human intelligence asset. */
    HUMAN_ASSET,

    /** Drone or aerial platform observation. */
    DRONE,

    /** Satellite imagery analysis. */
    SATELLITE_IMAGERY,

    /** Report from a partner intelligence or law enforcement agency. */
    PARTNER_AGENCY,

    /** Open-source intelligence (social media, news, public records). */
    OPEN_SOURCE
}
