package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Event;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

import java.util.List;

/**
 * A hostile raid. Attackers loot the stockpile (food and steel) and rough up a
 * random colonist, sapping their rest and mood. Severity scales both the plunder
 * and the injury. Non-lethal by design — the colony survives to rebuild.
 */
public final class RaidEvent implements Event {

    private final int severity; // 1..10
    private final int victimIndex; // which colonist is injured (-1 if none)

    public RaidEvent(int severity, int victimIndex) {
        this.severity = Math.max(1, Math.min(10, severity));
        this.victimIndex = victimIndex;
    }

    @Override
    public void apply(ColonyState state) {
        int foodLoot = 2 * severity;
        int steelLoot = severity;
        state.getResources().consumeFood(foodLoot);
        state.getResources().consumeSteel(steelLoot);

        List<Colonist> colonists = state.getColonists();
        if (victimIndex >= 0 && victimIndex < colonists.size()) {
            Needs needs = colonists.get(victimIndex).getNeeds();
            needs.setRest(needs.getRest() - 4.0 * severity);
            needs.setMood(needs.getMood() - 5.0 * severity);
        }
        state.addMessage(describe());
    }

    @Override
    public String describe() {
        return "Raid! Attackers plunder the stockpile (severity " + severity + ").";
    }
}
