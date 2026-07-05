package com.whim.populous.domain;

import com.whim.populous.api.Enums.Allegiance;
import com.whim.populous.api.Enums.SettlementType;
import com.whim.populous.api.Enums.TerrainType;

/**
 * GUI-free invariant check for the domain layer. Constructs a real game via
 * {@link GameStateManager} and asserts map dimensions, terrain derivation,
 * settlement tiering and mana-accrual math. Run {@link #main} (or call
 * {@link #run()} from a test) to validate the layer in isolation.
 */
public final class DomainSelfCheck {

    private DomainSelfCheck() { }

    public static void main(String[] args) {
        run();
        System.out.println("DomainSelfCheck: OK");
    }

    /** Throws {@link IllegalStateException} on the first failed invariant. */
    public static void run() {
        checkTerrainRules();
        checkSettlementRules();
        checkMapAndGame();
        checkFlatArea();
        checkManaAccrual();
        checkFlood();
    }

    private static void checkTerrainRules() {
        int sea = 0;
        require(TerrainRules.terrainFor(-3, sea) == TerrainType.WATER, "deep water");
        require(TerrainRules.terrainFor(-1, sea) == TerrainType.SHALLOW, "shallow");
        require(TerrainRules.terrainFor(0, sea) == TerrainType.SAND, "sand coast");
        require(TerrainRules.terrainFor(1, sea) == TerrainType.GRASS, "grass low");
        require(TerrainRules.terrainFor(2, sea) == TerrainType.GRASS, "grass high");
        require(TerrainRules.terrainFor(3, sea) == TerrainType.HILL, "hill low");
        require(TerrainRules.terrainFor(4, sea) == TerrainType.HILL, "hill high");
        require(TerrainRules.terrainFor(5, sea) == TerrainType.MOUNTAIN, "mountain low");
        require(TerrainRules.terrainFor(6, sea) == TerrainType.MOUNTAIN, "mountain high");
        require(TerrainRules.terrainFor(7, sea) == TerrainType.ROCK, "rock");
        // Sea level shifting (FLOOD) re-derives terrain.
        require(TerrainRules.terrainFor(1, 2) == TerrainType.SHALLOW, "flood submerges grass");
    }

    private static void checkSettlementRules() {
        require(SettlementRules.tierFor(0) == SettlementType.NONE, "0 -> NONE");
        require(SettlementRules.tierFor(1) == SettlementType.TENT, "1 -> TENT");
        require(SettlementRules.tierFor(2) == SettlementType.HUT, "2 -> HUT");
        require(SettlementRules.tierFor(3) == SettlementType.HUT, "3 -> HUT");
        require(SettlementRules.tierFor(4) == SettlementType.HOUSE, "4 -> HOUSE");
        require(SettlementRules.tierFor(8) == SettlementType.HOUSE, "8 -> HOUSE");
        require(SettlementRules.tierFor(9) == SettlementType.TOWER, "9 -> TOWER");
        require(SettlementRules.tierFor(15) == SettlementType.TOWER, "15 -> TOWER");
        require(SettlementRules.tierFor(16) == SettlementType.CASTLE, "16 -> CASTLE");
        require(SettlementRules.tierFor(40) == SettlementType.CASTLE, "40 -> CASTLE");

        require(SettlementRules.populationWeight(SettlementType.TENT) == 1, "tent pop");
        require(SettlementRules.populationWeight(SettlementType.HUT) == 2, "hut pop");
        require(SettlementRules.populationWeight(SettlementType.HOUSE) == 4, "house pop");
        require(SettlementRules.populationWeight(SettlementType.TOWER) == 7, "tower pop");
        require(SettlementRules.populationWeight(SettlementType.CASTLE) == 12, "castle pop");
        require(SettlementRules.manaWeight(SettlementType.CASTLE) == 12, "castle mana");
    }

    private static void checkMapAndGame() {
        GameStateManager mgr = new GameStateManager();
        GameState gs = mgr.newGame(1234L);
        require(gs.map().cols() == 64, "cols == 64");
        require(gs.map().rows() == 64, "rows == 64");
        require(gs.map().seaLevel() == 0, "seaLevel == 0");

        // Terrain reported by a tile matches the rules for its elevation.
        for (int r = 0; r < 64; r += 7) {
            for (int c = 0; c < 64; c += 7) {
                int e = gs.map().tileAt(c, r).elevation();
                require(gs.map().tileAt(c, r).terrain()
                        == TerrainRules.terrainFor(e, 0), "tile terrain derivation");
            }
        }

        // Both sides start with walkers and a settlement.
        require(gs.goodPopulation() == GameStateManager.START_FOLLOWERS, "good start pop");
        require(gs.evilPopulation() == GameStateManager.START_FOLLOWERS, "evil start pop");
        require(mgr.manaWeightFor(gs, Allegiance.GOOD) > 0, "good has mana source");
        require(mgr.manaWeightFor(gs, Allegiance.EVIL) > 0, "evil has mana source");
    }

    private static void checkFlatArea() {
        MapGrid map = new MapGrid(16, 16, 0);
        // Carve a 3x3 plateau at elevation 2, ringed lower so it is isolated.
        for (int r = 4; r <= 6; r++) {
            for (int c = 4; c <= 6; c++) {
                map.setElevation(c, r, 2);
            }
        }
        // Ring at elevation 1 so the flood-fill stops at the 3x3.
        for (int r = 3; r <= 7; r++) {
            for (int c = 3; c <= 7; c++) {
                boolean edge = r == 3 || r == 7 || c == 3 || c == 7;
                if (edge) {
                    map.setElevation(c, r, 1);
                }
            }
        }
        int flat = map.flatAreaAt(5, 5);
        require(flat == 9, "3x3 plateau flat area == 9 (got " + flat + ")");
        require(SettlementRules.tierFor(flat) == SettlementType.TOWER, "9 flat -> TOWER");

        // Water is never buildable.
        map.setElevation(0, 0, -2);
        require(map.flatAreaAt(0, 0) == 0, "water flat area == 0");
    }

    private static void checkManaAccrual() {
        GameStateManager mgr = new GameStateManager();
        GameState gs = mgr.newGame(42L);
        gs.setGoodMana(0);
        gs.setEvilMana(0);
        int perTick = mgr.manaWeightFor(gs, Allegiance.GOOD);
        require(perTick > 0, "good per-tick mana weight > 0");
        mgr.accrueMana(gs);
        require(gs.goodMana() == perTick, "one tick accrues exactly the weight sum");
        mgr.accrueMana(gs);
        require(gs.goodMana() == perTick * 2, "two ticks accrue 2x weight");

        // Mana clamps at maxMana.
        gs.setGoodMana(gs.maxMana());
        mgr.accrueMana(gs);
        require(gs.goodMana() == gs.maxMana(), "mana clamps at maxMana");
    }

    private static void checkFlood() {
        MapGrid map = new MapGrid(8, 8, 0);
        map.setElevation(2, 2, 1); // grass
        require(map.tileAt(2, 2).terrain() == TerrainType.GRASS, "pre-flood grass");
        map.raiseSeaLevel(2);       // FLOOD
        require(map.tileAt(2, 2).terrain() == TerrainType.SHALLOW, "post-flood submerged");
        require(map.tile(2, 2).isWater(), "submerged tile is water");
    }

    private static void require(boolean cond, String what) {
        if (!cond) {
            throw new IllegalStateException("DomainSelfCheck failed: " + what);
        }
    }
}
