package com.projectfaust.entity.enums;

/**
 * Classifies the technical protocol, architecture, and operational layer of a communication vector.
 * <p>
 * This enum maps structural communication endpoints into specific operational domains (SIGINT, OSINT, CYBER).
 * It enables the link-analysis engine to differentiate between open public infrastructure, commercially
 * secure protocols, and highly specialized covert or military transmission systems.
 * </p>
 *
 * @author Dimitri
 */
public enum ContactType {

    // =========================================================================
    // STANDARD TELEPHONY & CELLULAR (SIGINT / TELECOM TARGETING)
    // =========================================================================

    /**
     * Standard voice/SMS cellular endpoint using traditional GSM/UMTS/LTE/5G public switching networks.
     * Tied directly to an IMSI (Subscriber Identity) and MSISDN (Phone Number). Highly vulnerable to cell-tower dumping.
     */
    CELLULAR_GSM,

    /**
     * Voice over IP endpoint running over standard SIP/H.323 protocols (e.g., Skype for Business, corporate softphones, virtual numbers).
     * Frequently routed through unencrypted internet gateways or corporate proxies.
     */
    VOIP,

    /**
     * Dedicated, non-cellular hardware terminal utilizing low-Earth orbit or geostationary satellite constellations
     * (e.g., Inmarsat, Thuraya, Iridium). Typically used in remote operations or to bypass national telecom overrides.
     */
    SATELLITE_TERMINAL,

    // =========================================================================
    // ENCRYPTED MESSAGING & DECENTRALIZED PROTOCOLS
    // =========================================================================

    /**
     * Swiss-based, zero-knowledge encrypted messaging platform popular among high-OPSEC targets
     * due to its separation from phone numbers (uses an 8-character random ID).
     */
    THREEMA,

    /**
     * Signal Protocol-based communication handle. End-to-end encrypted, but traditionally verified
     * by phone numbers, offering strong content privacy but weaker metadata anonymity.
     */
    SIGNAL,

    /**
     * Matrix-based federated network endpoint (e.g., Element client accounts). Fully decentralized,
     * customizable, and often self-hosted by operational cells to prevent third-party server intercepts.
     */
    MATRIX_IDENTITY,

    /**
     * Endpoint utilizing Session/Oxen privacy network protocols. Features completely decentralized routing
     * with no central servers and no phone number or email tracking requirements.
     */
    SESSION_ID,

    /**
     * Broad category for commercial, centralized messaging networks that cooperate with specific state
     * jurisdictions or are prone to metadata extraction (e.g., WhatsApp, Telegram, WeChat).
     */
    COMMERCIAL_IM,

    // =========================================================================
    // INTERNET, INFRASTRUCTURE & CRYPTO NETWORKS
    // =========================================================================

    /**
     * Standard Electronic Mail address. Essential for tracking administrative registrations,
     * recovery channels, and corporate identities.
     */
    EMAIL,

    /**
     * Static or dynamic Internet Protocol version 4 or version 6 address. Used to map a physical location,
     * server endpoint, or network gateway used by the target.
     */
    IP_ADDRESS,

    /**
     * An onion routing hidden service address (.onion) within the Tor network, or an I2P destination keyspace.
     * Represents a darknet drop point, command-and-control server, or hidden marketplace handle.
     */
    DARKNET_ENDPOINT,

    /**
     * Public cryptographic wallet address or on-chain identifier (e.g., Bitcoin, Monero, Ethereum).
     * Used as a communication or attribution vector where payments or embedded ledger messages serve as a link.
     */
    CRYPTO_WALLET,

    // =========================================================================
    // MILITARY & COVERT FIELD VECTORS
    // =========================================================================

    /**
     * Tactically deployed Radio Frequency channels, including high-frequency (HF) burst transmissions,
     * VHF tactical nets, or numbers stations. Tracks hardware-level radio signatures.
     */
    TACTICAL_RF,

    /**
     * Physical, analog, or real-world dead-letter drops, covert visual signal points, or traditional
     * courier coordination nodes where no digital footprint is generated.
     */
    COVERT_PHYSICAL_DROP
}
