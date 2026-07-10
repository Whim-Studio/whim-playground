package com.whim.merchantprince.model.event;

/**
 * World events and hazards confirmed for Merchant Prince (1994): plague outbreaks,
 * papal interdicts, wars, the early Reformation, plus per-leg travel hazards
 * (storms, piracy) (GAME_DESIGN_REFERENCE §5). PLAGUE/INTERDICT/WAR/REFORMATION are
 * world-scale events raised by the event engine; STORM/PIRACY are per-unit travel
 * hazards resolved by the travel engine.
 */
public enum EventType {
    PLAGUE      ("Plague"),
    INTERDICT   ("Papal Interdict"),
    WAR         ("War"),
    REFORMATION ("Reformation"),
    STORM       ("Storm at Sea"),
    PIRACY      ("Piracy");

    public final String label;
    EventType(String label) { this.label = label; }
}
