package com.projectfaust.entity.enums;

/**
 * Kategorizace majetkových uzlů pro zpravodajskou analýzu v Projektu Faust.
 * Navrženo pro sledování toku peněz, vlivu a detekci nelegálního obohacování.
 */
public enum AssetType {

    // --- NEMOVITOSTI (Real Estate - Core Intelligence) ---
    RESIDENTIAL_PROPERTY,    // Byty, domy, vily (často psané na bílé koně)
    COMMERCIAL_PROPERTY,     // Kanceláře, sklady, hotely (zdroj pasivního příjmu)
    LAND_PLOT,               // Pozemky (spekulace, strategické parcely u hranic/základen)
    SECRET_FACILITY,         // Bunker, bezpečný byt, neregistrovaná stavba

    // --- FINANČNÍ NÁSTROJE (Financial Intelligence - FININT) ---
    BANK_ACCOUNT,            // Domácí i offshore účty
    CASH_STOCKPILE,          // Fyzická hotovost v trezorech (v detekci rozvědky)
    STOCK_SHARE,             // Akcie a podíly v korporacích (vazba na Obchodní rejstřík)
    INVESTMENT_BOND,         // Státní a korporátní dluhopisy
    OFFSHORE_SHELL_ENTITY,   // Prázdná schránka v daňovém ráji

    // --- DIGITÁLNÍ AKTIVA (Cyber Intelligence - CYBINT) ---
    CRYPTO_WALLET,           // BTC, ETH, Monero (klíčové pro sledování šedé zóny)
    DIGITAL_IDENTITY,        // Domény, sociální profily s vysokým dosahem (vlivové aktivum)
    INTELLECTUAL_PROPERTY,   // Patenty, ochranné známky, software (licenční poplatky)

    // --- MOVITÝ MAJETEK (Vysoká hodnota / Logistika) ---
    VEHICLE_LUXURY,          // Luxusní vozy (často psané na IČO)
    VEHICLE_OPERATIONAL,     // Operativní vozy, sledovací technika
    AIRCRAFT,                // Soukromé jety, vrtulníky (sledování pohybu elit)
    MARITIME_VESSEL,         // Jachty (oblíbený nástroj pro praní peněz v mezinárodních vodách)

    // --- CENNÉ PŘEDMĚTY (Nemožné zmrazit jedním kliknutím) ---
    PRECIOUS_METAL,          // Zlato, platina, diamanty
    ART_COLLECTION,          // Obrazy, starožitnosti (vysoká likvidita v šedé ekonomice)
    WEAPON_COLLECTION,       // Arzenál (sběratelský i funkční - bezpečnostní riziko)

    // --- VLIVOVÁ AKTIVA (Human Intelligence - HUMINT) ---
    POLITICAL_DEBT,          // Fiktivní dluhy nebo směnky sloužící k vydírání
    PROXIED_ASSET            // Majetek držený svěřenským fondem nebo příbuzným
}
