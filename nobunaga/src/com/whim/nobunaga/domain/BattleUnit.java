package com.whim.nobunaga.domain;

import java.awt.Color;

/**
 * One tactical unit on the battle grid: a block of troops belonging to one
 * side (attacker/defender), optionally the commander, positioned at a grid
 * cell. Morale erodes in melee and a routed unit (troops &lt;= 0) is dead.
 */
public final class BattleUnit {
    private final int id;
    private final int daimyoId;
    private final boolean attacker;
    private final boolean commander;
    private int col;
    private int row;
    private int troops;
    private int morale = 100;
    private final String abbrev;
    private final Color color;

    public BattleUnit(int id, int daimyoId, boolean attacker, boolean commander,
                      int col, int row, int troops, String abbrev, Color color) {
        this.id = id;
        this.daimyoId = daimyoId;
        this.attacker = attacker;
        this.commander = commander;
        this.col = col;
        this.row = row;
        this.troops = troops;
        this.abbrev = abbrev;
        this.color = color;
    }

    public int getId() {
        return id;
    }

    public int getDaimyoId() {
        return daimyoId;
    }

    public boolean isAttacker() {
        return attacker;
    }

    public boolean isCommander() {
        return commander;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getTroops() {
        return troops;
    }

    public void setTroops(int troops) {
        this.troops = troops;
    }

    public int getMorale() {
        return morale;
    }

    public void setMorale(int morale) {
        this.morale = morale;
    }

    public String getAbbrev() {
        return abbrev;
    }

    public Color getColor() {
        return color;
    }

    public boolean isAlive() {
        return troops > 0;
    }
}
