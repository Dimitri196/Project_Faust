package com.projectfaust.shared.enums;

/**
 * Defines the classification scheme of an institution identifier.
 *
 * <p>Deliberately kept small and scheme-focused — the country dimension
 * is handled by the separate {@code countryCode} field on
 * {@link com.projectfaust.institution.InstitutionIdentifier}, using
 * ISO 3166-1 alpha-2 codes. This avoids an unmaintainable explosion of
 * per-country enum constants (200+ countries × multiple scheme types)
 * while still enabling precise queries like "find all Czech national
 * registration numbers" via {@code type = NATIONAL_REGISTRATION AND
 * country_code = 'CZ'}.</p>
 *
 * <p><b>Examples of how scheme + country map to real-world identifiers:</b></p>
 * <pre>
 *   NATIONAL_REGISTRATION + CZ  →  IČO (Czech national register)
 *   NATIONAL_REGISTRATION + PL  →  REGON (Polish national register)
 *   NATIONAL_REGISTRATION + FR  →  SIREN (French national register)
 *   NATIONAL_REGISTRATION + DE  →  Handelsregisternummer
 *   NATIONAL_REGISTRATION + GB  →  Companies House CRN
 *   VAT               + CZ      →  DIČ (CZ + IČO)
 *   VAT               + DE      →  Umsatzsteuer-ID
 *   LEI               + null    →  ISO 17442 (global, no country)
 *   DUNS              + null    →  D&amp;B D-U-N-S (global, no country)
 *   EUID              + null    →  EU unique identifier (cross-border)
 * </pre>
 *
 * @author Dimitri / Project Faust
 */
public enum IdentifierType {

    /**
     * National company or organisation registration number — issued by
     * the country's official business/legal register. The specific register
     * and format vary by country; {@code countryCode} (ISO 3166-1 alpha-2)
     * on the identifier record identifies which country's register this
     * value belongs to.
     *
     * <p>This is the primary join key for national procurement data sources
     * (e.g. Czech IČO for Hlidač Státu, REGON for Polish e-Zamówienia).</p>
     */
    NATIONAL_REGISTRATION,

    /**
     * VAT registration number — issued by each country's tax authority.
     * Format is typically the country code prefix + national tax number
     * (e.g. CZ00006947, DE123456789). {@code countryCode} identifies
     * the issuing country's tax authority.
     *
     * <p>Links to EU VIES database for cross-border VAT verification.</p>
     */
    VAT,

    /**
     * ISO 17442 Legal Entity Identifier — 20-character alphanumeric global
     * standard maintained by GLEIF. Mandatory for EU financial market
     * participants (MiFID II, EMIR); used in SWIFT messaging, sanctions
     * lists, and international financial transaction reporting.
     *
     * <p>Global scheme — {@code countryCode} is null for LEI identifiers.</p>
     */
    LEI,

    /**
     * Dun &amp; Bradstreet D-U-N-S Number — 9-digit global business
     * identifier. Widely used in NATO/US federal procurement (SAM.gov),
     * international supply chain intelligence, and credit reporting.
     *
     * <p>Global scheme — {@code countryCode} is null for DUNS identifiers.</p>
     */
    DUNS,

    /**
     * European Unique Identifier — introduced by EU Directive 2019/1151 to
     * link national business registers across EU member states. Format:
     * {@code CC-RegisterCode-RegisteredNumber} (e.g. CZ-OR-B1234).
     *
     * <p>EU-wide scheme — {@code countryCode} may be set to the home member
     * state, but the identifier itself crosses borders by design.</p>
     */
    EUID,

    /**
     * GLEIF Relationship Map identifier — links parent/child entities within
     * the LEI framework. Useful for mapping corporate ownership structures
     * and tracing institutional affiliations across borders.
     *
     * <p>Global scheme — {@code countryCode} is null.</p>
     */
    GLEIF_RELATIONSHIP,

    /**
     * Source-specific or scheme-specific identifier not covered by the
     * standard types above. Use the {@code note} field on the identifier
     * record to document the exact scheme, format, and source system.
     *
     * <p>{@code countryCode} may or may not apply depending on the scheme.</p>
     */
    CUSTOM
}