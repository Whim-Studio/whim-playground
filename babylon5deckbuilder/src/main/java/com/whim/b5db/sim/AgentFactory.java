package com.whim.b5db.sim;

import com.whim.b5db.ai.Agent;
import com.whim.b5db.model.Faction;

/** Creates a fresh {@link Agent} for a seat in a specific game (for determinism). */
public interface AgentFactory {
    Agent create(int seatIndex, Faction faction, long gameSeed);
}
