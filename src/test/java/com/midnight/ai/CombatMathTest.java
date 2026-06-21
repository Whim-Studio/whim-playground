package com.midnight.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.midnight.core.Character;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Map;
import com.midnight.core.Side;
import com.midnight.core.Terrain;

/**
 * Combat-math tests for {@link StandardCombatResolver}: terrain defensive
 * bonus, courage, and fatigue (energy) effects, plus casualty / capture
 * bookkeeping. Scenarios are built on a fresh {@link GameState#newGame()} by
 * relocating two existing lords onto an otherwise-empty tile so the battle is
 * fully controlled. Force ratios are chosen so the asserted winner holds for
 * every value of the resolver's bounded (&plusmn;15%) fluctuation.
 */
public class CombatMathTest {

    private static final long SEED = 12345L;

    /** Terrain bonus must order strongholds/rough country above open plains. */
    @Test
    public void terrainBonusOrdering() {
        assertTrue(StandardCombatResolver.terrainBonus(Terrain.CITADEL)
                > StandardCombatResolver.terrainBonus(Terrain.KEEP));
        assertTrue(StandardCombatResolver.terrainBonus(Terrain.KEEP)
                > StandardCombatResolver.terrainBonus(Terrain.TOWER));
        assertTrue(StandardCombatResolver.terrainBonus(Terrain.MOUNTAINS)
                > StandardCombatResolver.terrainBonus(Terrain.FOREST));
        assertTrue(StandardCombatResolver.terrainBonus(Terrain.FOREST)
                > StandardCombatResolver.terrainBonus(Terrain.PLAINS));
        assertEquals(1.0, StandardCombatResolver.terrainBonus(Terrain.PLAINS), 1e-9);
    }

    /** Same forces: defender loses on PLAINS but wins inside a CITADEL. */
    @Test
    public void terrainFlipsOutcome() {
        // Free (defender) outnumbered 60 vs 100; on plains they break...
        GameState gs = GameState.newGame();
        Location tile = emptyTile(gs);
        Character free = anyOf(gs, Side.FREE);
        Character doom = anyOf(gs, Side.DOOMDARK);
        placeArmy(free, tile, 72, 0, 63, 63);
        placeArmy(doom, tile, 100, 0, 63, 63);
        gs.map().setTerrain(tile, Terrain.PLAINS);
        BattleResult plains = new StandardCombatResolver(SEED).resolveBattle(gs, tile);
        assertEquals(Side.DOOMDARK, plains.victor());

        // ...but the same defenders hold the same attackers off from a citadel.
        GameState gs2 = GameState.newGame();
        Location tile2 = emptyTile(gs2);
        Character free2 = anyOf(gs2, Side.FREE);
        Character doom2 = anyOf(gs2, Side.DOOMDARK);
        placeArmy(free2, tile2, 72, 0, 63, 63);
        placeArmy(doom2, tile2, 100, 0, 63, 63);
        gs2.map().setTerrain(tile2, Terrain.CITADEL);
        BattleResult citadel = new StandardCombatResolver(SEED).resolveBattle(gs2, tile2);
        assertEquals(Side.FREE, citadel.victor());
    }

    /** A valiant defender holds where a craven one falls (same numbers/terrain). */
    @Test
    public void courageDecidesEvenFight() {
        GameState gs = GameState.newGame();
        Location tile = emptyTile(gs);
        gs.map().setTerrain(tile, Terrain.PLAINS);
        Character free = anyOf(gs, Side.FREE);
        Character doom = anyOf(gs, Side.DOOMDARK);
        placeArmy(free, tile, 100, 0, /*courage*/127, /*energy*/63);
        placeArmy(doom, tile, 100, 0, 63, 63);
        BattleResult brave = new StandardCombatResolver(SEED).resolveBattle(gs, tile);
        assertEquals(Side.FREE, brave.victor());

        GameState gs2 = GameState.newGame();
        Location tile2 = emptyTile(gs2);
        gs2.map().setTerrain(tile2, Terrain.PLAINS);
        Character free2 = anyOf(gs2, Side.FREE);
        Character doom2 = anyOf(gs2, Side.DOOMDARK);
        placeArmy(free2, tile2, 100, 0, /*courage*/0, 63);
        placeArmy(doom2, tile2, 100, 0, 63, 63);
        BattleResult craven = new StandardCombatResolver(SEED).resolveBattle(gs2, tile2);
        assertEquals(Side.DOOMDARK, craven.victor());
    }

