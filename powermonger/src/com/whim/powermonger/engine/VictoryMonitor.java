package com.whim.powermonger.engine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.whim.powermonger.api.Enums.Allegiance;
import com.whim.powermonger.domain.Captain;
import com.whim.powermonger.domain.Town;
import com.whim.powermonger.domain.WorldState;

/**
 * Owns every shift of the {@link com.whim.powermonger.domain.BalanceOfPower}. Each
 * tick it diffs the world against what it has already scored:
 * <ul>
 *   <li>a captain newly at 0 strength shifts the balance (toward the player if the
 *       dead captain was an enemy, toward the enemy otherwise);</li>
 *   <li>a town whose controlling bloc has changed hands is (re)captured and shifts
 *       the balance;</li>
 *   <li>all enemy captains dead / balance at +1 = player victory; all player
 *       captains dead / balance at -1 = defeat.</li>
 * </ul>
 * Diffing (rather than firing from combat) guarantees each event scores exactly once.
 */
public final class VictoryMonitor {

    /** A bloc this close to a town seizes it. */
    private static final double CAPTURE_RANGE = 0.9;

    private final Set<Integer> countedDead = new HashSet<Integer>();

    public void tick(WorldState w, List<String> events) {
        scoreEliminations(w, events);
        scoreCaptures(w, events);
        evaluateEndState(w, events);
    }

    private void scoreEliminations(WorldState w, List<String> events) {
        List<Captain> caps = w.captains();
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (!c.alive() && !countedDead.contains(c.id())) {
                countedDead.add(c.id());
                boolean enemy = c.allegiance() == Allegiance.ENEMY;
                w.balance().onCaptainEliminated(enemy);
                events.add("Balance shifts " + (enemy ? "toward player" : "toward enemy")
                        + " (captain " + c.name() + " lost)");
            }
        }
    }

    private void scoreCaptures(WorldState w, List<String> events) {
        List<Town> towns = w.towns();
        for (int i = 0; i < towns.size(); i++) {
            Town town = towns.get(i);
            Allegiance holder = nearestBlocAllegiance(w, town.tileX(), town.tileY());
            if (holder == null) {
                continue;
            }
            if (holder == Allegiance.PLAYER && town.allegiance() != Allegiance.PLAYER) {
                town.setAllegiance(Allegiance.PLAYER);
                town.setCaptured(true);
                w.balance().onTownCaptured(true);
                events.add("Town " + town.name() + " captured by the player");
            } else if (holder == Allegiance.ENEMY && town.allegiance() == Allegiance.PLAYER) {
                town.setAllegiance(Allegiance.ENEMY);
                town.setCaptured(true);
                w.balance().onTownCaptured(false);
                events.add("Town " + town.name() + " lost to the enemy");
            }
        }
    }

    /** Allegiance of the closest living non-neutral bloc within capture range, or null. */
    private Allegiance nearestBlocAllegiance(WorldState w, int tx, int ty) {
        List<Captain> caps = w.captains();
        Allegiance best = null;
        double bestDist = CAPTURE_RANGE;
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (!c.alive() || c.allegiance() == Allegiance.NEUTRAL) {
                continue;
            }
            double d = CommandLag.distance(c.x(), c.y(), tx + 0.0, ty + 0.0);
            if (d <= bestDist) {
                bestDist = d;
                best = c.allegiance();
            }
        }
        return best;
    }

    private void evaluateEndState(WorldState w, List<String> events) {
        if (w.gameOver()) {
            return;
        }
        int enemyAlive = 0;
        int playerAlive = 0;
        List<Captain> caps = w.captains();
        for (int i = 0; i < caps.size(); i++) {
            Captain c = caps.get(i);
            if (!c.alive()) {
                continue;
            }
            if (c.allegiance() == Allegiance.ENEMY) {
                enemyAlive++;
            } else if (c.allegiance() == Allegiance.PLAYER) {
                playerAlive++;
            }
        }

        if (enemyAlive == 0) {
            w.balance().set(1.0);
            finish(w, events, true, "All enemy captains eliminated — PLAYER VICTORY");
        } else if (playerAlive == 0) {
            w.balance().set(-1.0);
            finish(w, events, false, "All player captains lost — DEFEAT");
        } else if (w.balance().playerVictory()) {
            finish(w, events, true, "Balance of Power tipped fully — PLAYER VICTORY");
        } else if (w.balance().playerDefeat()) {
            finish(w, events, false, "Balance of Power collapsed — DEFEAT");
        }
    }

    private void finish(WorldState w, List<String> events, boolean playerWon, String msg) {
        w.setGameOver(true);
        w.setPlayerWon(playerWon);
        w.setStatusMessage(msg);
        events.add(msg);
    }
}
