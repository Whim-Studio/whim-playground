package com.whim.powermonger.domain;

import com.whim.powermonger.api.Enums.CommandType;
import com.whim.powermonger.api.Enums.Posture;

/**
 * An immutable value object describing a single command: what to do, where, and
 * the posture in force when it was issued.
 */
public final class Order {

    private final CommandType type;
    private final int targetTileX;
    private final int targetTileY;
    private final Posture posture;

    public Order(CommandType type, int targetTileX, int targetTileY, Posture posture) {
        this.type = type;
        this.targetTileX = targetTileX;
        this.targetTileY = targetTileY;
        this.posture = posture;
    }

    public CommandType type() { return type; }
    public int targetTileX() { return targetTileX; }
    public int targetTileY() { return targetTileY; }
    public Posture posture() { return posture; }

    @Override public String toString() {
        return "Order{" + type + " ->(" + targetTileX + "," + targetTileY
                + ") " + posture + "}";
    }
}
