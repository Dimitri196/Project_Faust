package com.projectfaust.shared.enums;

/**
 * Klasifikace zdrojů příjmů pro finanční profilování subjektů v Projektu Faust.
 * Umožňuje rozlišit mezi standardním platem, pasivním příjmem a rizikovými finančními toky.
 */
public enum IncomeSourceType {

    // --- STANDARDNÍ LEGÁLNÍ PŘÍJMY ---
    EMPLOYMENT_WAGE,        // Standardní mzda ze zaměstnání (mimo politické funkce)
    STATE_SALARY,           // Plat ústavního činitele (to, co máme v Appointments)
    BUSINESS_PROFIT,        // Příjmy z OSVČ nebo přímého podnikání

    // --- KAPITÁLOVÉ A PASIVNÍ PŘÍJMY ---
    DIVIDENDS,              // Podíly na zisku z firem (např. Agrofert, holdingy)
    RENT_INCOME,            // Příjmy z pronájmu nemovitostí (sledovaných v ExternalAsset)
    INTEREST_INCOME,        // Výnosy z dluhopisů, úroky z vkladů
    ASSET_SALE_PROFIT,      // Zisk z prodeje majetku (nemovitosti, akcie)

    // --- SPECIFICKÉ A RIZIKOVÉ ZDROJE (Intelligence Focus) ---
    CRYPTO_EXIT,            // Příjem z prodeje kryptoměn (vysoké riziko praní peněz)
    CONSULTING_FEES,        // Často zneužíváno pro skryté úplatky nebo lobbing
    INTELLECTUAL_PROPERTY,  // Licenční poplatky, autorská práva
    DONATION_OR_GIFT,       // Dary (včetně politických darů, které mohou být rizikové)
    INSURANCE_PAYOUT,       // Pojistná plnění (někdy využívaná k legalizaci výnosů)

    // --- OSTATNÍ / DŮCHODOVÉ ---
    PENSION_BENEFIT,        // Starobní nebo výsluhový důchod (klíčové u ex-vojáků/agentů)
    OTHER_UNSPECIFIED       // Vyžaduje ruční prověření analytikem
}
