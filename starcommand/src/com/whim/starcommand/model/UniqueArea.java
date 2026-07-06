package com.whim.starcommand.model;

import java.io.Serializable;

/**
 * A "unique area" — a dungeon-like complex of linked rooms (a pirate base or
 * insectoid hive) explored with a drop-ship squad. The player moves room to
 * room under fog of war; rooms hold enemies, loot, or the mission objective.
 */
public class UniqueArea implements Serializable {
    private static final long serialVersionUID = 1L;

    /** One chamber of the complex. */
    public static class Room implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum Kind { ENTRANCE, EMPTY, ENEMY, LOOT, OBJECTIVE }

        public Kind kind = Kind.EMPTY;
        public boolean discovered = false; // seen on the map (adjacent-revealed)
        public boolean visited = false;    // entered
        public boolean cleared = false;    // enemy defeated / loot taken
        public int loot = 0;

        // Open doorways to neighbours; a missing door is a wall.
        public boolean north = false;
        public boolean south = false;
        public boolean west = false;
        public boolean east = false;
    }

    public final int rows;
    public final int cols;
    public final Room[][] rooms;
    public int pr;         // player row
    public int pc;         // player col
    public boolean boss;   // objective room houses Blackbeard
    public String title = "Unique Area";

    public UniqueArea(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.rooms = new Room[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                rooms[r][c] = new Room();
    }

    public boolean inBounds(int r, int c) {
        return r >= 0 && c >= 0 && r < rows && c < cols;
    }

    public Room at(int r, int c) { return rooms[r][c]; }

    public Room player() { return rooms[pr][pc]; }

    public boolean atEntrance() { return player().kind == Room.Kind.ENTRANCE; }

    /** Open a doorway between two orthogonally-adjacent rooms (both sides). */
    public void carve(int r, int c, int nr, int nc) {
        if (nr == r - 1 && nc == c) { rooms[r][c].north = true; rooms[nr][nc].south = true; }
        else if (nr == r + 1 && nc == c) { rooms[r][c].south = true; rooms[nr][nc].north = true; }
        else if (nr == r && nc == c - 1) { rooms[r][c].west = true; rooms[nr][nc].east = true; }
        else if (nr == r && nc == c + 1) { rooms[r][c].east = true; rooms[nr][nc].west = true; }
    }

    /** Whether the squad can step from (r,c) into adjacent (nr,nc) through a doorway. */
    public boolean canMove(int r, int c, int nr, int nc) {
        if (!inBounds(nr, nc)) return false;
        if (nr == r - 1 && nc == c) return rooms[r][c].north;
        if (nr == r + 1 && nc == c) return rooms[r][c].south;
        if (nr == r && nc == c - 1) return rooms[r][c].west;
        if (nr == r && nc == c + 1) return rooms[r][c].east;
        return false;
    }

    /** Reveal (r,c) and any rooms reachable from it through an open doorway. */
    public void reveal(int r, int c) {
        rooms[r][c].discovered = true;
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dd : d) {
            int nr = r + dd[0], nc = c + dd[1];
            if (canMove(r, c, nr, nc)) rooms[nr][nc].discovered = true;
        }
    }

    /** True once the objective room has been cleared. */
    public boolean objectiveSecured() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (rooms[r][c].kind == Room.Kind.OBJECTIVE) return rooms[r][c].cleared;
        return false;
    }
}
