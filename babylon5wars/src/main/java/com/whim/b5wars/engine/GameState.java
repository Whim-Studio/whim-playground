package com.whim.b5wars.engine;

import com.whim.b5wars.model.Dice;
import com.whim.b5wars.model.Faction;
import com.whim.b5wars.model.Placement;
import com.whim.b5wars.model.Scenario;
import com.whim.b5wars.model.Ship;
import com.whim.b5wars.model.ShipClass;
import com.whim.b5wars.model.Side;

import java.util.ArrayList;
import java.util.List;

/**
 * Full mutable game state: the in-play {@link Ship}s (built from the scenario's placements plus
 * the factions' ship classes), the seeded {@link Dice}, and the turn/impulse/phase/initiative
 * bookkeeping the {@link TurnManager} drives.
 *
 * <p>Getters match the contract; the {@code set*} mutators are package-private so only the
 * engine (chiefly {@link TurnManager}) advances the machine.
 */
public final class GameState {

    private final Scenario scenario;
    private final List<Ship> ships;
    private final Dice dice;

    private int turn = 1;
    private int impulse = 0;
    private TurnPhase phase = TurnPhase.INITIATIVE;
    private Side initiativeWinner = null;
    private boolean over = false;
    private Side winner = null;

    public GameState(Scenario scenario, List<Faction> factions, long seed) {
        this.scenario = scenario;
        this.dice = new Dice(seed);
        this.ships = new ArrayList<Ship>();
        for (Placement p : scenario.getPlacements()) {
            ShipClass type = resolveClass(factions, p.getShipClassId());
            if (type == null) {
                throw new IllegalArgumentException(
                        "No ShipClass found for placement id: " + p.getShipClassId());
            }
            ships.add(new Ship(type, p.getSide(), p.getPos(), p.getFacing(), p.getSpeed()));
        }
    }

    private static ShipClass resolveClass(List<Faction> factions, String id) {
        for (Faction f : factions) {
            ShipClass sc = f.byId(id);
            if (sc != null) {
                return sc;
            }
        }
        return null;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public List<Ship> getShips() {
        return ships;
    }

    public int getTurn() {
        return turn;
    }

    public int getImpulse() {
        return impulse;
    }

    public TurnPhase getPhase() {
        return phase;
    }

    public Dice getDice() {
        return dice;
    }

    public Side getInitiativeWinner() {
        return initiativeWinner;
    }

    public boolean isOver() {
        return over;
    }

    /** Winning side, or {@code null} until the game is over (or on a draw). */
    public Side getWinner() {
        return winner;
    }

    // --- package-private mutators used by the engine FSM ---

    void setTurn(int turn) {
        this.turn = turn;
    }

    void setImpulse(int impulse) {
        this.impulse = impulse;
    }

    void setPhase(TurnPhase phase) {
        this.phase = phase;
    }

    void setInitiativeWinner(Side side) {
        this.initiativeWinner = side;
    }

    void setOver(boolean over) {
        this.over = over;
    }

    void setWinner(Side winner) {
        this.winner = winner;
    }
}
