package com.whim.colony.api;

import com.whim.colony.ColonyState;
import com.whim.colony.domain.Colonist;

/**
 * A unit of assignable work (e.g. "haul steel", "sow the farm", "sleep"). A Job
 * is engine-agnostic: it describes WHAT should be done and WHERE, but not HOW to
 * step towards it — that is the job of an {@link Action}. The engine (Task 2)
 * assigns Jobs to colonists and drives them to completion.
 */
public interface Job {

    /**
     * @return a short human-readable label for this job, shown in the UI
     * (Task 3) and message log. Never {@code null}.
     */
    String getName();

    /**
     * Test whether this job has been fully satisfied for the given colonist in
     * the current world state. Called by the engine each tick to decide whether
     * to release the colonist and pick a new job.
     *
     * @param state the shared colony state (read-only for this check)
     * @param c     the colonist performing the job
     * @return true if no further work remains
     */
    boolean isComplete(ColonyState state, Colonist c);

    /**
     * @return the X coordinate of the tile this job takes place at (where the
     * colonist must stand or move adjacent to). Used by the engine's pathfinder.
     */
    int getTargetX();

    /**
     * @return the Y coordinate of the tile this job takes place at.
     * @see #getTargetX()
     */
    int getTargetY();
}
