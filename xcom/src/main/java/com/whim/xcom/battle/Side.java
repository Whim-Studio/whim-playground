package com.whim.xcom.battle;

/** The two combatant sides of a tactical mission. */
public enum Side {
    XCOM,
    ALIEN;

    public Side opponent() {
        return this == XCOM ? ALIEN : XCOM;
    }
}
