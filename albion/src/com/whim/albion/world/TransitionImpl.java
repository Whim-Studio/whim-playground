package com.whim.albion.world;

import com.whim.albion.api.Enums.Direction;
import com.whim.albion.api.WorldModel;

/** A door/edge link from one map cell to a cell on another (or the same) map. */
public final class TransitionImpl implements WorldModel.Transition {

    private final String targetMapId;
    private final int targetX;
    private final int targetY;
    private final Direction targetFacing;

    public TransitionImpl(String targetMapId, int targetX, int targetY, Direction targetFacing) {
        this.targetMapId = targetMapId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetFacing = targetFacing;
    }

    @Override public String targetMapId() { return targetMapId; }
    @Override public int targetX() { return targetX; }
    @Override public int targetY() { return targetY; }
    @Override public Direction targetFacing() { return targetFacing; }
}
