package com.whim.b5wars.model;

/** A single ship's starting placement within a scenario. */
public final class Placement {
    private final String shipClassId;
    private final Side side;
    private final Hex pos;
    private final Facing facing;
    private final int speed;

    public Placement(String shipClassId, Side side, Hex pos, Facing facing, int speed) {
        this.shipClassId = shipClassId;
        this.side = side;
        this.pos = pos;
        this.facing = facing;
        this.speed = speed;
    }

    public String getShipClassId() {
        return shipClassId;
    }

    public Side getSide() {
        return side;
    }

    public Hex getPos() {
        return pos;
    }

    public Facing getFacing() {
        return facing;
    }

    public int getSpeed() {
        return speed;
    }
}
