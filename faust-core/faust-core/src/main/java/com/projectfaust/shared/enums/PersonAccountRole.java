package com.projectfaust.shared.enums;

public enum PersonAccountRole {
    OWNER,          // Hlavní majitel účtu (fyzická osoba)
    DISPONENT,      // Osoba s dispozičním právem k nakládání s prostředky
    BENEFICIARY,    // Skutečný majitel / koncový příjemce výhod (skrytá identita)
    SIGNATORY       // Pouze podpisové právo k operacím a schvalování
}