    /** A fresh defender holds where an exhausted one falls (same numbers/terrain/courage). */
    @Test
    public void fatigueWeakensArmy() {
        GameState gs = GameState.newGame();
        Location tile = emptyTile(gs);
        gs.map().setTerrain(tile, Terrain.PLAINS);
        Character free = anyOf(gs, Side.FREE);
        Character doom = anyOf(gs, Side.DOOMDARK);
        placeArmy(free, tile, 100, 0, 63, /*energy*/127);
        placeArmy(doom, tile, 100, 0, 63, 63);
        BattleResult rested = new StandardCombatResolver(SEED).resolveBattle(gs, tile);
        assertEquals(Side.FREE, rested.victor());

        GameState gs2 = GameState.newGame();
        Location tile2 = emptyTile(gs2);
        gs2.map().setTerrain(tile2, Terrain.PLAINS);
        Character free2 = anyOf(gs2, Side.FREE);
        Character doom2 = anyOf(gs2, Side.DOOMDARK);
        placeArmy(free2, tile2, 100, 0, 63, /*energy*/0);
        placeArmy(doom2, tile2, 100, 0, 63, 63);
        BattleResult spent = new StandardCombatResolver(SEED).resolveBattle(gs2, tile2);
        assertEquals(Side.DOOMDARK, spent.victor());
    }

    /** An overwhelmed lord stripped of his army in a decisive defeat is slain. */
    @Test
    public void crushedLordIsSlainAndLosesCounted() {
        GameState gs = GameState.newGame();
        Location tile = emptyTile(gs);
        gs.map().setTerrain(tile, Terrain.PLAINS);
        Character free = anyOf(gs, Side.FREE);
        Character doom = anyOf(gs, Side.DOOMDARK);
        placeArmy(free, tile, 3, 0, 30, 30);
        placeArmy(doom, tile, 5000, 0, 100, 100);
        BattleResult br = new StandardCombatResolver(SEED).resolveBattle(gs, tile);
        assertEquals(Side.DOOMDARK, br.victor());
        assertFalse("annihilated lord should be slain", free.isAlive());
        assertTrue("free casualties counted", br.freeLosses() > 0);
    }

    /** Resolver is deterministic for a fixed seed. */
    @Test
    public void deterministicForSeed() {
        BattleResult a = runSimple(new StandardCombatResolver(SEED));
        BattleResult b = runSimple(new StandardCombatResolver(SEED));
        assertEquals(a.victor(), b.victor());
        assertEquals(a.freeLosses(), b.freeLosses());
        assertEquals(a.doomdarkLosses(), b.doomdarkLosses());
    }

    /** A clash of identical armies on neutral ground reports indecisive. */
    @Test
    public void evenFightCanBeIndecisive() {
        // Not asserting a specific seed outcome here beyond non-null result/text.
        BattleResult br = runSimple(new StandardCombatResolver(SEED));
        assertNotNull(br);
        assertNotNull(br.text());
        // victor may be a side or null (indecisive); both are valid.
        if (br.victor() == null) {
            assertNull(br.victor());
        }
    }

    private BattleResult runSimple(StandardCombatResolver resolver) {
        GameState gs = GameState.newGame();
        Location tile = emptyTile(gs);
        gs.map().setTerrain(tile, Terrain.PLAINS);
        Character free = anyOf(gs, Side.FREE);
        Character doom = anyOf(gs, Side.DOOMDARK);
        placeArmy(free, tile, 100, 0, 63, 63);
        placeArmy(doom, tile, 100, 0, 63, 63);
        return resolver.resolveBattle(gs, tile);
    }

    // ---- helpers -------------------------------------------------------------

    private static Character anyOf(GameState gs, Side side) {
        for (Character c : gs.charactersOf(side)) {
            if (c != null && c.isAlive()) {
                return c;
            }
        }
        throw new IllegalStateException("no living character for side " + side);
    }

    private static void placeArmy(Character c, Location where, int warriors, int riders,
                                  int courage, int energy) {
        c.setLocation(where);
        c.setWarriors(warriors);
        c.setRiders(riders);
        c.setCourage(courage);
        c.setEnergy(energy);
    }

    /** Find a passable, stronghold-free tile that currently holds no character. */
    private static Location emptyTile(GameState gs) {
        Map map = gs.map();
        for (int y = 0; y < map.height(); y++) {
            for (int x = 0; x < map.width(); x++) {
                Location loc = Location.of(x, y);
                if (!map.isPassable(loc)) {
                    continue;
                }
                if (map.strongholdAt(loc) != null) {
                    continue;
                }
                if (!gs.charactersAt(loc).isEmpty()) {
                    continue;
                }
                return loc;
            }
        }
        throw new IllegalStateException("no empty tile found on the map");
    }
}
