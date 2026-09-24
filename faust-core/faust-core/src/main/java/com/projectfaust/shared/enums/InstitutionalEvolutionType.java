package com.projectfaust.shared.enums;

public enum InstitutionalEvolutionType {
    /** * Formální změna bez faktického přerušení kontinuity.
     * Příklad: Ministerstvo zdravotnictví ČSR -> Ministerstvo zdravotnictví ČR (1993).
     */
    CONTINUITY_REORGANIZATION,

    /** * Vznik nové instituce vyčleněním agendy z existujícího subjektu.
     * Příklad: Vyčlenění NCTEKK z NCOZ (2023).
     */
    ORGANIZATIONAL_SPLIT_OFF,

    /** * Sloučení dvou a více subjektů do jednoho nového (původní zanikají).
     * Příklad: ÚOOZ + ÚOKFK -> NCOZ (2016).
     */
    INSTITUTIONAL_MERGER,

    /** * Převzetí kompetencí zaniklé instituce jinou, již existující.
     * Příklad: Rozpuštění speciálního odboru a převedení pod krajské ředitelství.
     */
    COMPETENCE_ABSORPTION,

    /** * Úplný zánik instituce a její agendy bez přímého nástupce v systému.
     */
    TOTAL_DISSOLUTION,

    /** * Rozdělení jedné federální/centrální instituce na více samostatných nástupců.
     * Příklad: Federální ministerstvo vnitra -> MV ČR + MV SR (1993).
     */
    SUCCESSION_BY_PARTITION
}