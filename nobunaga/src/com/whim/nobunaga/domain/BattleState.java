package com.whim.nobunaga.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * A tactical battle in progress on a {@link #cols} x {@link #rows} grid. Tracks
 * both sides' units and rice supply (burned daily), the current day, and the
 * winner (null while ongoing). The engine resolves a day at a time and sets
 * {@link #winnerDaimyoId} once a victory condition is met.
 */
public final class BattleState {
    public final int cols = 14;
    public final int rows = 10;

    public final List<BattleUnit> units = new ArrayList<BattleUnit>();
    public int day = 1;

    public final int attackerProvId;
    public final int defenderProvId;
    public final int attackerDaimyoId;
    public final int defenderDaimyoId;

    public int attackerRice;
    public int defenderRice;

    public Integer winnerDaimyoId; // null while ongoing
    public String log = "";

    public BattleState(int attackerProvId, int defenderProvId,
                       int attackerDaimyoId, int defenderDaimyoId) {
        this.attackerProvId = attackerProvId;
        this.defenderProvId = defenderProvId;
        this.attackerDaimyoId = attackerDaimyoId;
        this.defenderDaimyoId = defenderDaimyoId;
    }

    /** The living unit occupying the given cell, or null. */
    public BattleUnit unitAt(int col, int row) {
        for (int i = 0; i < units.size(); i++) {
            BattleUnit u = units.get(i);
            if (u.isAlive() && u.getCol() == col && u.getRow() == row) {
                return u;
            }
        }
        return null;
    }

    public boolean inBounds(int c, int r) {
        return c >= 0 && c < cols && r >= 0 && r < rows;
    }
}
