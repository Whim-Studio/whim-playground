package com.heroquest.model;

/**
 * A single dungeon square. Occupancy by entities is tracked on {@link GameState}
 * rather than here, so the map stays a pure spatial description.
 */
public final class Tile {
    private TileType type;
    private int roomId;          // -1 for corridors / no room
    private boolean revealed;    // Fog of War: false until seen by a hero
    private Furniture furniture; // nullable
    private boolean trap;        // hidden pit trap
    private boolean trapSprung;

    public Tile(TileType type, int roomId) {
        this.type = type;
        this.roomId = roomId;
        this.revealed = false;
        this.furniture = null;
        this.trap = false;
        this.trapSprung = false;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = type;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public boolean isRoom() {
        return roomId >= 0;
    }

    public boolean isRevealed() {
        return revealed;
    }

    public void setRevealed(boolean revealed) {
        this.revealed = revealed;
    }

    public Furniture getFurniture() {
        return furniture;
    }

    public void setFurniture(Furniture furniture) {
        this.furniture = furniture;
    }

    public boolean hasTrap() {
        return trap && !trapSprung;
    }

    public void setTrap(boolean trap) {
        this.trap = trap;
    }

    public void springTrap() {
        this.trapSprung = true;
    }

    /** Passable for movement: furniture blocks entry, walls/closed doors block. */
    public boolean isWalkable() {
        return type.isPassable() && furniture == null;
    }
}
