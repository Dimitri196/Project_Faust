package com.projectfaust.shared.enums;

import lombok.Getter;

/**
 * Defines the nature and strength of relationships between entities
 * within the Faust Intelligence network.
 */
@Getter
public enum ConnectionType {

    FAMILY_MEMBER("Family Member", 1.0),
    CLASSMATE("Classmate / Academic Tie", 0.4),
    DISSENT_ALLY("Dissent Ally", 0.8),
    POLITICAL_ALLY("Political Ally", 0.7),
    BUSINESS_PARTNER("Business Partner", 0.6),
    LOBBYIST_CONTACT("Lobbyist Contact", 0.5),
    SENSITIVE_LINK("Monitored / Sensitive Link", 0.3),
    UNDER_INVESTIGATION("Subject of Investigation", 0.2),
    INTELLIGENCE_SOURCE("Intelligence Source / Asset", 0.9),
    CLANDESTINE_CONTACT("Clandestine / Unofficial Contact", 0.85);

    private final String label;
    private final double defaultWeight;

    ConnectionType(String label, double defaultWeight) {
        this.label = label;
        this.defaultWeight = defaultWeight;}

}
