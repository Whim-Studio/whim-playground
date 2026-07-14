package com.whim.stars.model.race;

/**
 * Primary Racial Traits — the ten mutually-exclusive specializations a race
 * picks exactly one of in the race wizard. Each is a broad play-style identity.
 *
 * <p>Numeric effects are applied by the simulation engine; this enum only
 * captures identity plus a short design note. Values are ✅ community-confirmed
 * identities; the exact per-trait modifiers are RECONSTRUCTED where applied.
 */
public enum PRT {
    HE("Hyper-Expansion", "Cheap small colonizers, faster overall growth, lower max pop per planet."),
    SS("Super Stealth", "Superior cloaking; can spy minerals/research from scanned enemies."),
    WM("War Monger", "Cheaper, earlier weapons and warships; weak at defenses and terraforming."),
    CA("Claim Adjuster", "Cheap/automatic terraforming; can deterraform enemy worlds."),
    IS("Inner Strength", "Resilient colonists; cheaper scanners and shields; slow early expansion."),
    SD("Space Demolition", "Minefield specialist: more field types, denser fields, faster laying."),
    PP("Packet Physics", "Mass-driver specialist: flings mineral packets further and faster."),
    IT("Interstellar Traveler", "Stargate specialist: cheaper, higher-capacity gates; scans through gates."),
    AR("Alternate Reality", "Population lives in orbitals, not on the surface; a different economy."),
    JOAT("Jack of All Trades", "No specialty; small all-round starting-tech bonus and higher max pop.");

    private final String label;
    private final String note;

    PRT(String label, String note) {
        this.label = label;
        this.note = note;
    }

    public String label() { return label; }
    public String note() { return note; }
}
