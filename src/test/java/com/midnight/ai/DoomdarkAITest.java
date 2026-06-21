package com.midnight.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.midnight.core.Character;
import com.midnight.core.GameState;
import com.midnight.core.Location;
import com.midnight.core.Phase;
import com.midnight.core.Side;

/**
 * Legality tests for {@link DoomdarkAI#resolveNight}: it must mutate the live
 * state through core setters only, never move FREE lords, never advance the day,
 * and always return a populated, non-null {@link NightReport}.
 */
public class DoomdarkAITest {

    @Test
    public void reportIsNeverNull() {
        GameState gs = GameState.newGame();
        NightReport report = new DoomdarkAI().resolveNight(gs);
        assertNotNull(report);
        assertNotNull(report.battles());
        assertNotNull(report.movements());
        assertNotNull(report.narrative());
    }

    @Test
    public void doesNotMoveFreeLords() {
        GameState gs = GameState.newGame();
        java.util.Map<String, Location> before = new HashMap<String, Location>();
        for (Character c : gs.charactersOf(Side.FREE)) {
            before.put(c.name(), c.location());
        }
        new DoomdarkAI().resolveNight(gs);
        for (Character c : gs.charactersOf(Side.FREE)) {
            assertEquals("FREE lord " + c.name() + " must not move at night",
                    before.get(c.name()), c.location());
        }
    }

    @Test
    public void doesNotAdvanceDayOrPhase() {
        GameState gs = GameState.newGame();
        int day = gs.day();
        Phase phase = gs.phase();
        new DoomdarkAI().resolveNight(gs);
        assertEquals("resolveNight must not advance the day", day, gs.day());
        assertEquals("resolveNight must not change the phase", phase, gs.phase());
    }

    @Test
    public void doomdarkArmiesAdvance() {
        GameState gs = GameState.newGame();
        java.util.Map<String, Location> before = new HashMap<String, Location>();
        for (Character c : gs.charactersOf(Side.DOOMDARK)) {
            before.put(c.name(), c.location());
        }
        NightReport report = new DoomdarkAI().resolveNight(gs);

        boolean someMoved = false;
        for (Character c : gs.charactersOf(Side.DOOMDARK)) {
            if (c.isAlive() && !before.get(c.name()).equals(c.location())) {
                someMoved = true;
                break;
            }
        }
        assertTrue("at least one Doomdark army should advance at night",
                someMoved || !report.movements().isEmpty());
    }

    @Test
    public void doomdarkArmiesStayInBounds() {
        GameState gs = GameState.newGame();
        new DoomdarkAI().resolveNight(gs);
        for (Character c : gs.charactersOf(Side.DOOMDARK)) {
            Location loc = c.location();
            assertTrue("Doomdark army moved off the map at " + loc,
                    gs.map().isPassable(loc));
        }
    }

    @Test
    public void customResolverIsInvokedWhenForcesMeet() {
        GameState gs = GameState.newGame();
        // Force a meeting: drop a Doomdark army onto a player lord's tile.
        List<Character> lords = gs.playerLords();
        assertTrue("expected at least one player lord", !lords.isEmpty());
        Character target = lords.get(0);
        Character doom = firstAlive(gs, Side.DOOMDARK);
        doom.setLocation(target.location());

        CountingResolver counter = new CountingResolver();
        NightReport report = new DoomdarkAI(counter).resolveNight(gs);
        assertNotNull(report);
        assertTrue("combat resolver should have been invoked at the shared tile",
                counter.calls >= 1);
    }

    private static Character firstAlive(GameState gs, Side side) {
        for (Character c : gs.charactersOf(side)) {
            if (c != null && c.isAlive()) {
                return c;
            }
        }
        throw new IllegalStateException("no living character for " + side);
    }

    /** A resolver that records how many battles it was asked to resolve. */
    private static final class CountingResolver implements CombatResolver {
        int calls = 0;

        @Override
        public BattleResult resolveBattle(GameState state, Location where) {
            calls++;
            return new BattleResult(where, null, 0, 0, "counted");
        }
    }
}
