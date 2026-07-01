package com.whim.warroom.engine;

import com.whim.warroom.domain.MapState;
import com.whim.warroom.domain.Stance;
import com.whim.warroom.domain.Unit;
import com.whim.warroom.domain.Vec2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-stance tactical behavior. Each tick this produces one {@link Order} per
 * live unit describing HOW it should move (follow its route, pursue an enemy,
 * or flee). Combat multipliers derived from stance live in {@link CombatResolver};
 * this class only governs intent/motion so the pieces stay independent.
 *
 * Behavior by stance (contract "Stances" section):
 *  - OFFENSIVE: closes distance; once an enemy is within sight range it ignores
 *    its route and pursues the nearest enemy (stopping just inside weapon range).
 *  - DEFENSIVE: holds position / follows its route.
 *  - RETREAT: flees the nearest enemy.
 * A routed unit (panicked) always flees regardless of stance.
 *
 * Deterministic: no randomness, purely a function of the unit set + tick.
 */
final class AiController {

    /** Sight range is this multiple of a unit's weapon range. */
    static final double SIGHT_MULT = 3.0;

    /** How a unit intends to move this tick. */
    enum Mode { ROUTE, PURSUE, FLEE }

    /** Immutable per-tick movement order handed to {@link MovementSystem}. */
    static final class Order {
        final Mode mode;
        /** Target for PURSUE/FLEE (enemy position). Null for ROUTE. */
        final Vec2 target;
        Order(Mode mode, Vec2 target) {
            this.mode = mode;
            this.target = target;
        }
    }

    /** Compute one Order per alive unit id. */
    Map<Integer, Order> plan(List<Unit> units, MapState map, int tick) {
        Map<Integer, Order> orders = new HashMap<Integer, Order>();
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (!u.isAlive()) {
                continue;
            }
            if (u.isRouted()) {
                Unit enemy = nearestEnemy(u, units);
                orders.put(u.getId(), new Order(Mode.FLEE, enemy == null ? null : enemy.getPos()));
                continue;
            }
            Stance stance = u.getStance();
            Unit enemy = nearestEnemy(u, units);
            double sight = u.getType().getRange() * SIGHT_MULT;

            switch (stance) {
                case OFFENSIVE:
                    if (enemy != null && u.getPos().dist(enemy.getPos()) <= sight) {
                        orders.put(u.getId(), new Order(Mode.PURSUE, enemy.getPos()));
                    } else {
                        orders.put(u.getId(), new Order(Mode.ROUTE, null));
                    }
                    break;
                case RETREAT:
                    orders.put(u.getId(), new Order(Mode.FLEE, enemy == null ? null : enemy.getPos()));
                    break;
                case DEFENSIVE:
                default:
                    orders.put(u.getId(), new Order(Mode.ROUTE, null));
                    break;
            }
        }
        return orders;
    }

    /** Nearest alive enemy of the opposing faction, or null. */
    static Unit nearestEnemy(Unit u, List<Unit> units) {
        Unit best = null;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < units.size(); i++) {
            Unit o = units.get(i);
            if (o == u || !o.isAlive()) {
                continue;
            }
            if (o.getFaction() != u.getFaction().enemyOf()) {
                continue;
            }
            double d = u.getPos().dist(o.getPos());
            if (d < bestD) {
                bestD = d;
                best = o;
            }
        }
        return best;
    }
}
