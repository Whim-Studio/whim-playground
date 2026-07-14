package com.whim.b5db.engine;

import com.whim.b5db.model.Faction;

/** A player seat requested at game creation: display name, faction, human/AI. */
public final class Seat {
    public final String name;
    public final Faction faction;
    public final boolean ai;

    public Seat(String name, Faction faction, boolean ai) {
        this.name = name;
        this.faction = faction;
        this.ai = ai;
    }
}
