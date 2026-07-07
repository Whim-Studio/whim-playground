package com.whim.b5wars.engine;

import com.whim.b5wars.data.CriticalEntry;
import com.whim.b5wars.data.DataLoader;
import com.whim.b5wars.model.DefenseType;
import com.whim.b5wars.model.Facing;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;
import com.whim.b5wars.model.VictoryCondition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Drives the per-turn finite-state machine:
 *
 * <pre>INITIATIVE -> POWER -> EW -> IMPULSE -> END_OF_TURN -> (next turn / game over)</pre>
 *
 * The impulse loop moves ships per the data-driven speed→cadence table and auto-resolves fire
 * (each ship shoots ready weapons at the nearest in-arc/in-range enemy), so the engine can run a
 * full game headless; the UI can instead drive movement/fire manually and call {@link #endTurn()}.
 */
public final class TurnManager {

    // APPROXIMATED, unverified vs rulebook — impulses per turn when no cadence data is available.
    static final int DEFAULT_IMPULSE_COUNT = 8;

    private final GameState state;
    private final MovementEngine movement = new MovementEngine();
    private final CombatEngine combat;
    private final Map<Integer, boolean[]> cadence;
    private final int impulseCount;

    public TurnManager(GameState state) {
        this.state = state;
        this.combat = new CombatEngine(state.getDice(), loadCritTable());
        this.cadence = loadCadence();
        int count = DEFAULT_IMPULSE_COUNT;
        for (boolean[] row : cadence.values()) {
            if (row != null && row.length > 0) {
                count = row.length;
                break;
            }
        }
        this.impulseCount = count;
    }

    private static List<CriticalEntry> loadCritTable() {
        try {
            return DataLoader.loadCriticalTable();
        } catch (RuntimeException ex) {
            return new ArrayList<CriticalEntry>();
        }
    }

    private static Map<Integer, boolean[]> loadCadence() {
        try {
            Map<Integer, boolean[]> m = DataLoader.loadImpulseCadence();
            if (m != null && !m.isEmpty()) {
                return m;
            }
        } catch (RuntimeException ex) {
            // fall through to computed cadence
        }
        return new HashMap<Integer, boolean[]>();
    }

    /** Advance the FSM one phase, returning the events produced by that phase. */
    public List<GameEvent> advancePhase() {
        List<GameEvent> events = new ArrayList<GameEvent>();
        if (state.isOver()) {
            return events;
        }
        switch (state.getPhase()) {
            case INITIATIVE:
                rollInitiative();
                events.add(new GameEvent("PHASE", "Initiative: side " + state.getInitiativeWinner()
                        + " wins the turn"));
                state.setPhase(TurnPhase.POWER);
                break;
            case POWER:
                allocatePower();
                events.add(new GameEvent("PHASE", "Power allocated (thrust set)"));
                state.setPhase(TurnPhase.EW);
                break;
            case EW:
                allocateEw();
                events.add(new GameEvent("PHASE", "EW allocated"));
                state.setPhase(TurnPhase.IMPULSE);
                break;
            case IMPULSE:
                events.addAll(runImpulseLoop());
                state.setPhase(TurnPhase.END_OF_TURN);
                break;
            case END_OF_TURN:
            default:
                events.addAll(endTurn());
                break;
        }
        return events;
    }

    /** d6 + best initiative bonus per side; higher wins (ties -> A). */
    public void rollInitiative() {
        int a = state.getDice().d(6) + bestInitiativeBonus(Side.A);
        int b = state.getDice().d(6) + bestInitiativeBonus(Side.B);
        state.setInitiativeWinner(b > a ? Side.B : Side.A);
    }

    private int bestInitiativeBonus(Side side) {
        int best = 0;
        boolean any = false;
        for (Ship s : state.getShips()) {
            if (s.getSide() == side) {
                int bonus = s.getType().getInitiativeBonus();
                if (!any || bonus > best) {
                    best = bonus;
                    any = true;
                }
            }
        }
        return best;
    }

    /** Set each ship's available thrust from its class thrust, bounded by reactor power. */
    private void allocatePower() {
        movement.resetTurn();
        for (Ship s : state.getShips()) {
            if (s.isDestroyed()) {
                continue;
            }
            ShipClass c = s.getType();
            // APPROXIMATED, unverified vs rulebook — available thrust is thrust capped by power.
            s.setThrustAvailable(Math.min(c.getThrust(), c.getPower()));
        }
    }

    /** Default EW split: half to offense, remainder to defense (UI may overwrite). */
    private void allocateEw() {
        for (Ship s : state.getShips()) {
            if (s.isDestroyed()) {
                continue;
            }
            int rating = s.getType().getEwRating();
            // APPROXIMATED, unverified vs rulebook — even offense/defense EW split as a default.
            int off = rating / 2;
            s.setEwOffensive(off);
            s.setEwDefensive(rating - off);
        }
    }

    /** Run all impulses this turn: move ships per cadence, then auto-resolve fire each impulse. */
    public List<GameEvent> runImpulseLoop() {
        List<GameEvent> events = new ArrayList<GameEvent>();
        for (int imp = 0; imp < impulseCount; imp++) {
            state.setImpulse(imp);
            for (Ship s : state.getShips()) {
                if (s.isDestroyed()) {
                    continue;
                }
                if (entersHexOnImpulse(s.getSpeed(), imp)) {
                    if (movement.moveForward(s)) {
                        events.add(new GameEvent("MOVE", s.getType().getName() + " -> "
                                + s.getPos()));
                    }
                }
            }
            events.addAll(resolveFire(state.getTurn()));
        }
        return events;
    }

    private List<GameEvent> resolveFire(int currentTurn) {
        List<GameEvent> events = new ArrayList<GameEvent>();
        for (Ship attacker : state.getShips()) {
            if (attacker.isDestroyed()) {
                continue;
            }
            Ship target = nearestEnemy(attacker);
            if (target == null) {
                continue;
            }
            int weapons = attacker.getType().getWeapons().size();
            for (int i = 0; i < weapons; i++) {
                if (attacker.getReloadReadyTurn(i) > currentTurn) {
                    continue;
                }
                if (combat.inArcAndRange(attacker, i, target)) {
                    events.addAll(combat.fire(attacker, i, target, currentTurn));
                }
            }
        }
        return events;
    }

    private Ship nearestEnemy(Ship attacker) {
        Ship best = null;
        int bestDist = Integer.MAX_VALUE;
        for (Ship s : state.getShips()) {
            if (s.getSide() == attacker.getSide() || s.isDestroyed()) {
                continue;
            }
            int d = attacker.getPos().distance(s.getPos());
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    /** Cadence lookup: use data if present, else an even distribution of Speed hexes over impulses. */
    private boolean entersHexOnImpulse(int speed, int impulse) {
        boolean[] row = cadence.get(Integer.valueOf(speed));
        if (row != null && impulse < row.length) {
            return row[impulse];
        }
        return computedCadence(speed, impulse);
    }

    /** Evenly spread {@code speed} hex-entries across {@code impulseCount} impulses. */
    private boolean computedCadence(int speed, int impulse) {
        if (speed <= 0) {
            return false;
        }
        if (speed >= impulseCount) {
            return true;
        }
        // Mark an entry whenever the running (impulse*speed) crosses an integer multiple of impulseCount.
        int prev = (impulse * speed) / impulseCount;
        int cur = ((impulse + 1) * speed) / impulseCount;
        return cur > prev;
    }

    /** End-of-turn bookkeeping: shield regen, drift powerless ships, victory check, advance turn. */
    public List<GameEvent> endTurn() {
        List<GameEvent> events = new ArrayList<GameEvent>();

        for (Ship s : state.getShips()) {
            if (s.isDestroyed()) {
                continue;
            }
            // SHIELD-type defense regenerates to class values; ARMOR does not.
            if (s.getType().getDefenseType() == DefenseType.SHIELD) {
                Map<Facing, Integer> cur = s.getArmor();
                for (Map.Entry<Facing, Integer> e : s.getType().getArmor().entrySet()) {
                    cur.put(e.getKey(), e.getValue());
                }
            }
            // Powerless ships drift along their vector.
            if (s.getThrustAvailable() <= 0) {
                movement.drift(s);
                events.add(new GameEvent("MOVE", s.getType().getName() + " drifts to " + s.getPos()));
            }
        }

        events.addAll(checkVictory());

        if (!state.isOver()) {
            state.setTurn(state.getTurn() + 1);
            state.setImpulse(0);
            state.setPhase(TurnPhase.INITIATIVE);
        }
        return events;
    }

    private List<GameEvent> checkVictory() {
        List<GameEvent> events = new ArrayList<GameEvent>();
        boolean aDown = sideEliminated(Side.A);
        boolean bDown = sideEliminated(Side.B);
        VictoryCondition vc = state.getScenario().getVictory();
        int turnLimit = state.getScenario().getTurnLimit();

        if (vc == VictoryCondition.DESTROY_OR_CRIPPLE_ENEMY) {
            if (aDown && bDown) {
                finish(events, null, "Mutual destruction");
                return events;
            }
            if (bDown) {
                finish(events, Side.A, "Side B destroyed/crippled");
                return events;
            }
            if (aDown) {
                finish(events, Side.B, "Side A destroyed/crippled");
                return events;
            }
        }

        // Time-limit resolution (applies to both conditions once turns run out).
        if (turnLimit > 0 && state.getTurn() >= turnLimit) {
            int a = totalStructure(Side.A);
            int b = totalStructure(Side.B);
            if (a > b) {
                finish(events, Side.A, "More structure at turn limit");
            } else if (b > a) {
                finish(events, Side.B, "More structure at turn limit");
            } else {
                finish(events, null, "Draw at turn limit");
            }
        }
        return events;
    }

    private void finish(List<GameEvent> events, Side winner, String why) {
        state.setOver(true);
        state.setWinner(winner);
        events.add(new GameEvent("VICTORY",
                (winner == null ? "Game over (draw): " : "Winner: side " + winner + " — ") + why));
    }

    private boolean sideEliminated(Side side) {
        boolean any = false;
        for (Ship s : state.getShips()) {
            if (s.getSide() == side) {
                any = true;
                if (!s.isDestroyed() && !s.isCrippled()) {
                    return false;
                }
            }
        }
        return any;
    }

    private int totalStructure(Side side) {
        int total = 0;
        for (Ship s : state.getShips()) {
            if (s.getSide() == side && !s.isDestroyed()) {
                total += s.totalStructureRemaining();
            }
        }
        return total;
    }
}
