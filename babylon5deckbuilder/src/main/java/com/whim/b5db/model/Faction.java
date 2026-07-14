package com.whim.b5db.model;

/**
 * The playable and support factions of the Babylon 5 setting.
 * Maps directly to the GDD faction identities. NON_ALIGNED / PSI_CORPS are
 * support factions available in the shared market but not chosen as a seat.
 */
public enum Faction {
    NARN_REGIME("Narn Regime"),
    CENTAURI_REPUBLIC("Centauri Republic"),
    MINBARI_FEDERATION("Minbari Federation"),
    EARTH_ALLIANCE("Earth Alliance"),
    PSI_CORPS("Psi Corps"),
    NON_ALIGNED("Non-aligned");

    private final String display;

    Faction(String display) {
        this.display = display;
    }

    /** @return human-readable faction name for the UI. */
    public String display() {
        return display;
    }

    /** @return the four factions a player may pick as a seat. */
    public static Faction[] playable() {
        return new Faction[]{NARN_REGIME, CENTAURI_REPUBLIC, MINBARI_FEDERATION, EARTH_ALLIANCE};
    }
}
