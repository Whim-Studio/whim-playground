package com.whim.populous.api;

/**
 * Shared enumerations — the vocabulary of the whole game. Owned by the
 * orchestrator; do NOT modify in child tasks. Domain, engine, and UI all
 * import from here so they agree on terrain, allegiance, settlements, powers.
 */
public final class Enums {

    private Enums() { }

    /** Who a tile / follower / settlement belongs to. */
    public enum Allegiance {
        NEUTRAL, GOOD, EVIL
    }

    /**
     * Visual/behavioural terrain class. This is DERIVED from a tile's integer
     * elevation (plus transient states like swamp/lava). Task 1 defines the
     * elevation->terrain thresholds; the UI colours by this enum.
     *
     * Elevation model (recommendation, Task 1 is source of truth):
     *   elevation &lt; 0            -> WATER (deep), SHALLOW (just below 0)
     *   elevation == 0           -> SAND (coast / beach, buildable but poor)
     *   1..2                     -> GRASS (prime flat building land)
     *   3..4                     -> HILL
     *   5+                       -> MOUNTAIN / ROCK
     * Transient overrides: SWAMP (drowning trap), LAVA (from volcano).
     */
    public enum TerrainType {
        WATER, SHALLOW, SAND, GRASS, HILL, MOUNTAIN, ROCK, LAVA, SWAMP
    }

    /**
     * Settlement building tier. Larger contiguous flat area around a follower's
     * chosen site => higher tier => more population + more mana. Task 1 owns the
     * exact flat-tile thresholds; see CONTRACT for the target table.
     */
    public enum SettlementType {
        NONE, TENT, HUT, HOUSE, TOWER, CASTLE
    }

    /**
     * Divine powers. manaCost is what the caster (Good or Evil) pays.
     * targeted=true powers need a click location; RAISE_LAND / LOWER_LAND are
     * the always-available terraforming clicks and are effectively free-per-use
     * area edits (small mana trickle handled by the engine).
     */
    public enum GodPower {
        RAISE_LAND ("Raise Land",     0,   true),
        LOWER_LAND ("Lower Land",     0,   true),
        PAPAL_MAGNET("Papal Magnet",  200, true),
        EARTHQUAKE ("Earthquake",     800, true),
        SWAMP      ("Swamp",          400, true),
        VOLCANO    ("Volcano",        1500, true),
        FLOOD      ("Flood",          3000, false),
        ARMAGEDDON ("Armageddon",     5000, false);

        private final String label;
        private final int manaCost;
        private final boolean targeted;

        GodPower(String label, int manaCost, boolean targeted) {
            this.label = label;
            this.manaCost = manaCost;
            this.targeted = targeted;
        }

        public String label() { return label; }
        public int manaCost() { return manaCost; }
        /** true => power is cast at a clicked tile; false => global effect. */
        public boolean targeted() { return targeted; }
    }
}
