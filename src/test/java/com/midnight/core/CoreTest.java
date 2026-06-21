package com.midnight.core;

import com.midnight.ai.BattleResult;
import com.midnight.ai.NightReport;
import com.midnight.ai.NightResolver;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Tests for the {@code com.midnight.core} engine of The Lords of Midnight. */
public class CoreTest {

    /** A no-op night that does nothing but satisfy {@code endDay}. */
    private static NightResolver quietNight() {
        return new NightResolver() {
            @Override
            public NightReport resolveNight(GameState state) {
                return new NightReport(new ArrayList<BattleResult>(), new ArrayList<String>());
            }
        };
    }

    private Character find(GameState s, String name) {
        for (Character c : s.characters()) {
            if (c.name().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private Stronghold stronghold(GameState s, String name) {
        for (Stronghold sh : s.map().strongholds()) {
            if (sh.name().equals(name)) {
                return sh;
            }
        }
        return null;
    }

    // --- value types --------------------------------------------------

    @Test
    public void directionDeltasAndRotation() {
        assertEquals(0, Direction.NORTH.dx());
        assertEquals(-1, Direction.NORTH.dy());
        assertEquals(1, Direction.EAST.dx());
        assertEquals(0, Direction.EAST.dy());
        assertEquals(1, Direction.SOUTHEAST.dx());
        assertEquals(1, Direction.SOUTHEAST.dy());

        assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
        assertEquals(Direction.WEST, Direction.EAST.opposite());
        assertEquals(Direction.NORTHEAST, Direction.NORTH.clockwise());
        assertEquals(Direction.NORTHWEST, Direction.NORTH.anticlockwise());
    }

    @Test
    public void locationNeighborAndDistance() {
        Location a = Location.of(10, 10);
        assertEquals(Location.of(10, 9), a.neighbor(Direction.NORTH));
        assertEquals(Location.of(11, 11), a.neighbor(Direction.SOUTHEAST));
        assertEquals(3, a.chebyshevDistanceTo(Location.of(13, 12)));
        assertEquals(a, Location.of(10, 10));
    }

    @Test
    public void terrainPassabilityAndCost() {
        assertFalse(Terrain.MOUNTAINS.isPassable());
        assertFalse(Terrain.LAKE.isPassable());
        assertTrue(Terrain.PLAINS.isPassable());
        assertTrue(Terrain.CITADEL.isPassable());
        assertEquals(4, Terrain.PLAINS.moveCost());
        assertTrue(Terrain.FOREST.moveCost() > Terrain.PLAINS.moveCost());
    }

    @Test
    public void sideOpponent() {
        assertEquals(Side.DOOMDARK, Side.FREE.opponent());
        assertEquals(Side.FREE, Side.DOOMDARK.opponent());
    }

    // --- map / strongholds --------------------------------------------

    @Test
    public void standardMapIsLargeEnoughWithStrongholds() {
        Map m = Map.standard();
        assertTrue(m.width() >= 60);
        assertTrue(m.height() >= 40);
        assertNotNull(m.strongholdAt(Location.of(30, 3)));
        assertTrue(m.strongholdAt(Location.of(30, 3)).isUshgarak());
        assertTrue(m.strongholdAt(Location.of(28, 37)).isXajorkith());
    }

    @Test
    public void newGameSeedsCompanionsAndCrown() {
        GameState s = GameState.newGame();
        assertEquals(1, s.day());
        assertEquals(Phase.DAY, s.phase());
        assertNotNull(find(s, "Luxor"));
        assertNotNull(find(s, "Morkin"));
        assertNotNull(find(s, "Corleth the Fey"));
        assertNotNull(find(s, "Rorthron the Wise"));
        assertTrue(find(s, "Luxor").isRecruited());
        assertFalse(find(s, "Lord Blood").isRecruited());
        // Doomdark armies present.
        assertFalse(s.charactersOf(Side.DOOMDARK).isEmpty());
        // Ice Crown rests at the Tower of Doom.
        assertEquals(Location.of(40, 1), s.iceCrownLocation());
        assertEquals(Outcome.ONGOING, s.outcome());
    }

    // --- movement cost / validation -----------------------------------

    @Test
    public void mountedLordPaysReducedCost() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        luxor.setLocation(Location.of(20, 20));
        s.map().setTerrain(luxor.location().neighbor(Direction.NORTH), Terrain.PLAINS);
        luxor.setRiders(500);   // mounted
        int mounted = s.moveCost(luxor, Direction.NORTH);

        luxor.setRiders(0);     // on foot
        int onFoot = s.moveCost(luxor, Direction.NORTH);

        assertEquals(4, onFoot);          // PLAINS base
        assertEquals(3, mounted);         // 75% of 4
        assertTrue(mounted < onFoot);
    }

    @Test
    public void moveDeductsHoursAndUpdatesLocation() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        luxor.setLocation(Location.of(20, 20));
        luxor.setRiders(0);
        Location dest = luxor.location().neighbor(Direction.WEST);
        s.map().setTerrain(dest, Terrain.PLAINS);
        int before = luxor.hoursRemaining();

        assertTrue(s.move(luxor, Direction.WEST));
        assertEquals(dest, luxor.location());
        assertEquals(Direction.WEST, luxor.facing());
        assertEquals(before - 4, luxor.hoursRemaining());
    }

    @Test
    public void cannotMoveIntoImpassableTerrain() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        luxor.setLocation(Location.of(20, 20));
        s.map().setTerrain(luxor.location().neighbor(Direction.NORTH), Terrain.MOUNTAINS);
        assertFalse(s.canMove(luxor, Direction.NORTH));
        assertFalse(s.move(luxor, Direction.NORTH));
        assertEquals(Location.of(20, 20), luxor.location());
    }

