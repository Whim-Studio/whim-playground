package com.whim.startrek.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Root mutable game state: the galaxy map, all empires, the current turn number and
 * phase, the persistent Borg state, and whether a live RTS battle is in progress.
 */
public class GameState {

    private final GalaxyMap map;
    private final List<Empire> empires;
    private final BorgState borgState = new BorgState();

    private int turnNumber = 1;
    private TurnPhase phase = TurnPhase.INCOME;
    private boolean battleActive;

    public GameState(GalaxyMap map, List<Empire> empires) {
        this.map = map;
        this.empires = empires == null ? new ArrayList<Empire>() : empires;
    }

    public GalaxyMap getMap() {
        return map;
    }

    public List<Empire> getEmpires() {
        return empires;
    }

    /** The (first) player-controlled empire, or null if none is flagged. */
    public Empire getPlayerEmpire() {
        for (Empire e : empires) {
            if (e.isPlayer()) {
                return e;
            }
        }
        return null;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int n) {
        this.turnNumber = n;
    }

    public TurnPhase getPhase() {
        return phase;
    }

    public void setPhase(TurnPhase p) {
        this.phase = p;
    }

    /** Never null. */
    public BorgState getBorgState() {
        return borgState;
    }

    public boolean isBattleActive() {
        return battleActive;
    }

    public void setBattleActive(boolean b) {
        this.battleActive = b;
    }
}
