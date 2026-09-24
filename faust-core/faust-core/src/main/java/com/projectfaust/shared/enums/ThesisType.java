package com.projectfaust.shared.enums;

/**
 * Typy závěrečných prací v českém akademickém systému.
 * Reflektuje specifika titulů v bezpečnostních a vojenských složkách.
 */
public enum ThesisType {
    /** Bakalářská práce (Bc.) */
    BACHELOR,

    /** Diplomová práce (Mgr., Ing.) */
    MASTER,

    /** Rigorózní práce (PhDr., JUDr., PaedDr.) - velmi časté u PČR a vnitra */
    RIGOROUS,

    /** Disertační práce (Ph.D.) - expertní úroveň */
    DOCTORAL,

    /** Habilitační práce (Doc.) - akademické špičky v oboru */
    HABILITATION,

    /** Specifický vojenský/policejní kurz (např. Generální štáb) */
    SPECIAL_OFFICER_COURSE
}