    @Test
    public void cannotMoveWithoutEnoughHours() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        luxor.setLocation(Location.of(20, 20));
        luxor.setRiders(0);
        s.map().setTerrain(luxor.location().neighbor(Direction.EAST), Terrain.PLAINS);
        luxor.setHoursRemaining(3);   // PLAINS costs 4
        assertFalse(s.canMove(luxor, Direction.EAST));
        assertFalse(s.move(luxor, Direction.EAST));
    }

    @Test
    public void unrecruitedLordCannotBeMoved() {
        GameState s = GameState.newGame();
        Character blood = find(s, "Lord Blood");
        assertFalse(blood.isRecruited());
        s.map().setTerrain(blood.location().neighbor(Direction.SOUTH), Terrain.PLAINS);
        assertFalse(s.canMove(blood, Direction.SOUTH));
    }

    // --- no movement at night -----------------------------------------

    @Test
    public void lordsCannotMoveAtNight() {
        GameState s = GameState.newGame();
        final Character luxor = find(s, "Luxor");
        luxor.setLocation(Location.of(20, 20));
        s.map().setTerrain(luxor.location().neighbor(Direction.SOUTH), Terrain.PLAINS);

        final boolean[] checkedAtNight = {false};
        s.endDay(new NightResolver() {
            @Override
            public NightReport resolveNight(GameState state) {
                // During the resolver the phase is NIGHT — free lords are frozen.
                assertEquals(Phase.NIGHT, state.phase());
                assertFalse(state.canMove(luxor, Direction.SOUTH));
                assertFalse(state.move(luxor, Direction.SOUTH));
                checkedAtNight[0] = true;
                return new NightReport(new ArrayList<BattleResult>(), new ArrayList<String>());
            }
        });
        assertTrue(checkedAtNight[0]);
    }

    // --- day / night transition ---------------------------------------

    @Test
    public void endDayCallsResolverOnceThenDawns() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        luxor.setHoursRemaining(0);
        luxor.setEnergy(50);

        final int[] calls = {0};
        s.endDay(new NightResolver() {
            @Override
            public NightReport resolveNight(GameState state) {
                calls[0]++;
                return new NightReport(new ArrayList<BattleResult>(), new ArrayList<String>());
            }
        });

        assertEquals(1, calls[0]);
        assertEquals(2, s.day());
        assertEquals(Phase.DAY, s.phase());
        assertEquals(GameState.DAY_HOURS, luxor.hoursRemaining());  // hours restored
        assertTrue(luxor.energy() > 50);                            // stamina recovered
    }

    @Test
    public void endDayRejectsNullResolver() {
        GameState s = GameState.newGame();
        try {
            s.endDay(null);
            org.junit.Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    // --- recruitment ---------------------------------------------------

    @Test
    public void recruitIndependentLordBesideRecruiter() {
        GameState s = GameState.newGame();
        Character luxor = find(s, "Luxor");
        Character blood = find(s, "Lord Blood");
        blood.setLocation(luxor.location());   // stand together

        assertTrue(s.tryRecruit(blood));
        assertTrue(blood.isRecruited());
        assertEquals(Side.FREE, blood.side());
        assertTrue(s.playerLords().contains(blood));
        // Already recruited -> no second recruit.
        assertFalse(s.tryRecruit(blood));
    }

    @Test
    public void recruitFailsWithoutRecruiter() {
        GameState s = GameState.newGame();
        Character brith = find(s, "Lord Brith");
        // Alone in the wilds, far from any recruited lord.
        brith.setLocation(Location.of(5, 18));
        assertFalse(s.tryRecruit(brith));
        assertFalse(brith.isRecruited());
    }

    // --- victory conditions -------------------------------------------

    @Test
    public void adventureWinWhenMorkinDestroysCrown() {
        GameState s = GameState.newGame();
        Character morkin = find(s, "Morkin");
        morkin.setLocation(s.iceCrownLocation());   // reach the Tower of Doom

        assertTrue(s.tryDestroyIceCrown());
        assertEquals(Outcome.FREE_ADVENTURE_WIN, s.outcome());
        assertTrue(s.isOver());
    }

    @Test
    public void onlyMorkinMayDestroyTheCrown() {
        GameState s = GameState.newGame();
        Character corleth = find(s, "Corleth the Fey");
        corleth.setLocation(s.iceCrownLocation());
        assertFalse(s.tryDestroyIceCrown());
        assertEquals(Outcome.ONGOING, s.outcome());
        // And no other lord may even bear it.
        corleth.setCarriesIceCrown(true);
        assertFalse(corleth.carriesIceCrown());
    }

    @Test
    public void wargameWinWhenFreeHoldsUshgarak() {
        GameState s = GameState.newGame();
        assertEquals(Outcome.ONGOING, s.outcome());
        stronghold(s, "Ushgarak").setOwner(Side.FREE);
        assertEquals(Outcome.FREE_WARGAME_WIN, s.outcome());
    }

    @Test
    public void doomdarkWinsWhenLuxorSlain() {
        GameState s = GameState.newGame();
        find(s, "Luxor").kill();
        assertEquals(Outcome.DOOMDARK_WIN, s.outcome());
    }

    @Test
    public void doomdarkWinsWhenXajorkithFalls() {
        GameState s = GameState.newGame();
        stronghold(s, "Xajorkith").setOwner(Side.DOOMDARK);
        assertEquals(Outcome.DOOMDARK_WIN, s.outcome());
    }
}
