package com.whim.alganon.worldsim;

import com.whim.alganon.api.Enums.ControlState;

/** A contested Tower/Keep objective in the background faction-war sim. */
public final class WarObjective {
    public final String name;
    public ControlState control;
    /** Continuous influence in [-1, +1]: -1 fully Kujix, +1 fully Asharr. */
    public double influence;
    /** Seconds until the next drift step for this objective. */
    public double nextTick;

    public WarObjective(String name, ControlState control, double influence, double nextTick) {
        this.name = name;
        this.control = control;
        this.influence = influence;
        this.nextTick = nextTick;
    }
}
