package com.whim.firetop.model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The dungeon: a graph of {@link Room}s keyed by id, with a marked entrance and
 * Zagor's lair. Exits are bidirectional.
 */
public final class Board implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, Room> rooms = new HashMap<Integer, Room>();
    private int entranceId;
    private int lairId;

    public void addRoom(Room room) { rooms.put(room.getId(), room); }

    /** Links two rooms with a bidirectional corridor. */
    public void link(int a, int b) {
        Room ra = rooms.get(a);
        Room rb = rooms.get(b);
        if (ra != null && rb != null) {
            ra.addExit(b);
            rb.addExit(a);
        }
    }

    public Room getRoom(int id) { return rooms.get(id); }
    public Collection<Room> getRooms() { return rooms.values(); }

    public int getEntranceId() { return entranceId; }
    public void setEntranceId(int entranceId) { this.entranceId = entranceId; }
    public int getLairId() { return lairId; }
    public void setLairId(int lairId) { this.lairId = lairId; }

    /** Direct neighbours of a room. */
    public List<Room> neighbors(int id) {
        List<Room> out = new ArrayList<Room>();
        Room r = rooms.get(id);
        if (r != null) {
            for (Integer e : r.getExits()) {
                Room n = rooms.get(e);
                if (n != null) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    /**
     * Breadth-first set of room ids reachable within exactly up to {@code steps}
     * moves from {@code startId}, excluding the start itself.
     */
    public Set<Integer> roomsWithin(int startId, int steps) {
        Set<Integer> result = new LinkedHashSet<Integer>();
        Set<Integer> seen = new HashSet<Integer>();
        Deque<int[]> queue = new ArrayDeque<int[]>(); // [roomId, depth]
        queue.add(new int[] { startId, 0 });
        seen.add(startId);
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            int rid = cur[0];
            int depth = cur[1];
            if (depth >= steps) {
                continue;
            }
            Room r = rooms.get(rid);
            if (r == null) {
                continue;
            }
            for (Integer e : r.getExits()) {
                if (!seen.contains(e)) {
                    seen.add(e);
                    result.add(e);
                    queue.add(new int[] { e, depth + 1 });
                }
            }
        }
        return result;
    }
}
