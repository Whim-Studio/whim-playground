package com.whim.b5wars.engine;

import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Ship;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Thrust / turn-mode / drift movement rules.
 *
 * <p>All thrust and per-hexside costs are APPROXIMATED and marked below; they are the kind of
 * value that would ultimately move into data, but the contract keeps them in the engine as
 * named constants. Per-turn "hexes entered so far" is tracked in a side table keyed by ship
 * identity and reset each turn via {@link #resetTurn()} (the model's {@code Ship} intentionally
 * carries no such counter).
 */
public final class MovementEngine {

    // APPROXIMATED, unverified vs rulebook — thrust to change facing by one hexside.
    static final int TURN_THRUST_COST = 1;
    // APPROXIMATED, unverified vs rulebook — thrust to sideslip one hex laterally.
    static final int SIDESLIP_THRUST_COST = 1;
    // APPROXIMATED, unverified vs rulebook — thrust spent per 1 point of speed change.
    static final int ACCEL_THRUST_PER_SPEED = 1;

    private final Map<Ship, Integer> movedThisTurn = new IdentityHashMap<Ship, Integer>();

    /** Clear the per-turn "hexes entered" counters (call at the start of each turn). */
    public void resetTurn() {
        movedThisTurn.clear();
    }

    private int moved(Ship s) {
        Integer v = movedThisTurn.get(s);
        return v == null ? 0 : v.intValue();
    }

    /**
     * Enter the hex directly ahead ({@code neighbor(facing)}), bounded by the ship's Speed for
     * this turn. Increments straight-hex count (feeds turn-mode). Returns false if the ship has
     * already entered Speed hexes this turn.
     */
    public boolean moveForward(Ship s) {
        if (moved(s) >= s.getSpeed()) {
            return false;
        }
        s.setPos(s.getPos().neighbor(s.getFacing()));
        s.setStraightHexes(s.getStraightHexes() + 1);
        movedThisTurn.put(s, Integer.valueOf(moved(s) + 1));
        return true;
    }

    /**
     * Change facing by one hexside ({@code steps} must be +1 or -1). Legal only when the ship
     * has travelled at least its class turn-mode in straight hexes since the last facing change
     * and has the thrust to spend. On success the straight-hex counter resets to 0.
     */
    public boolean turn(Ship s, int steps) {
        if (steps != 1 && steps != -1) {
            return false;
        }
        if (s.getStraightHexes() < s.getType().getTurnMode()) {
            return false;
        }
        if (s.getThrustAvailable() < TURN_THRUST_COST) {
            return false;
        }
        s.setFacing(s.getFacing().rotate(steps));
        s.setStraightHexes(0);
        s.setThrustAvailable(s.getThrustAvailable() - TURN_THRUST_COST);
        return true;
    }

    /**
     * Translate one hex laterally without changing facing ({@code steps} +1 = toward the
     * starboard-ish hexside, -1 = port-ish). Costs thrust; does not reset the straight-hex
     * counter (facing is unchanged).
     */
    public boolean sideslip(Ship s, int steps) {
        if (steps != 1 && steps != -1) {
            return false;
        }
        if (s.getThrustAvailable() < SIDESLIP_THRUST_COST) {
            return false;
        }
        Facing lateral = s.getFacing().rotate(steps > 0 ? 2 : -2);
        s.setPos(s.getPos().neighbor(lateral));
        s.setThrustAvailable(s.getThrustAvailable() - SIDESLIP_THRUST_COST);
        return true;
    }

    /**
     * Change speed by up to {@code delta}, bounded by the class max speed, floor 0, and the
     * thrust available this turn. The applied delta may be smaller than requested when thrust
     * or the speed bounds run out; thrust is spent for whatever change is applied.
     */
    public void accelerate(Ship s, int delta) {
        if (delta == 0 || ACCEL_THRUST_PER_SPEED <= 0) {
            return;
        }
        int affordable = s.getThrustAvailable() / ACCEL_THRUST_PER_SPEED;
        if (affordable <= 0) {
            return;
        }
        int magnitude = Math.min(Math.abs(delta), affordable);
        int sign = delta > 0 ? 1 : -1;
        int target = s.getSpeed() + sign * magnitude;
        if (target < 0) {
            target = 0;
        }
        if (target > s.getType().getMaxSpeed()) {
            target = s.getType().getMaxSpeed();
        }
        int applied = Math.abs(target - s.getSpeed());
        if (applied == 0) {
            return;
        }
        s.setSpeed(target);
        s.setThrustAvailable(s.getThrustAvailable() - applied * ACCEL_THRUST_PER_SPEED);
    }

    /**
     * Drift: a powerless ship continues along its current vector at current Speed and cannot
     * turn. Advances Speed hexes straight ahead without spending thrust.
     */
    public void drift(Ship s) {
        int steps = s.getSpeed();
        for (int i = 0; i < steps; i++) {
            s.setPos(s.getPos().neighbor(s.getFacing()));
            s.setStraightHexes(s.getStraightHexes() + 1);
        }
    }
}
