package com.whim.startrek.engine;

import com.whim.startrek.domain.Empire;
import com.whim.startrek.domain.FacilityType;
import com.whim.startrek.domain.Fleet;
import com.whim.startrek.domain.GalaxyMap;
import com.whim.startrek.domain.GameState;
import com.whim.startrek.domain.GridCell;
import com.whim.startrek.domain.MapObjectType;
import com.whim.startrek.domain.Race;
import com.whim.startrek.domain.Ship;
import com.whim.startrek.domain.StarSystem;

/**
 * Sensors, cloaking, and grid navigation AI.
 *
 * <p>Detection pits an observer's SENSOR_ARRAY strength against a target fleet's cloak rating.
 * A fleet sitting in a NEBULA can never be detected (the nebula blanks sensors). Romulans cloak the
 * hardest, Klingons next; everyone else is trivially visible when uncloaked.
 */
public class FleetAI {

    public FleetAI() {
    }

    /**
     * Can {@code observer} see {@code target} right now?
     *
     * <ul>
     *   <li>A target sitting in a NEBULA is never detected.</li>
     *   <li>An uncloaked target is always detected.</li>
     *   <li>A cloaked target is detected only when the observer's total SENSOR_ARRAY strength
     *       meets or beats the target's cloak rating.</li>
     * </ul>
     */
    public boolean isDetected(Fleet target, Empire observer, GameState s) {
        if (target == null || observer == null || s == null) {
            return false;
        }
        GalaxyMap map = s.getMap();
        GridCell cell = map.getCell(target.getRow(), target.getCol());
        if (cell != null && cell.getType() == MapObjectType.NEBULA) {
            return false; // nebula blocks sensors / cloak detection
        }
        if (!target.isCloaked()) {
            return true;
        }
        int sensorStrength = sensorStrength(observer);
        int cloakRating = cloakRating(target.getOwner());
        return sensorStrength >= cloakRating;
    }

    /**
     * Update cloak state for all of an empire's cloak-capable ships.
     *
     * <ul>
     *   <li>Romulans run cloaked whenever capable (signature tactic).</li>
     *   <li>Klingons cloak only while at WAR.</li>
     *   <li>Everyone else stays de-cloaked.</li>
     * </ul>
     * Ships sitting in a NEBULA decloak — there is no point spending energy when sensors are already
     * blind, and it keeps weapons hot.
     */
    public void stepCloaking(Empire e, GameState s) {
        if (e == null || s == null) {
            return;
        }
        Race race = e.getRace();
        boolean wantCloak;
        if (race == Race.ROMULAN) {
            wantCloak = true;
        } else if (race == Race.KLINGON) {
            wantCloak = e.getStatus() == com.whim.startrek.domain.EmpireStatus.WAR;
        } else {
            wantCloak = false;
        }
        for (Fleet f : e.getFleets()) {
            GridCell cell = s.getMap().getCell(f.getRow(), f.getCol());
            boolean inNebula = cell != null && cell.getType() == MapObjectType.NEBULA;
            for (Ship ship : f.getShips()) {
                if (!ship.isCloakCapable()) {
                    ship.setCloaked(false);
                    continue;
                }
                ship.setCloaked(wantCloak && !inNebula);
            }
        }
    }

    /**
     * One grid step (8-directional) from the fleet's current cell toward {@code (destRow,destCol)}.
     * Returns {@code {row,col}} of the next cell. Stays put if already at the destination, and never
     * steps onto an instantly-fatal cell (black holes) when a non-fatal sideways option exists.
     */
    public int[] nextStepToward(Fleet f, int destRow, int destCol, GameState s) {
        int curR = f.getRow();
        int curC = f.getCol();
        if (curR == destRow && curC == destCol) {
            return new int[] { curR, curC };
        }
        int stepR = curR + Integer.signum(destRow - curR);
        int stepC = curC + Integer.signum(destCol - curC);
        GalaxyMap map = s.getMap();
        if (!map.inBounds(stepR, stepC)) {
            stepR = clamp(stepR, 0, map.getRows() - 1);
            stepC = clamp(stepC, 0, map.getCols() - 1);
        }
        // Prefer not to fly straight into something that destroys the fleet if we can still make progress.
        if (isFatal(map, stepR, stepC)) {
            int[] alt = avoidFatal(map, curR, curC, destRow, destCol);
            if (alt != null) {
                return alt;
            }
        }
        return new int[] { stepR, stepC };
    }

    private int[] avoidFatal(GalaxyMap map, int curR, int curC, int destRow, int destCol) {
        int bestR = -1, bestC = -1;
        int bestDist = Integer.MAX_VALUE;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int nr = curR + dr;
                int nc = curC + dc;
                if (!map.inBounds(nr, nc) || isFatal(map, nr, nc)) {
                    continue;
                }
                int dist = Math.abs(destRow - nr) + Math.abs(destCol - nc);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestR = nr;
                    bestC = nc;
                }
            }
        }
        if (bestR >= 0) {
            return new int[] { bestR, bestC };
        }
        return null;
    }

    private boolean isFatal(GalaxyMap map, int row, int col) {
        GridCell cell = map.getCell(row, col);
        return cell != null && cell.getType().destroysAssets();
    }

    /** Total sensor strength = sum of SENSOR_ARRAY facilities across the empire's systems. */
    private int sensorStrength(Empire observer) {
        int n = 0;
        for (StarSystem sys : observer.getSystems()) {
            n += Math.max(0, sys.getFacility(FacilityType.SENSOR_ARRAY));
        }
        return n;
    }

    /** How hard a race's cloak is to crack. */
    private int cloakRating(Race owner) {
        if (owner == Race.ROMULAN) {
            return 4;
        }
        if (owner == Race.KLINGON) {
            return 3;
        }
        return 2;
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}
