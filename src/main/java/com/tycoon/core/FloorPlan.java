package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The freeform building grid. Rooms and facilities are mutated only while the
 * plan is unlocked (the paused building phase). The auto-turn engine calls
 * {@link #lock()} to finalize the layout for a turn and {@link #unlock()} when
 * control returns to the player.
 */
public class FloorPlan {
    private final int width;
    private final int height;
    private final List<Room> rooms = new ArrayList<Room>();
    private boolean locked = false;

    public FloorPlan(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public List<Room> rooms() {
        return rooms;
    }

    /** @throws IllegalStateException if the plan is locked. */
    public void addRoom(Room r) {
        if (locked) {
            throw new IllegalStateException("FloorPlan is locked; cannot add room while a turn is executing.");
        }
        rooms.add(r);
    }

    private boolean inBounds(GridPos p) {
        return p.x() >= 0 && p.x() < width && p.y() >= 0 && p.y() < height;
    }

    /**
     * Place a facility at its position. Returns false if the plan is locked, the
     * position is out of bounds, no room covers it, or the tile is already occupied.
     */
    public boolean placeFacility(Facility f) {
        if (locked) {
            return false;
        }
        GridPos p = f.pos();
        if (!inBounds(p)) {
            return false;
        }
        Room room = roomAt(p);
        if (room == null) {
            return false;
        }
        if (facilityAt(p) != null) {
            return false;
        }
        room.addFacility(f);
        return true;
    }

    /** @return the facility at p, or null if the tile is empty. */
    public Facility facilityAt(GridPos p) {
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            List<Facility> fs = room.facilities();
            for (int j = 0; j < fs.size(); j++) {
                Facility f = fs.get(j);
                if (f.pos().equals(p)) {
                    return f;
                }
            }
        }
        return null;
    }

    /** @return the (last-added) room covering p, or null if none. */
    public Room roomAt(GridPos p) {
        Room found = null;
        for (int i = 0; i < rooms.size(); i++) {
            Room room = rooms.get(i);
            if (room.contains(p)) {
                found = room;
            }
        }
        return found;
    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public boolean isLocked() {
        return locked;
    }
}
