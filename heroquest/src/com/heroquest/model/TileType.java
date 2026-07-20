package com.heroquest.model;

public enum TileType {
    WALL(false),
    FLOOR(true),
    DOOR_CLOSED(false),
    DOOR_OPEN(true);

    private final boolean passable;

    TileType(boolean passable) {
        this.passable = passable;
    }

    public boolean isPassable() {
        return passable;
    }

    public boolean isDoor() {
        return this == DOOR_CLOSED || this == DOOR_OPEN;
    }
}
