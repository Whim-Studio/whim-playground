package com.whim.xcom.battle;

import java.util.Collections;
import java.util.List;

/**
 * The public RESULT contract of a tactical mission. The Geoscape/meta layer will
 * read this to award score, recover items and update the surviving soldiers.
 */
public final class BattleOutcome {

    private final Side winner;         // null while the battle is still running
    private final int turns;
    private final List<String> survivingSoldiers;
    private final List<String> fallenSoldiers;
    private final int aliensKilled;

    public BattleOutcome(Side winner, int turns,
                         List<String> survivingSoldiers, List<String> fallenSoldiers,
                         int aliensKilled) {
        this.winner = winner;
        this.turns = turns;
        this.survivingSoldiers = survivingSoldiers;
        this.fallenSoldiers = fallenSoldiers;
        this.aliensKilled = aliensKilled;
    }

    public boolean decided() { return winner != null; }
    public Side winner() { return winner; }
    public boolean xcomVictory() { return winner == Side.XCOM; }
    public int turns() { return turns; }
    public List<String> survivingSoldiers() { return Collections.unmodifiableList(survivingSoldiers); }
    public List<String> fallenSoldiers() { return Collections.unmodifiableList(fallenSoldiers); }
    public int aliensKilled() { return aliensKilled; }
}
