package com.tycoon.core;
// LOCAL-ONLY VERIFICATION STUB (Task 1 owns this) — never committed by Task 2.
import java.util.ArrayList;
import java.util.List;
public class FloorPlan {
    private final int width, height;
    private final List<Room> rooms = new ArrayList<Room>();
    private boolean locked;
    public FloorPlan(int width, int height) { this.width = width; this.height = height; }
    public int width() { return width; }
    public int height() { return height; }
    public List<Room> rooms() { return rooms; }
    public void addRoom(Room r) {
        if (locked) throw new IllegalStateException("locked");
        rooms.add(r);
    }
    public boolean placeFacility(Facility f) {
        if (locked) return false;
        Room r = roomAt(f.pos());
        if (r == null) return false;
        if (facilityAt(f.pos()) != null) return false;
        r.addFacility(f);
        return true;
    }
    public Facility facilityAt(GridPos p) {
        for (Room r : rooms) {
            for (Facility f : r.facilities()) {
                if (f.pos().equals(p)) return f;
            }
        }
        return null;
    }
    public Room roomAt(GridPos p) {
        for (Room r : rooms) {
            if (r.contains(p)) return r;
        }
        return null;
    }
    public void lock() { locked = true; }
    public void unlock() { locked = false; }
    public boolean isLocked() { return locked; }
}
