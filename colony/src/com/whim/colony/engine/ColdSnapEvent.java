package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Event;
import com.whim.colony.domain.Colonist;
import com.whim.colony.domain.Needs;

/**
 * A sudden cold snap. Everyone's mood drops as the colony shivers, and rest
 * suffers a little from the miserable night. Severity scales the mood hit.
 */
public final class ColdSnapEvent implements Event {

    private final int severity; // 1 (mild) .. 10 (brutal)

    public ColdSnapEvent(int severity) {
        this.severity = Math.max(1, Math.min(10, severity));
    }

    @Override
    public void apply(ColonyState state) {
        double moodHit = 3.0 * severity;
        double restHit = 1.0 * severity;
        for (Colonist c : state.getColonists()) {
            Needs needs = c.getNeeds();
            needs.setMood(needs.getMood() - moodHit);
            needs.setRest(needs.getRest() - restHit);
        }
        state.addMessage(describe());
    }

    @Override
    public String describe() {
        return "Cold snap! The colony shivers (severity " + severity + ").";
    }
}
