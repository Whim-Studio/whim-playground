package com.whim.xcom.battle;

import java.util.ArrayList;
import java.util.List;

/**
 * The public INPUT contract for a tactical mission. A caller (a skirmish, or —
 * later — the Geoscape when a UFO mission begins) fills one of these and hands it
 * to {@link BattleFactory} to obtain a runnable {@link BattleGame}; the game ends
 * with a {@link BattleOutcome}. This keeps the Battlescape drivable headlessly
 * and decoupled from the meta layers.
 */
public final class BattleSetup {

    /** A soldier/alien to deploy: which content ids to use and starting stats. */
    public static final class UnitSpec {
        public final String name;
        public final String weaponId;   // ruleset weapon id
        public final String armorId;    // ruleset armor id (soldiers); null for alien innate
        public final String alienId;    // ruleset alien id (aliens); null for soldiers
        public final int firingAccuracy; // used for soldiers (aliens read from alienId)
        public final int maxTU;
        public final int maxHealth;
        public final int reactions;
        public final int strength;

        private UnitSpec(String name, String weaponId, String armorId, String alienId,
                         int firingAccuracy, int maxTU, int maxHealth, int reactions, int strength) {
            this.name = name;
            this.weaponId = weaponId;
            this.armorId = armorId;
            this.alienId = alienId;
            this.firingAccuracy = firingAccuracy;
            this.maxTU = maxTU;
            this.maxHealth = maxHealth;
            this.reactions = reactions;
            this.strength = strength;
        }

        /** A human X-COM soldier. */
        public static UnitSpec soldier(String name, String weaponId, String armorId,
                                       int firingAccuracy, int maxTU, int maxHealth,
                                       int reactions, int strength) {
            return new UnitSpec(name, weaponId, armorId, null,
                    firingAccuracy, maxTU, maxHealth, reactions, strength);
        }

        /** An alien; stats are read from the ruleset alien def, weapon supplied here. */
        public static UnitSpec alien(String alienId, String weaponId) {
            return new UnitSpec(null, weaponId, null, alienId, 0, 0, 0, 0, 0);
        }
    }

    private final List<UnitSpec> soldiers = new ArrayList<UnitSpec>();
    private final List<UnitSpec> aliens = new ArrayList<UnitSpec>();
    private int mapWidth = 16;
    private int mapHeight = 16;
    private boolean night = false;
    private long seed = 1L;

    public BattleSetup addSoldier(UnitSpec s) { soldiers.add(s); return this; }
    public BattleSetup addAlien(UnitSpec a) { aliens.add(a); return this; }
    public BattleSetup mapSize(int w, int h) { this.mapWidth = w; this.mapHeight = h; return this; }
    public BattleSetup night(boolean night) { this.night = night; return this; }
    public BattleSetup seed(long seed) { this.seed = seed; return this; }

    public List<UnitSpec> soldiers() { return soldiers; }
    public List<UnitSpec> aliens() { return aliens; }
    public int mapWidth() { return mapWidth; }
    public int mapHeight() { return mapHeight; }
    public boolean night() { return night; }
    public long seed() { return seed; }
}
