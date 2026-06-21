package com.tycoon.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A rectangular region of the floor grid that can hold facilities.
 */
public class Room {
    private final RoomType type;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<Facility> facilities = new ArrayList<Facility>();

    public Room(RoomType type, int x, int y, int width, int height) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public RoomType type() {
        return type;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean contains(GridPos p) {
        return p.x() >= x && p.x() < x + width
                && p.y() >= y && p.y() < y + height;
    }

    /** Live, ordered list of facilities placed in this room. */
    public List<Facility> facilities() {
        return facilities;
    }

    /** Caller ensures the facility is inside this room and not overlapping. */
    public void addFacility(Facility f) {
        facilities.add(f);
    }
}
