package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.ArrayList;
import java.util.List;
public class Room {
    private final RoomType type;
    private final int x, y, width, height;
    private final List<Facility> facilities = new ArrayList<Facility>();
    public Room(RoomType type, int x, int y, int width, int height) {
        this.type = type; this.x = x; this.y = y; this.width = width; this.height = height;
    }
    public RoomType type() { return type; }
    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }
    public boolean contains(GridPos p) {
        return p.x() >= x && p.x() < x + width && p.y() >= y && p.y() < y + height;
    }
    public List<Facility> facilities() { return facilities; }
    public void addFacility(Facility f) { facilities.add(f); }
}
