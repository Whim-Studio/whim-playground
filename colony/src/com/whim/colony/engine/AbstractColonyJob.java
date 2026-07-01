package com.whim.colony.engine;

import com.whim.colony.ColonyState;
import com.whim.colony.api.Action;
import com.whim.colony.api.Job;
import com.whim.colony.domain.Colonist;

/**
 * Engine-side base class for the concrete jobs. It layers a small two-phase
 * state machine on top of the bare {@link Job} contract: first walk to the job's
 * target tile (a {@link MoveAction} consuming the colonist's planned path), then
 * carry out the job's work {@link Action}. The {@link Simulation} advances a job
 * one tick at a time via {@link #tick(ColonyState, Colonist)}.
 *
 * <p>The base still implements the plain {@link Job} interface so the UITask 3)
 * can treat every job uniformly through {@code getName()}/target accessors.
 */
public abstract class AbstractColonyJob implements Job {

    private final String name;
    private final int targetX;
    private final int targetY;

    private final MoveAction moveAction = new MoveAction();
    private Action workAction; // lazily created once the colonist arrives

    protected AbstractColonyJob(String name, int targetX, int targetY) {
        this.name = name;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final int getTargetX() {
        return targetX;
    }

    @Override
    public final int getTargetY() {
        return targetY;
    }

    /**
     * Advance this job by exactly one simulation tick for {@code c}: either take
     * one step along the planned path, or (once arrived) perform one tick of the
     * work action.
     */
    public final void tick(ColonyState state, Colonist c) {
        if (c.hasPath()) {
            moveAction.perform(state, c);
            return;
        }
        if (workAction == null) {
            workAction = createWorkAction(state, c);
        }
        if (workAction != null && !workAction.isFinished()) {
            workAction.perform(state, c);
        }
    }

    /** @return true once the work action has run to completion. */
    protected final boolean isWorkFinished() {
        return workAction != null && workAction.isFinished();
    }

    /**
     * Build the {@link Action} that does this job's actual work, invoked once the
     * colonist has reached the target tile. May return {@code null} for jobs that
     * are pure movement (e.g. wandering).
     */
    protected abstract Action createWorkAction(ColonyState state, Colonist c);
}
